import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';

import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { InventarioLotes } from '../../../aplicacion/inventario-lotes';
import { Bodega } from '../../../dominio/modelos/catalogos';
import { TIPOS_MOVIMIENTO, TIPOS_MOVIMIENTO_DIRECTO, TipoMovimientoLote } from '../../../dominio/modelos/lotes';
import { ModeloActivo, nombreCompletoModelo } from '../../../dominio/modelos/modelo-activo';

export type ModoMovimiento = 'movimiento' | 'traslado';

export interface DatosDialogoMovimiento {
  readonly modo: ModoMovimiento;
  /** Solo modelos controlados por lote. */
  readonly modelos: readonly ModeloActivo[];
  readonly bodegas: readonly Bodega[];
  readonly idModelo?: number;
  readonly idBodega?: number;
}

const LONGITUD_MOTIVO = 255;

/**
 * Registra un movimiento de existencias o un traslado entre bodegas. Las
 * cantidades son positivas; solo los ajustes admiten valores negativos.
 */
@Component({
  selector: 'app-dialogo-movimiento-lote',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-movimiento-lote.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoMovimientoLote {
  protected readonly datos = inject<DatosDialogoMovimiento>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoMovimientoLote, boolean>);
  private readonly inventario = inject(InventarioLotes);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly esTraslado = this.datos.modo === 'traslado';
  protected readonly nombreModelo = nombreCompletoModelo;
  protected readonly etiquetasTipo = TIPOS_MOVIMIENTO;
  protected readonly tiposDirectos = TIPOS_MOVIMIENTO_DIRECTO;
  protected readonly bodegasActivas = this.datos.bodegas.filter((bodega) => bodega.activa);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    idModelo: [this.datos.idModelo ?? 0, Validators.min(1)],
    tipo: ['INGRESO' as TipoMovimientoLote, Validators.required],
    idBodega: [this.datos.idBodega ?? 0, Validators.min(1)],
    idBodegaDestino: [0, this.esTraslado ? Validators.min(1) : []],
    cantidad: [null as number | null, [Validators.required, Validators.pattern(/^-?\d+$/)]],
    motivo: ['', Validators.maxLength(LONGITUD_MOTIVO)],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.operacion().subscribe({
      next: () => this.dialogo.close(true),
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.guardando.set(false);
      },
    });
  }

  private operacion(): Observable<unknown> {
    const valores = this.formulario.getRawValue();
    const motivo = valores.motivo.trim() || null;
    const cantidad = Number(valores.cantidad);
    if (this.esTraslado) {
      return this.inventario.trasladar({
        idModelo: valores.idModelo,
        idBodegaOrigen: valores.idBodega,
        idBodegaDestino: valores.idBodegaDestino,
        cantidad,
        motivo,
      });
    }
    return this.inventario.registrarMovimiento({
      idModelo: valores.idModelo,
      idBodega: valores.idBodega,
      tipo: valores.tipo,
      cantidad,
      motivo,
    });
  }
}
