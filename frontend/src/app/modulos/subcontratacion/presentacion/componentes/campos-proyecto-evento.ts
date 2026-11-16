import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { map } from 'rxjs';

import { busquedaRemota } from '../../../../compartido/utilidades/busqueda-remota';
import { ServicioNotificaciones } from '../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionProyectos } from '../../../comercial/aplicacion/gestion-proyectos';
import { Evento, Proyecto } from '../../../comercial/dominio/modelos/proyecto';

const SUGERENCIAS = 10;
const PROYECTOS_VIGENTES = new Set(['PLANIFICACION', 'EN_EJECUCION']);

/**
 * Proyecto vigente (buscado por nombre o cliente) y, opcionalmente, uno de
 * sus eventos no cancelados.
 */
@Component({
  selector: 'app-campos-proyecto-evento',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <mat-form-field appearance="outline" class="completo">
      <mat-label>Proyecto</mat-label>
      <input matInput [formControl]="proyecto()" [matAutocomplete]="autocompletarProyecto" placeholder="Buscar proyecto o cliente" />
      <mat-autocomplete #autocompletarProyecto="matAutocomplete" [displayWith]="mostrarProyecto">
        @for (opcion of sugeridos(); track opcion.id) {
          <mat-option [value]="opcion">{{ opcion.nombre }} <span class="texto-secundario">· {{ opcion.cliente.razonSocial }}</span></mat-option>
        }
      </mat-autocomplete>
      <mat-error>Elija un proyecto vigente de la lista</mat-error>
    </mat-form-field>
    <mat-form-field appearance="outline" class="completo">
      <mat-label>Evento</mat-label>
      <mat-select [formControl]="evento()">
        <mat-option [value]="null">Todo el proyecto</mat-option>
        @for (opcion of eventos(); track opcion.id) {
          <mat-option [value]="opcion.id">{{ opcion.nombre }}</mat-option>
        }
      </mat-select>
    </mat-form-field>
  `,
  styles: `
    :host {
      display: contents;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CamposProyectoEvento implements OnInit {
  readonly proyecto = input.required<FormControl<Proyecto | string | null>>();
  readonly evento = input.required<FormControl<number | null>>();

  private readonly proyectos = inject(GestionProyectos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly destruccion = inject(DestroyRef);

  protected readonly eventos = signal<Evento[]>([]);
  protected readonly sugeridos = signal<Proyecto[]>([]);
  protected readonly mostrarProyecto = (valor: Proyecto | string | null): string =>
    typeof valor === 'string' ? valor : valor ? `${valor.nombre} · ${valor.cliente.razonSocial}` : '';

  ngOnInit(): void {
    busquedaRemota(this.proyecto(), (texto) =>
      this.proyectos
        .listar({ texto }, { pagina: 0, tamano: SUGERENCIAS })
        .pipe(map((pagina) => pagina.contenido.filter((opcion) => PROYECTOS_VIGENTES.has(opcion.estado)))),
    )
      .pipe(takeUntilDestroyed(this.destruccion))
      .subscribe((proyectos) => this.sugeridos.set(proyectos));
    this.evento().disable();
    this.proyecto()
      .valueChanges.pipe(takeUntilDestroyed(this.destruccion))
      .subscribe((valor) => this.cargarEventos(typeof valor === 'object' && valor ? valor.id : null));
  }

  private cargarEventos(idProyecto: number | null): void {
    const control = this.evento();
    control.setValue(null);
    control.disable();
    this.eventos.set([]);
    if (idProyecto === null) {
      return;
    }
    this.proyectos.obtener(idProyecto).subscribe({
      next: (detalle) => {
        this.eventos.set(detalle.eventos.filter((opcion) => opcion.estado !== 'CANCELADO'));
        if (this.eventos().length) {
          control.enable();
        }
      },
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }
}
