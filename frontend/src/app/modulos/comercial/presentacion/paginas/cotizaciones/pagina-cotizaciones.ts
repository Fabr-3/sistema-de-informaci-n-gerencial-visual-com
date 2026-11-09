import { CurrencyPipe, DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
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
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { GestionCotizaciones } from '../../../aplicacion/gestion-cotizaciones';
import {
  Cotizacion,
  ESTADOS_COTIZACION,
  FiltroCotizaciones,
  tipoVisualCotizacion,
} from '../../../dominio/modelos/cotizacion';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['numero', 'cliente', 'titulo', 'emision', 'validez', 'importe', 'estado'];

interface CriteriosCotizaciones {
  readonly filtro: FiltroCotizaciones;
  readonly solicitud: SolicitudPagina;
}

/** Cotizaciones emitidas, de la mas reciente a la mas antigua. */
@Component({
  selector: 'app-pagina-cotizaciones',
  imports: [
    CurrencyPipe,
    DatePipe,
    EtiquetaPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    SiPermiso,
  ],
  templateUrl: './pagina-cotizaciones.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCotizaciones {
  private readonly gestion = inject(GestionCotizaciones);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly permisos = Permisos;
  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_COTIZACION;
  protected readonly tipoVisual = tipoVisualCotizacion;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly rutaNueva = rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES, Rutas.NUEVA);
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly resultado = signal<Pagina<Cotizacion> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosCotizaciones>({
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

  protected filtrar(cambios: Partial<FiltroCotizaciones>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(cotizacion: Cotizacion): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES, String(cotizacion.id)));
  }
}
