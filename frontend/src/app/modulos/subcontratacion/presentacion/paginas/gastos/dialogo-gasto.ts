import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, of } from 'rxjs';

import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { requiereSeleccion } from '../../../../../compartido/utilidades/validadores-seleccion';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { Proyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionConsumos, GestionGastos } from '../../../aplicacion/gestion-subcontratacion';
import { EventoAsignado } from '../../../dominio/modelos/consumo';
import { GastoEvento, TIPOS_COMPROBANTE, problemaComprobante } from '../../../dominio/modelos/gasto';
import { CamposProyectoEvento } from '../../componentes/campos-proyecto-evento';

const LONGITUD_DESCRIPCION = 500;
const MONTO_MINIMO = 0.01;

export interface DatosDialogoGasto {
  /** Tecnico: solo elige entre los eventos donde esta asignado. */
  readonly limitadoAEventosAsignados: boolean;
}

/**
 * Registra un gasto del evento que no esta en el catalogo de consumibles,
 * con su comprobante (foto o PDF). Se registra como egreso del proyecto y
 * del evento en Finanzas.
 */
@Component({
  selector: 'app-dialogo-gasto',
  imports: [
    CamposProyectoEvento,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Registrar gasto del evento</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          Para compras o pagos que no están en el catálogo de consumibles (un repuesto, un pasaje, material de
          último momento). Se registra como egreso del evento en Finanzas.
        </p>
        <div class="formulario">
          @if (datos.limitadoAEventosAsignados) {
            <mat-form-field appearance="outline" class="completo">
              <mat-label>Evento</mat-label>
              <mat-select [formControl]="eventoAsignado">
                @for (opcion of eventosAsignados(); track opcion.idEvento) {
                  <mat-option [value]="opcion">
                    {{ opcion.evento }} <span class="texto-secundario">· {{ opcion.fechaInicio | date: 'dd/MM/yyyy' }} · {{ opcion.proyecto }}</span>
                  </mat-option>
                } @empty {
                  <mat-option disabled>No tiene asignaciones en eventos recientes</mat-option>
                }
              </mat-select>
              <mat-error>Elija el evento donde hizo el gasto</mat-error>
            </mat-form-field>
          } @else {
            <app-campos-proyecto-evento [proyecto]="proyecto" [evento]="evento" />
            @if (evento.touched && evento.invalid) {
              <p class="completo error-grupo" role="alert">Elija el evento del gasto</p>
            }
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción</mat-label>
            <input matInput formControlName="descripcion" placeholder="Qué se compró o pagó y por qué" />
            <mat-error>Describa el gasto</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Monto (Bs)</mat-label>
            <input matInput type="number" formControlName="monto" min="0.01" step="0.01" />
            <mat-error>Indique un monto mayor a cero</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fecha</mat-label>
            <input matInput type="date" formControlName="fecha" [max]="hoy" />
            <mat-error>Indique la fecha</mat-error>
          </mat-form-field>
          <div class="completo comprobante">
            <input #selector type="file" hidden [accept]="tiposAceptados" (change)="elegirArchivo(selector)" />
            <button mat-stroked-button type="button" (click)="selector.click()">
              <mat-icon>{{ archivo() ? 'swap_horiz' : 'attach_file' }}</mat-icon>{{ archivo() ? 'Cambiar comprobante' : 'Adjuntar comprobante' }}
            </button>
            @if (archivo(); as elegido) {
              <span class="comprobante__nombre"><mat-icon>receipt</mat-icon>{{ elegido.name }}</span>
            } @else {
              <span class="texto-secundario" [class.error-grupo]="faltaArchivo()">Foto o PDF del recibo o factura (máx. 5 MB)</span>
            }
          </div>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Registrar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .comprobante {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
    }

    .comprobante__nombre {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-weight: 500;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoGasto {
  protected readonly datos = inject<DatosDialogoGasto>(MAT_DIALOG_DATA);
  private readonly gestion = inject(GestionGastos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoGasto, GastoEvento>);

  protected readonly hoy = aFechaIso(new Date());
  protected readonly tiposAceptados = TIPOS_COMPROBANTE.join(',');
  protected readonly guardando = signal(false);
  protected readonly archivo = signal<File | null>(null);
  protected readonly faltaArchivo = signal(false);
  protected readonly proyecto = new FormControl<Proyecto | string | null>(null, requiereSeleccion);
  protected readonly evento = new FormControl<number | null>(null, Validators.required);
  protected readonly eventoAsignado = new FormControl<EventoAsignado | null>(null, Validators.required);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    descripcion: ['', [Validators.required, Validators.maxLength(LONGITUD_DESCRIPCION)]],
    monto: [null as number | null, [Validators.required, Validators.min(MONTO_MINIMO)]],
    fecha: [this.hoy, Validators.required],
  });

  protected readonly eventosAsignados = toSignal(
    this.datos.limitadoAEventosAsignados
      ? inject(GestionConsumos).eventosAsignados().pipe(catchError(() => of<EventoAsignado[]>([])))
      : of<EventoAsignado[]>([]),
    { initialValue: [] as EventoAsignado[] },
  );

  /** Valida tipo y tamano en el navegador; el servidor vuelve a validar el contenido. */
  protected elegirArchivo(selector: HTMLInputElement): void {
    const elegido = selector.files?.[0];
    selector.value = '';
    if (!elegido) {
      return;
    }
    const problema = problemaComprobante(elegido);
    if (problema) {
      this.notificaciones.advertencia(problema);
      return;
    }
    this.archivo.set(elegido);
    this.faltaArchivo.set(false);
  }

  protected guardar(): void {
    const destino = this.destino();
    const archivo = this.archivo();
    if (this.formulario.invalid || !destino || !archivo || this.guardando()) {
      this.formulario.markAllAsTouched();
      this.proyecto.markAsTouched();
      this.evento.markAsTouched();
      this.eventoAsignado.markAsTouched();
      this.faltaArchivo.set(!archivo);
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.registrar(
        { ...destino, descripcion: valores.descripcion, monto: Number(valores.monto), fecha: valores.fecha },
        archivo,
      ),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }

  private destino(): { idProyecto: number; idEvento: number } | null {
    if (this.datos.limitadoAEventosAsignados) {
      const asignado = this.eventoAsignado.value;
      return asignado ? { idProyecto: asignado.idProyecto, idEvento: asignado.idEvento } : null;
    }
    const proyecto = this.proyecto.value;
    const idEvento = this.evento.value;
    return typeof proyecto === 'object' && proyecto && idEvento ? { idProyecto: proyecto.id, idEvento } : null;
  }
}
