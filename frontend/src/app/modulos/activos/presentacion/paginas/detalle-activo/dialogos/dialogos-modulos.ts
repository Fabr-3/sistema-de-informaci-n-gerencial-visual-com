import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { catchError, map, of } from 'rxjs';

import { EtiquetaPipe } from '../../../../../../compartido/pipes/etiqueta.pipe';
import { guardarYCerrar } from '../../../../../../compartido/utilidades/guardar-y-cerrar';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionActivos } from '../../../../aplicacion/gestion-activos';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo, fueraDeInventario } from '../../../../dominio/modelos/activo';
import {
  ESTADOS_MODULO,
  EntradaHistorialModulo,
  ModuloLed,
  TIPOS_CAMBIO_MODULO,
  detalleCambio,
} from '../../../../dominio/modelos/historial';

const LONGITUD_CODIGO_MODULO = 50;
const LONGITUD_MOTIVO = 500;
/** Pantallas candidatas a recibir el modulo; la API admite hasta 100 por pagina. */
const MAXIMO_PANTALLAS = 100;
const COLUMNAS_HISTORIAL = ['fecha', 'tipo', 'detalle', 'motivo', 'registradoPor'];

export interface DatosDialogoModulo {
  readonly activo: Activo;
  readonly modulo: ModuloLed;
}

/** Retira el modulo y registra el que ocupa su lugar en la misma pantalla. */
@Component({
  selector: 'app-dialogo-reemplazo-modulo',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Reemplazar módulo {{ datos.modulo.codigo }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">
          El módulo actual ({{ estados[datos.modulo.estado].toLowerCase() }}) quedará retirado y el nuevo, activo, en
          {{ datos.activo.codigoInventario }}.
        </p>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Código del módulo nuevo</mat-label>
            <input matInput formControlName="codigoNuevo" [maxlength]="longitudCodigo" />
            <mat-error>Indique el código del módulo nuevo</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Motivo</mat-label>
            <textarea matInput rows="2" formControlName="motivo" [maxlength]="longitudMotivo"></textarea>
            <mat-error>Indique el motivo</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Reemplazar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoReemplazoModulo {
  protected readonly datos = inject<DatosDialogoModulo>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoReemplazoModulo, ModuloLed>);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly estados = ESTADOS_MODULO;
  protected readonly longitudCodigo = LONGITUD_CODIGO_MODULO;
  protected readonly longitudMotivo = LONGITUD_MOTIVO;
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    codigoNuevo: ['', [Validators.required, Validators.maxLength(LONGITUD_CODIGO_MODULO)]],
    motivo: ['', [Validators.required, Validators.maxLength(LONGITUD_MOTIVO)]],
  });

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.historial.reemplazarModulo(this.datos.activo.id, this.datos.modulo.id, this.formulario.getRawValue()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

/** Pasa un modulo activo a otra pantalla de la misma categoria que siga en el inventario. */
@Component({
  selector: 'app-dialogo-reasignacion-modulo',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Reasignar módulo {{ datos.modulo.codigo }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">El módulo dejará {{ datos.activo.codigoInventario }} y pasará a la pantalla elegida.</p>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Pantalla de destino</mat-label>
            <mat-select formControlName="idActivoDestino">
              @for (pantalla of pantallas(); track pantalla.id) {
                <mat-option [value]="pantalla.id">{{ pantalla.codigoInventario }} · {{ pantalla.modelo }}</mat-option>
              } @empty {
                <mat-option disabled>No hay otras pantallas de {{ datos.activo.categoria }} en el inventario</mat-option>
              }
            </mat-select>
            <mat-error>Seleccione la pantalla de destino</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Motivo</mat-label>
            <textarea matInput rows="2" formControlName="motivo" [maxlength]="longitudMotivo"></textarea>
            <mat-error>Indique el motivo</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Reasignar</button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoReasignacionModulo {
  protected readonly datos = inject<DatosDialogoModulo>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoReasignacionModulo, ModuloLed>);
  private readonly historial = inject(HistorialActivos);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly longitudMotivo = LONGITUD_MOTIVO;
  protected readonly guardando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    idActivoDestino: [0, Validators.min(1)],
    motivo: ['', [Validators.required, Validators.maxLength(LONGITUD_MOTIVO)]],
  });
  protected readonly pantallas = toSignal(
    inject(GestionActivos)
      .listar({ idCategoria: this.datos.activo.idCategoria }, { pagina: 0, tamano: MAXIMO_PANTALLAS })
      .pipe(
        map((pagina) =>
          pagina.contenido.filter(
            (pantalla) => pantalla.id !== this.datos.activo.id && !fueraDeInventario(pantalla.estado),
          ),
        ),
        catchError((error: unknown) => {
          this.notificaciones.error(error);
          return of<Activo[]>([]);
        }),
      ),
    { initialValue: [] as Activo[] },
  );

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    guardarYCerrar(
      this.historial.reasignarModulo(this.datos.activo.id, this.datos.modulo.id, this.formulario.getRawValue()),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }
}

/** Alta, cambios de estado, reemplazos y reasignaciones del modulo, en orden cronologico. */
@Component({
  selector: 'app-dialogo-historial-modulo',
  imports: [DatePipe, EtiquetaPipe, MatButtonModule, MatDialogModule, MatTableModule],
  template: `
    <h2 mat-dialog-title>Historial del módulo {{ datos.modulo.codigo }}</h2>
    <mat-dialog-content>
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="entradas()">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let entrada">{{ entrada.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Cambio</th>
            <td mat-cell *matCellDef="let entrada"><strong>{{ entrada.tipo | etiqueta: tipos }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="detalle">
            <th mat-header-cell *matHeaderCellDef>Detalle</th>
            <td mat-cell *matCellDef="let entrada">{{ detalleCambio(entrada) }}</td>
          </ng-container>
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef>Motivo</th>
            <td mat-cell *matCellDef="let entrada" class="texto-secundario">{{ entrada.motivo ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="registradoPor">
            <th mat-header-cell *matHeaderCellDef>Registrado por</th>
            <td mat-cell *matCellDef="let entrada" class="texto-secundario">{{ entrada.registradoPor ?? '—' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">Sin cambios registrados.</td>
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
export class DialogoHistorialModulo {
  protected readonly datos = inject<DatosDialogoModulo>(MAT_DIALOG_DATA);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly tipos = TIPOS_CAMBIO_MODULO;
  protected readonly columnas = COLUMNAS_HISTORIAL;
  protected readonly detalleCambio = detalleCambio;
  protected readonly entradas = toSignal(
    inject(HistorialActivos)
      .historialModulo(this.datos.activo.id, this.datos.modulo.id)
      .pipe(
        catchError((error: unknown) => {
          this.notificaciones.error(error);
          return of<EntradaHistorialModulo[]>([]);
        }),
      ),
    { initialValue: [] as EntradaHistorialModulo[] },
  );
}
