import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, map, of } from 'rxjs';

import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { requiereSeleccion } from '../../../../../compartido/utilidades/validadores-seleccion';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { Proyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionConsumibles, GestionConsumos } from '../../../aplicacion/gestion-subcontratacion';
import { Consumible } from '../../../dominio/modelos/consumible';
import { Consumo, EventoAsignado } from '../../../dominio/modelos/consumo';
import { CamposProyectoEvento } from '../../componentes/campos-proyecto-evento';

/** El catalogo de consumibles activos cabe en una pagina de la API. */
const MAXIMO_CONSUMIBLES = 100;
const CANTIDAD_MINIMA = 0.01;

export interface DatosDialogoConsumo {
  /** Tecnico: solo elige entre los eventos donde esta asignado. */
  readonly limitadoAEventosAsignados: boolean;
}

/** Registra lo que se uso de un consumible; descuenta el stock en el momento. */
@Component({
  selector: 'app-dialogo-consumo',
  imports: [
    CamposProyectoEvento,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Registrar consumo</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Consumible</mat-label>
            <mat-select formControlName="idConsumible">
              @for (opcion of consumibles(); track opcion.id) {
                <mat-option [value]="opcion.id" [disabled]="opcion.stockActual <= 0">
                  {{ opcion.nombre }}
                  <span class="texto-secundario">· {{ opcion.stockActual | number: '1.0-2' }} {{ opcion.unidadMedida }} disponibles</span>
                </mat-option>
              }
            </mat-select>
            <mat-error>Elija el consumible</mat-error>
          </mat-form-field>
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
              <mat-error>Elija el evento donde se usó</mat-error>
            </mat-form-field>
          } @else {
            <app-campos-proyecto-evento [proyecto]="proyecto" [evento]="evento" />
          }
          <mat-form-field appearance="outline">
            <mat-label>Cantidad{{ elegido() ? ' (' + elegido()!.unidadMedida + ')' : '' }}</mat-label>
            <input matInput type="number" formControlName="cantidad" min="0.01" step="0.01" />
            @if (elegido(); as consumible) {
              <mat-hint>Disponible: {{ consumible.stockActual | number: '1.0-2' }}</mat-hint>
            }
            <mat-error>Indique una cantidad mayor a cero</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fecha</mat-label>
            <input matInput type="date" formControlName="fecha" [max]="hoy" />
            <mat-error>Indique la fecha</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Registrar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoConsumo {
  protected readonly datos = inject<DatosDialogoConsumo>(MAT_DIALOG_DATA);
  private readonly gestion = inject(GestionConsumos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoConsumo, Consumo>);

  protected readonly hoy = aFechaIso(new Date());
  protected readonly guardando = signal(false);
  protected readonly proyecto = new FormControl<Proyecto | string | null>(null, requiereSeleccion);
  protected readonly evento = new FormControl<number | null>(null);
  protected readonly eventoAsignado = new FormControl<EventoAsignado | null>(null, Validators.required);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    idConsumible: [0, Validators.min(1)],
    cantidad: [null as number | null, [Validators.required, Validators.min(CANTIDAD_MINIMA)]],
    fecha: [this.hoy, Validators.required],
  });

  protected readonly consumibles = toSignal(
    inject(GestionConsumibles)
      .listar({ activo: true }, { pagina: 0, tamano: MAXIMO_CONSUMIBLES })
      .pipe(map((pagina) => [...pagina.contenido]), catchError(() => of<Consumible[]>([]))),
    { initialValue: [] as Consumible[] },
  );
  protected readonly eventosAsignados = toSignal(
    this.datos.limitadoAEventosAsignados
      ? this.gestion.eventosAsignados().pipe(catchError(() => of<EventoAsignado[]>([])))
      : of<EventoAsignado[]>([]),
    { initialValue: [] as EventoAsignado[] },
  );
  private readonly idConsumible = toSignal(this.formulario.controls.idConsumible.valueChanges, { initialValue: 0 });
  protected readonly elegido = computed(() => this.consumibles().find((opcion) => opcion.id === this.idConsumible()));

  protected guardar(): void {
    const destino = this.destino();
    if (this.formulario.invalid || !destino || this.guardando()) {
      this.formulario.markAllAsTouched();
      this.proyecto.markAsTouched();
      this.eventoAsignado.markAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.registrar({ ...destino, idConsumible: valores.idConsumible, cantidad: Number(valores.cantidad), fecha: valores.fecha }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }

  private destino(): { idProyecto: number; idEvento: number | null } | null {
    if (this.datos.limitadoAEventosAsignados) {
      const asignado = this.eventoAsignado.value;
      return asignado ? { idProyecto: asignado.idProyecto, idEvento: asignado.idEvento } : null;
    }
    const proyecto = this.proyecto.value;
    return typeof proyecto === 'object' && proyecto ? { idProyecto: proyecto.id, idEvento: this.evento.value } : null;
  }
}
