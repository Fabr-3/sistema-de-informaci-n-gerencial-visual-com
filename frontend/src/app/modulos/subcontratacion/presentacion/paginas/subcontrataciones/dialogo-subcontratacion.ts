import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { map } from 'rxjs';

import { busquedaRemota } from '../../../../../compartido/utilidades/busqueda-remota';
import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import { requiereSeleccion } from '../../../../../compartido/utilidades/validadores-seleccion';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ElementoCatalogo } from '../../../../comercial/dominio/modelos/catalogos';
import { Proyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionProveedores } from '../../../../finanzas/aplicacion/gestion-proveedores';
import { Proveedor } from '../../../../finanzas/dominio/modelos/proveedor';
import { GestionSubcontrataciones } from '../../../aplicacion/gestion-subcontratacion';
import { Subcontratacion } from '../../../dominio/modelos/subcontratacion';
import { CamposProyectoEvento } from '../../componentes/campos-proyecto-evento';

const SUGERENCIAS = 10;
const LONGITUD_DESCRIPCION = 2000;
const COSTO_MINIMO = 0.01;

/** Registro de un servicio subcontratado; genera su egreso en finanzas. */
@Component({
  selector: 'app-dialogo-subcontratacion',
  imports: [
    CamposProyectoEvento,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Nueva subcontratación</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <p class="indicacion">El costo se registra como egreso del proyecto en Finanzas; para corregirlo, anule la subcontratación.</p>
        <div class="formulario">
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Proveedor</mat-label>
            <input matInput formControlName="proveedor" [matAutocomplete]="autocompletarProveedor" placeholder="Buscar por nombre, NIT o servicio" />
            <mat-autocomplete #autocompletarProveedor="matAutocomplete" [displayWith]="mostrarProveedor">
              @for (proveedor of proveedoresSugeridos(); track proveedor.id) {
                <mat-option [value]="proveedor">{{ proveedor.razonSocial }}</mat-option>
              }
            </mat-autocomplete>
            <mat-error>Elija el proveedor de la lista</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Servicio (opcional)</mat-label>
            <mat-select formControlName="idServicio">
              <mat-option [value]="null">Otro servicio</mat-option>
              @for (servicio of servicios(); track servicio.id) {
                <mat-option [value]="servicio.id">{{ servicio.nombre }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <app-campos-proyecto-evento [proyecto]="proyecto" [evento]="evento" />
          <mat-form-field appearance="outline" class="completo">
            <mat-label>Descripción</mat-label>
            <textarea matInput rows="2" formControlName="descripcion" [maxlength]="longitudDescripcion"></textarea>
            <mat-error>Describa el servicio subcontratado</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Costo (Bs)</mat-label>
            <input matInput type="number" formControlName="costo" min="0.01" step="0.01" />
            <mat-error>Indique un costo mayor a cero</mat-error>
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
export class DialogoSubcontratacion {
  private readonly gestion = inject(GestionSubcontrataciones);
  private readonly proveedores = inject(GestionProveedores);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogo = inject(MatDialogRef<DialogoSubcontratacion, Subcontratacion>);

  protected readonly hoy = aFechaIso(new Date());
  protected readonly longitudDescripcion = LONGITUD_DESCRIPCION;
  protected readonly guardando = signal(false);
  protected readonly servicios = signal<ElementoCatalogo[]>([]);
  protected readonly proyecto = new FormControl<Proyecto | string | null>(null, requiereSeleccion);
  protected readonly evento = new FormControl<number | null>(null);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    proveedor: [null as Proveedor | string | null, requiereSeleccion],
    idServicio: [null as number | null],
    descripcion: ['', [Validators.required, Validators.maxLength(LONGITUD_DESCRIPCION)]],
    costo: [null as number | null, [Validators.required, Validators.min(COSTO_MINIMO)]],
    fecha: [this.hoy, Validators.required],
  });
  protected readonly proveedoresSugeridos = toSignal(
    busquedaRemota(this.formulario.controls.proveedor, (texto) =>
      this.proveedores.listar({ texto, activo: true }, { pagina: 0, tamano: SUGERENCIAS }).pipe(map((pagina) => [...pagina.contenido])),
    ),
    { initialValue: [] as Proveedor[] },
  );
  protected readonly mostrarProveedor = (valor: Proveedor | string | null): string =>
    typeof valor === 'string' ? valor : (valor?.razonSocial ?? '');

  constructor() {
    this.formulario.controls.proveedor.valueChanges.pipe(takeUntilDestroyed()).subscribe((valor) => {
      this.formulario.controls.idServicio.setValue(null);
      this.cargarServicios(typeof valor === 'object' && valor ? valor.id : null);
    });
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.proyecto.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      this.proyecto.markAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.registrar({
        idProveedor: (valores.proveedor as Proveedor).id,
        idServicio: valores.idServicio,
        idProyecto: (this.proyecto.value as Proyecto).id,
        idEvento: this.evento.value,
        descripcion: valores.descripcion,
        costo: Number(valores.costo),
        fecha: valores.fecha,
      }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }

  private cargarServicios(idProveedor: number | null): void {
    this.servicios.set([]);
    if (idProveedor === null) {
      return;
    }
    this.gestion.serviciosDelProveedor(idProveedor).subscribe({
      next: (servicios) => this.servicios.set(servicios),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }
}
