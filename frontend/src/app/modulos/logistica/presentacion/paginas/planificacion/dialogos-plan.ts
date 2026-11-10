import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, startWith, switchMap } from 'rxjs';

import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { finPosteriorAInicio } from '../../../../../compartido/utilidades/validadores-periodo';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ElementoCatalogo } from '../../../../comercial/dominio/modelos/catalogos';
import { GestionPlanificacion } from '../../../aplicacion/gestion-planificacion';
import { PersonalDisponible, asignadoEnEvento, crucesConOtrosEventos } from '../../../dominio/modelos/personal';
import {
  Asignacion,
  Jornada,
  PlanEvento,
  TIPOS_JORNADA,
  periodosSugeridos,
} from '../../../dominio/modelos/planificacion';

const LONGITUD_DESCRIPCION = 500;
const RETARDO_CONSULTA_MS = 300;
/** Recorta "AAAA-MM-DDTHH:mm:ss" al formato del campo datetime-local. */
const LONGITUD_FECHA_HORA = 16;

const fechaHora = (valor: string | undefined): string => valor?.substring(0, LONGITUD_FECHA_HORA) ?? '';

export interface DatosDialogoJornada {
  readonly plan: PlanEvento;
  readonly jornada?: Jornada;
}

/** Registro o edicion de una jornada de montaje o desmontaje. */
@Component({
  selector: 'app-dialogo-jornada',
  imports: [
    DatePipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ datos.jornada ? 'Editar jornada' : 'Nueva jornada' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          El montaje debe terminar antes del inicio del evento ({{ evento.fechaInicio | date: 'dd/MM/yyyy HH:mm' }}) y
          el desmontaje comenzar después de su fin ({{ evento.fechaFin | date: 'dd/MM/yyyy HH:mm' }}).
        </p>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Tipo</mat-label>
            <mat-select formControlName="tipo">
              @for (tipo of tipos | keyvalue: ordenOriginal; track tipo.key) {
                <mat-option [value]="tipo.key">{{ tipo.value }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput type="datetime-local" formControlName="inicio" />
            <mat-error>Indique el inicio</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput type="datetime-local" formControlName="fin" />
            <mat-error>Indique el fin</mat-error>
          </mat-form-field>
          @if (formulario.hasError('periodoInvalido')) {
            <p class="completo error-grupo" role="alert">El fin debe ser posterior al inicio</p>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción (opcional)</mat-label>
            <textarea matInput rows="2" formControlName="descripcion" placeholder="Estructura, pantalla central, cableado…"></textarea>
            <mat-error>Máximo {{ longitudDescripcion }} caracteres</mat-error>
          </mat-form-field>
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
export class DialogoJornada {
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionPlanificacion);
  private readonly dialogo = inject(MatDialogRef<DialogoJornada, PlanEvento>);
  protected readonly datos = inject<DatosDialogoJornada>(MAT_DIALOG_DATA);
  protected readonly evento = this.datos.plan.evento;
  protected readonly tipos = TIPOS_JORNADA;
  protected readonly longitudDescripcion = LONGITUD_DESCRIPCION;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      tipo: [this.datos.jornada?.tipo ?? ('MONTAJE' as keyof typeof TIPOS_JORNADA)],
      inicio: [fechaHora(this.datos.jornada?.inicio), Validators.required],
      fin: [fechaHora(this.datos.jornada?.fin), Validators.required],
      descripcion: [this.datos.jornada?.descripcion ?? '', Validators.maxLength(LONGITUD_DESCRIPCION)],
    },
    { validators: finPosteriorAInicio('inicio', 'fin') },
  );

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.gestion.guardarJornada(this.evento.id, this.datos.jornada?.id ?? null, this.formulario.getRawValue()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

export interface DatosDialogoAsignacion {
  readonly plan: PlanEvento;
  readonly roles: readonly ElementoCatalogo[];
  readonly asignacion?: Asignacion;
}

/**
 * Asignacion de una persona a un horario del plan. La lista de personal
 * muestra quien ya trabaja en otro evento en ese horario: se permite, pero
 * se advierte el cruce.
 */
@Component({
  selector: 'app-dialogo-asignacion',
  imports: [
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
    <h2 mat-dialog-title>{{ datos.asignacion ? 'Editar asignación' : 'Asignar personal' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          Dentro del plan: {{ datos.plan.ventanaInicio | date: 'dd/MM/yyyy HH:mm' }} –
          {{ datos.plan.ventanaFin | date: 'dd/MM/yyyy HH:mm' }}.
        </p>
        <div class="sugerencias" role="group" aria-label="Horarios sugeridos">
          @for (periodo of sugeridos; track periodo.etiqueta) {
            <button mat-stroked-button type="button" (click)="usarPeriodo(periodo.inicio, periodo.fin)">
              {{ periodo.etiqueta }}
            </button>
          }
        </div>
        <div class="formulario">
          <mat-form-field appearance="outline">
            <mat-label>Inicio</mat-label>
            <input matInput type="datetime-local" formControlName="inicio" />
            <mat-error>Indique el inicio</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fin</mat-label>
            <input matInput type="datetime-local" formControlName="fin" />
            <mat-error>Indique el fin</mat-error>
          </mat-form-field>
          @if (formulario.hasError('periodoInvalido')) {
            <p class="completo error-grupo" role="alert">El fin debe ser posterior al inicio</p>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Persona</mat-label>
            <mat-select formControlName="idPersonal">
              @for (persona of personal(); track persona.id) {
                @let enEsteEvento = asignadoEnEvento(persona, datos.plan.evento.id);
                @let otrosEventos = crucesConOtrosEventos(persona, datos.plan.evento.id);
                <mat-option [value]="persona.id" [disabled]="enEsteEvento">
                  {{ persona.nombreCompleto }}
                  @if (enEsteEvento) {
                    <span class="texto-secundario">· ya asignado en este evento en ese horario</span>
                  } @else if (otrosEventos.length) {
                    <span class="ocupado">· ocupado en {{ otrosEventos[0].evento }}</span>
                  } @else if (persona.especialidades.length) {
                    <span class="texto-secundario">· {{ persona.especialidades.join(', ') }}</span>
                  }
                </mat-option>
              } @empty {
                <mat-option disabled>Indique un horario válido para ver al personal</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione a la persona</mat-error>
          </mat-form-field>
          @if (crucesElegido().length) {
            <div class="completo aviso aviso--alerta" role="status">
              <mat-icon>warning</mat-icon>
              <span>
                En ese horario también está asignado a
                @for (cruce of crucesElegido(); track cruce.idAsignacion; let ultimo = $last) {
                  {{ cruce.evento }} ({{ cruce.inicio | date: 'dd/MM HH:mm' }}–{{ cruce.fin | date: 'HH:mm' }}){{ ultimo ? '.' : ', ' }}
                }
                Se puede asignar, pero revise el cruce.
              </span>
            </div>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Rol en el evento</mat-label>
            <mat-select formControlName="idRol">
              @for (rol of datos.roles; track rol.id) {
                <mat-option [value]="rol.id">{{ rol.nombre }}</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione el rol</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .sugerencias {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 16px;
    }

    .ocupado {
      color: var(--vc-alerta);
    }

    .aviso {
      margin-bottom: 16px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoAsignacion {
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionPlanificacion);
  private readonly dialogo = inject(MatDialogRef<DialogoAsignacion, Asignacion>);
  protected readonly datos = inject<DatosDialogoAsignacion>(MAT_DIALOG_DATA);
  protected readonly sugeridos = periodosSugeridos(this.datos.plan);
  protected readonly asignadoEnEvento = asignadoEnEvento;
  protected readonly crucesConOtrosEventos = crucesConOtrosEventos;
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      inicio: [fechaHora(this.datos.asignacion?.inicio), Validators.required],
      fin: [fechaHora(this.datos.asignacion?.fin), Validators.required],
      idPersonal: [this.datos.asignacion?.personal.id ?? 0, Validators.min(1)],
      idRol: [this.datos.asignacion?.rol.id ?? 0, Validators.min(1)],
    },
    { validators: finPosteriorAInicio('inicio', 'fin') },
  );

  /** Personal activo y sus cruces en el horario elegido (se consulta al cambiar el horario). */
  protected readonly personal = toSignal(
    this.formulario.valueChanges.pipe(
      startWith(this.formulario.getRawValue()),
      map(() => this.formulario.getRawValue()),
      filter(({ inicio, fin }) => !!inicio && !!fin && fin > inicio),
      map(({ inicio, fin }) => `${inicio}|${fin}`),
      distinctUntilChanged(),
      debounceTime(RETARDO_CONSULTA_MS),
      switchMap((clave) => {
        const [inicio, fin] = clave.split('|');
        return this.gestion.disponibles(this.datos.plan.evento.id, inicio, fin, this.datos.asignacion?.id ?? null).pipe(
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of<PersonalDisponible[]>([]);
          }),
        );
      }),
      takeUntilDestroyed(),
    ),
    { initialValue: [] as PersonalDisponible[] },
  );

  private readonly idPersonal = toSignal(this.formulario.controls.idPersonal.valueChanges, {
    initialValue: this.formulario.controls.idPersonal.value,
  });

  protected readonly crucesElegido = computed(() => {
    const elegido = this.personal().find((persona) => persona.id === this.idPersonal());
    return elegido ? crucesConOtrosEventos(elegido, this.datos.plan.evento.id) : [];
  });

  protected usarPeriodo(inicio: string, fin: string): void {
    this.formulario.patchValue({ inicio: fechaHora(inicio), fin: fechaHora(fin) });
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.gestion.guardarAsignacion(
        this.datos.plan.evento.id,
        this.datos.asignacion?.id ?? null,
        this.formulario.getRawValue(),
      ),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}
