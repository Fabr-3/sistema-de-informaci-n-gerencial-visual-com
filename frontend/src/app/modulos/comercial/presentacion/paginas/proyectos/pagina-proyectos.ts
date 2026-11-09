import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionProyectos } from '../../../aplicacion/gestion-proyectos';
import {
  ESTADOS_PROYECTO,
  FiltroProyectos,
  Proyecto,
  tipoVisualProyecto,
} from '../../../dominio/modelos/proyecto';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['nombre', 'cliente', 'cotizacion', 'periodo', 'estado'];

interface CriteriosProyectos {
  readonly filtro: FiltroProyectos;
  readonly solicitud: SolicitudPagina;
}

/** Proyectos generados por cotizaciones aceptadas. */
@Component({
  selector: 'app-pagina-proyectos',
  imports: [
    DatePipe,
    EtiquetaPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './pagina-proyectos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaProyectos {
  private readonly gestion = inject(GestionProyectos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_PROYECTO;
  protected readonly tipoVisual = tipoVisualProyecto;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly resultado = signal<Pagina<Proyecto> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosProyectos>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(this.criterios)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ filtro, solicitud }) =>
          this.gestion.listar(filtro, solicitud).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
      });
  }

  protected filtrar(cambios: Partial<FiltroProyectos>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(proyecto: Proyecto): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS, String(proyecto.id)));
  }
}
