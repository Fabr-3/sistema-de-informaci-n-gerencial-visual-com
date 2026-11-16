import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { catchError, map, of } from 'rxjs';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionConsumibles } from '../../../aplicacion/gestion-subcontratacion';
import {
  Consumible,
  MovimientoConsumible,
  TIPOS_MOVIMIENTO_CONSUMIBLE,
  stockTrasAjuste,
} from '../../../dominio/modelos/consumible';

const LONGITUD_NOMBRE = 120;
const LONGITUD_UNIDAD = 30;
const LONGITUD_MOTIVO = 255;
const CANTIDAD_MINIMA = 0.01;
const MOVIMIENTOS_VISIBLES = 50;
const COLUMNAS_MOVIMIENTOS = ['fecha', 'tipo', 'cantidad', 'motivo', 'creador'];

/** Alta o edicion de un consumible del catalogo (el stock cambia con ingresos y ajustes). */
@Component({
  selector: 'app-dialogo-consumible',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule],
  template: `
    <h2 mat-dialog-title>{{ consumible ? 'Editar consumible' : 'Nuevo consumible' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" [maxlength]="longitudNombre" placeholder="Cinta gaffer negra 2 pulgadas" />
            <mat-error>Indique el nombre</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Unidad de medida</mat-label>
            <input matInput formControlName="unidadMedida" [maxlength]="longitudUnidad" placeholder="rollo, unidad, metro…" />
            <mat-error>Indique la unidad</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Costo unitario (Bs)</mat-label>
            <input matInput type="number" formControlName="costoUnitario" min="0" step="0.01" />
            <mat-error>Indique un costo de cero o más</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Stock mínimo (opcional)</mat-label>
            <input matInput type="number" formControlName="stockMinimo" min="0" step="0.01" />
            <mat-hint>Bajo este nivel se advierte el stock bajo</mat-hint>
            <mat-error>No puede ser negativo</mat-error>
          </mat-form-field>
          <mat-slide-toggle formControlName="activo">Activo</mat-slide-toggle>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoConsumible {
  protected readonly consumible = inject<Consumible | undefined>(MAT_DIALOG_DATA);
  private readonly gestion = inject(GestionConsumibles);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoConsumible, Consumible>);

  protected readonly longitudNombre = LONGITUD_NOMBRE;
  protected readonly longitudUnidad = LONGITUD_UNIDAD;
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.consumible?.nombre ?? '', [Validators.required, Validators.maxLength(LONGITUD_NOMBRE)]],
    unidadMedida: [this.consumible?.unidadMedida ?? '', [Validators.required, Validators.maxLength(LONGITUD_UNIDAD)]],
    costoUnitario: [this.consumible?.costoUnitario ?? (null as number | null), [Validators.required, Validators.min(0)]],
    stockMinimo: [this.consumible?.stockMinimo ?? (null as number | null), Validators.min(0)],
    activo: [this.consumible?.activo ?? true],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.guardar(this.consumible?.id ?? null, {
        ...valores,
        costoUnitario: Number(valores.costoUnitario),
        stockMinimo: valores.stockMinimo === null || `${valores.stockMinimo}` === '' ? null : Number(valores.stockMinimo),
      }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

export interface DatosDialogoStock {
  readonly consumible: Consumible;
  readonly modo: 'INGRESO' | 'AJUSTE';
}

/**
 * Ingreso de stock (compra o reposicion, con costo nuevo opcional) o ajuste
 * por conteo fisico (con signo y motivo obligatorio).
 */
@Component({
  selector: 'app-dialogo-stock',
  imports: [DecimalPipe, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ esIngreso ? 'Ingreso de stock' : 'Ajuste de stock' }} · {{ datos.consumible.nombre }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          Stock actual: {{ datos.consumible.stockActual | number: '1.0-2' }} {{ datos.consumible.unidadMedida }}.
          @if (!esIngreso) {
            Use una cantidad negativa para descontar (p. ej. -2).
          }
        </p>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Cantidad ({{ datos.consumible.unidadMedida }})</mat-label>
            <input matInput type="number" formControlName="cantidad" step="0.01" />
            @if (!esIngreso && stockResultante() !== null) {
              <mat-hint>Quedarán {{ stockResultante() | number: '1.0-2' }}</mat-hint>
            }
            <mat-error>{{ esIngreso ? 'Indique una cantidad mayor a cero' : 'Indique una cantidad distinta de cero' }}</mat-error>
          </mat-form-field>
          @if (esIngreso) {
            <mat-form-field appearance="outline">
              <mat-label>Costo unitario nuevo (opcional)</mat-label>
              <input matInput type="number" formControlName="costoUnitario" min="0" step="0.01" />
              <mat-hint>Vigente: Bs {{ datos.consumible.costoUnitario | number: '1.2-2' }}</mat-hint>
            </mat-form-field>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>{{ esIngreso ? 'Detalle (opcional)' : 'Motivo' }}</mat-label>
            <input matInput formControlName="motivo" [maxlength]="longitudMotivo" />
            <mat-error>Indique el motivo del ajuste</mat-error>
          </mat-form-field>
          @if (stockNegativo()) {
            <p class="completo error-grupo" role="alert">El ajuste dejaría el stock en negativo</p>
          }
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando() || stockNegativo()">Registrar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoStock {
  protected readonly datos = inject<DatosDialogoStock>(MAT_DIALOG_DATA);
  private readonly gestion = inject(GestionConsumibles);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoStock, Consumible>);

  protected readonly esIngreso = this.datos.modo === 'INGRESO';
  protected readonly longitudMotivo = LONGITUD_MOTIVO;
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    cantidad: [null as number | null, this.esIngreso ? [Validators.required, Validators.min(CANTIDAD_MINIMA)] : Validators.required],
    costoUnitario: [null as number | null, Validators.min(0)],
    motivo: ['', this.esIngreso ? Validators.maxLength(LONGITUD_MOTIVO) : [Validators.required, Validators.maxLength(LONGITUD_MOTIVO)]],
  });
  private readonly cantidad = toSignal(this.formulario.controls.cantidad.valueChanges, { initialValue: null });
  protected readonly stockResultante = computed(() => {
    const cantidad = Number(this.cantidad());
    return this.cantidad() === null || Number.isNaN(cantidad) ? null : stockTrasAjuste(this.datos.consumible, cantidad);
  });
  protected readonly stockNegativo = computed(() => !this.esIngreso && (this.stockResultante() ?? 0) < 0);

  protected guardar(): void {
    const cantidad = Number(this.formulario.controls.cantidad.value);
    if (this.formulario.invalid || this.guardando() || this.stockNegativo() || (!this.esIngreso && cantidad === 0)) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { costoUnitario, motivo } = this.formulario.getRawValue();
    const id = this.datos.consumible.id;
    const operacion = this.esIngreso
      ? this.gestion.ingresar(id, {
          cantidad,
          costoUnitario: costoUnitario === null || `${costoUnitario}` === '' ? null : Number(costoUnitario),
          motivo,
        })
      : this.gestion.ajustar(id, { cantidad, motivo });
    guardarYCerrar(operacion, this.dialogo, this.guardando, (error) => this.notificaciones.error(error));
  }
}

/** Ultimos movimientos de stock del consumible. */
@Component({
  selector: 'app-dialogo-movimientos-consumible',
  imports: [DatePipe, DecimalPipe, EtiquetaPipe, MatButtonModule, MatDialogModule, MatTableModule],
  template: `
    <h2 mat-dialog-title>Movimientos · {{ consumible.nombre }}</h2>
    <mat-dialog-content>
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="movimientos()">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let fila">{{ fila.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let fila">{{ fila.tipo | etiqueta: tipos }}</td>
          </ng-container>
          <ng-container matColumnDef="cantidad">
            <th mat-header-cell *matHeaderCellDef class="numero">Cantidad</th>
            <td mat-cell *matCellDef="let fila" class="numero">{{ fila.cantidad > 0 ? '+' : '' }}{{ fila.cantidad | number: '1.0-2' }}</td>
          </ng-container>
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef>Detalle</th>
            <td mat-cell *matCellDef="let fila" class="texto-secundario">{{ fila.motivo ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="creador">
            <th mat-header-cell *matHeaderCellDef>Registrado por</th>
            <td mat-cell *matCellDef="let fila" class="texto-secundario">{{ fila.nombreCreador ?? '—' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">Sin movimientos.</td>
          </tr>
        </table>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoMovimientosConsumible {
  protected readonly consumible = inject<Consumible>(MAT_DIALOG_DATA);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly tipos = TIPOS_MOVIMIENTO_CONSUMIBLE;
  protected readonly columnas = COLUMNAS_MOVIMIENTOS;
  protected readonly movimientos = toSignal(
    inject(GestionConsumibles)
      .movimientos(this.consumible.id, { pagina: 0, tamano: MOVIMIENTOS_VISIBLES })
      .pipe(
        map((pagina) => [...pagina.contenido]),
        catchError((error: unknown) => {
          this.notificaciones.error(error);
          return of<MovimientoConsumible[]>([]);
        }),
      ),
    { initialValue: [] as MovimientoConsumible[] },
  );
}
