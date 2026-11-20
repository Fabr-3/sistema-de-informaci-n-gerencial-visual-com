import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, debounceTime, distinctUntilChanged, finalize, map, of, startWith, switchMap, tap } from 'rxjs';

import { CampoFechaHora } from '../../../../../compartido/componentes/campo-fecha-hora/campo-fecha-hora';
import { ESPERA_BUSQUEDA_MS } from '../../../../../compartido/utilidades/busqueda-remota';
import { finPosteriorAInicio, noFutura } from '../../../../../compartido/utilidades/validadores-periodo';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionActivos } from '../../../aplicacion/gestion-activos';
import { HistorialActivos } from '../../../aplicacion/historial-activos';
import { Activo } from '../../../dominio/modelos/activo';
import { RegistroUso } from '../../../dominio/modelos/historial';

const LONGITUD_DESCRIPCION = 255;
const RESULTADOS = 50;

/**
 * Uso grupal: registra el mismo periodo de uso (ya realizado) para varios
 * equipos a la vez, por ejemplo todos los que se usaron en un evento.
 */
@Component({
  selector: 'app-dialogo-uso-grupal',
  imports: [
    CampoFechaHora,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Registrar uso de varios equipos</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          Elija los equipos que se usaron juntos y el periodo de uso. Se registran todos o ninguno: si alguno ya
          tiene un uso en ese periodo, se indica cuál.
        </p>
        <mat-form-field appearance="outline" class="buscador" subscriptSizing="dynamic">
          <mat-label>Buscar por código, serie o modelo</mat-label>
          <input matInput [formControl]="texto" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        @if (buscando()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <ul class="equipos" aria-label="Equipos">
          @for (activo of activos(); track activo.id) {
            <li>
              <mat-checkbox [checked]="seleccion().has(activo.id)" (change)="alternar(activo)">
                <strong>{{ activo.codigoInventario }}</strong> · {{ activo.modelo }}
                <span class="texto-secundario">· {{ activo.categoria }}</span>
              </mat-checkbox>
            </li>
          } @empty {
            @if (!buscando()) {
              <li class="texto-secundario">No hay equipos con ese criterio.</li>
            }
          }
        </ul>
        <p class="seleccionados">
          @if (seleccion().size) {
            Seleccionados ({{ seleccion().size }}): {{ codigosSeleccionados() }}
          } @else {
            <span class="texto-secundario">Aún no eligió equipos.</span>
          }
        </p>
        <div class="formulario">
          <app-campo-fecha-hora class="completo" formControlName="fechaInicio" etiqueta="Inicio del uso" mensajeError="Indique un inicio que no sea futuro" />
          <app-campo-fecha-hora class="completo" formControlName="fechaFin" etiqueta="Fin del uso" mensajeError="Indique un fin que no sea futuro" />
          @if (formulario.hasError('periodoInvalido')) {
            <p class="completo error-grupo" role="alert">El fin debe ser posterior al inicio</p>
          }
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción (opcional)</mat-label>
            <input matInput formControlName="descripcion" placeholder="Evento, cliente o motivo" />
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="!seleccion().size || guardando()">
          Registrar{{ seleccion().size ? ' (' + seleccion().size + ')' : '' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .buscador {
      width: 100%;
    }

    .equipos {
      list-style: none;
      margin: 0;
      padding: 0;
      max-height: 220px;
      overflow-y: auto;
    }

    .seleccionados {
      margin: 8px 0 16px;
      font-size: 13px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoUsoGrupal {
  private readonly gestion = inject(GestionActivos);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoUsoGrupal, RegistroUso[]>);

  protected readonly texto = new FormControl('', { nonNullable: true });
  protected readonly buscando = signal(true);
  protected readonly guardando = signal(false);
  /** Seleccion por id con el codigo, para mostrarla aunque cambie la busqueda. */
  protected readonly seleccion = signal<ReadonlyMap<number, string>>(new Map());
  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      fechaInicio: ['', [Validators.required, noFutura]],
      fechaFin: ['', [Validators.required, noFutura]],
      descripcion: ['', Validators.maxLength(LONGITUD_DESCRIPCION)],
    },
    { validators: finPosteriorAInicio('fechaInicio', 'fechaFin') },
  );

  /** Solo equipos en el inventario (disponibles o en uso). */
  protected readonly activos = toSignal(
    this.texto.valueChanges.pipe(
      startWith(this.texto.value),
      debounceTime(ESPERA_BUSQUEDA_MS),
      distinctUntilChanged(),
      tap(() => this.buscando.set(true)),
      switchMap((texto) =>
        this.gestion.listar({ texto }, { pagina: 0, tamano: RESULTADOS }).pipe(
          map((pagina) => pagina.contenido.filter((activo) => activo.estado !== 'BAJA' && activo.estado !== 'VENDIDO')),
          catchError((error: unknown) => {
            this.notificaciones.error(error);
            return of<Activo[]>([]);
          }),
          finalize(() => this.buscando.set(false)),
        ),
      ),
    ),
    { initialValue: [] as Activo[] },
  );

  protected codigosSeleccionados(): string {
    return [...this.seleccion().values()].join(', ');
  }

  protected alternar(activo: Activo): void {
    this.seleccion.update((actual) => {
      const nueva = new Map(actual);
      if (!nueva.delete(activo.id)) {
        nueva.set(activo.id, activo.codigoInventario);
      }
      return nueva;
    });
  }

  protected guardar(): void {
    if (this.formulario.invalid || !this.seleccion().size || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { fechaInicio, fechaFin, descripcion } = this.formulario.getRawValue();
    this.guardando.set(true);
    this.historial
      .registrarUsoGrupal([...this.seleccion().keys()], {
        fechaInicio,
        fechaFin,
        descripcion: descripcion.trim() || null,
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (registros) => this.dialogo.close(registros),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }
}
