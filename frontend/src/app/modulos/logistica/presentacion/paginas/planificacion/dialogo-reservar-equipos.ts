import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, distinctUntilChanged, finalize, of, startWith, switchMap, tap } from 'rxjs';

import { ESPERA_BUSQUEDA_MS } from '../../../../../compartido/utilidades/busqueda-remota';
import { conflictosDeError } from '../../../../../compartido/utilidades/errores-http';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionReservas } from '../../../aplicacion/gestion-reservas';
import { PlanEvento } from '../../../dominio/modelos/planificacion';
import {
  ActivoReservable,
  ConflictoReserva,
  Reserva,
  alternarSeleccion,
  categoriasDe,
} from '../../../dominio/modelos/reservas';

const TODAS_LAS_CATEGORIAS = '';

export interface DatosDialogoReserva {
  readonly plan: PlanEvento;
}

/**
 * Busca equipos libres durante el periodo del plan y los reserva juntos. Si
 * alguno quedo ocupado por otro evento, muestra los conflictos sin cerrar.
 */
@Component({
  selector: 'app-dialogo-reservar-equipos',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Reservar equipos</h2>
    <mat-dialog-content>
      <p class="indicacion">
        Periodo de la reserva: {{ datos.plan.ventanaInicio | date: 'dd/MM/yyyy HH:mm' }} –
        {{ datos.plan.ventanaFin | date: 'dd/MM/yyyy HH:mm' }}. Solo se muestran equipos por unidad disponibles en
        ese periodo.
      </p>
      <div class="filtros">
        <mat-form-field appearance="outline">
          <mat-label>Buscar por código, serie o modelo</mat-label>
          <input matInput [formControl]="texto" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Categoría</mat-label>
          <mat-select [value]="categoria()" (valueChange)="categoria.set($event)">
            <mat-option [value]="todas">Todas</mat-option>
            @for (nombre of categorias(); track nombre) {
              <mat-option [value]="nombre">{{ nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (conflictos().length) {
        <div class="aviso aviso--alerta" role="alert">
          <mat-icon>event_busy</mat-icon>
          <div>
            <p>No se reservó ningún equipo: estos ya están ocupados en ese periodo.</p>
            <ul>
              @for (conflicto of conflictos(); track $index) {
                <li>
                  <strong>{{ conflicto.codigoActivo }}</strong> · {{ conflicto.evento }}
                  ({{ conflicto.salida | date: 'dd/MM HH:mm' }} – {{ conflicto.retorno | date: 'dd/MM HH:mm' }})
                </li>
              }
            </ul>
          </div>
        </div>
      }

      @if (buscando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <ul class="equipos" aria-label="Equipos disponibles">
        @for (activo of visibles(); track activo.id) {
          <li>
            <mat-checkbox [checked]="seleccion().has(activo.id)" (change)="alternar(activo.id)">
              <strong>{{ activo.codigo }}</strong> · {{ activo.modelo }}
              <span class="texto-secundario">· {{ activo.categoria }}</span>
            </mat-checkbox>
          </li>
        } @empty {
          @if (!buscando()) {
            <li class="texto-secundario">No hay equipos disponibles con ese criterio.</li>
          }
        }
      </ul>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button type="button" [disabled]="!seleccion().size || guardando()" (click)="reservar()">
        Reservar{{ seleccion().size ? ' (' + seleccion().size + ')' : '' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .filtros {
      display: grid;
      grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
      gap: 12px;
    }

    .equipos {
      list-style: none;
      margin: 0;
      padding: 0;
      max-height: 320px;
      overflow-y: auto;
    }

    .aviso {
      margin-bottom: 16px;
    }

    .aviso p,
    .aviso ul {
      margin: 0;
    }

    @media (max-width: 599px) {
      .filtros {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoReservarEquipos {
  private readonly gestion = inject(GestionReservas);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoReservarEquipos, Reserva[]>);
  protected readonly datos = inject<DatosDialogoReserva>(MAT_DIALOG_DATA);
  protected readonly todas = TODAS_LAS_CATEGORIAS;
  protected readonly texto = new FormControl('', { nonNullable: true });
  protected readonly categoria = signal(TODAS_LAS_CATEGORIAS);
  protected readonly seleccion = signal<ReadonlySet<number>>(new Set());
  protected readonly conflictos = signal<readonly ConflictoReserva[]>([]);
  protected readonly buscando = signal(true);
  protected readonly guardando = signal(false);

  private readonly disponibles = toSignal(
    this.texto.valueChanges.pipe(
      startWith(this.texto.value),
      debounceTime(ESPERA_BUSQUEDA_MS),
      distinctUntilChanged(),
      tap(() => this.buscando.set(true)),
      switchMap((texto) =>
        this.gestion.disponibles(this.datos.plan.evento.id, texto).pipe(
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of<ActivoReservable[]>([]);
          }),
          finalize(() => this.buscando.set(false)),
        ),
      ),
    ),
    { initialValue: [] as ActivoReservable[] },
  );

  protected readonly categorias = computed(() => categoriasDe(this.disponibles()));
  protected readonly visibles = computed(() => {
    const categoria = this.categoria();
    return categoria === TODAS_LAS_CATEGORIAS
      ? this.disponibles()
      : this.disponibles().filter((activo) => activo.categoria === categoria);
  });

  protected alternar(id: number): void {
    this.seleccion.update((actual) => alternarSeleccion(actual, id));
  }

  protected reservar(): void {
    this.guardando.set(true);
    this.conflictos.set([]);
    this.gestion.reservar(this.datos.plan.evento.id, [...this.seleccion()]).subscribe({
      next: (reservas) => this.dialogo.close(reservas),
      error: (error: unknown) => {
        const conflictos = conflictosDeError<ConflictoReserva>(error);
        if (conflictos.length) {
          this.conflictos.set(conflictos);
        } else {
          this.notificaciones.error(error);
        }
        this.guardando.set(false);
      },
    });
  }
}
