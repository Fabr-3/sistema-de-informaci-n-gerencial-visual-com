import { DatePipe, LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ESTADOS_EVENTO, tipoVisualEvento } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionPlanificacion } from '../../../aplicacion/gestion-planificacion';
import {
  EstadoPlanificacion,
  FiltroEventosLogistica,
  ResumenLogisticoEvento,
  estadoPlanificacion,
} from '../../../dominio/modelos/planificacion';

const RETARDO_BUSQUEDA_MS = 300;
/** Por defecto se muestran los eventos de los proximos dos meses. */
const DIAS_POR_DEFECTO = 60;
const COLUMNAS = ['fecha', 'evento', 'lugar', 'jornadas', 'personal', 'estado'];

interface CriteriosPlanificacion {
  readonly filtro: FiltroEventosLogistica;
  readonly solicitud: SolicitudPagina;
}

/** Eventos con el avance de su planificacion logistica (montaje, desmontaje y personal). */
@Component({
  selector: 'app-pagina-planificacion',
  imports: [
    DatePipe,
    EtiquetaPipe,
    LowerCasePipe,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-planificacion.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaPlanificacion {
  private readonly gestion = inject(GestionPlanificacion);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estadosEvento = ESTADOS_EVENTO;
  protected readonly tipoVisualEvento = tipoVisualEvento;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(new Date()),
    hasta: new FormControl<Date | null>(this.diasDesdeHoy(DIAS_POR_DEFECTO)),
  });
  protected readonly resultado = signal<Pagina<ResumenLogisticoEvento> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosPlanificacion>({
    filtro: this.filtroDeRango(),
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    // Solo se consulta cuando el rango esta completo (o vacio)
    this.rango.valueChanges
      .pipe(
        filter(({ desde, hasta }) => !desde === !hasta),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.filtrar(this.filtroDeRango()));

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

  protected estado(resumen: ResumenLogisticoEvento): EstadoPlanificacion {
    return estadoPlanificacion(resumen);
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(resumen: ResumenLogisticoEvento): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.LOGISTICA, Rutas.PLANIFICACION, String(resumen.evento.id)));
  }

  private filtrar(cambios: Partial<FiltroEventosLogistica>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  private filtroDeRango(): FiltroEventosLogistica {
    const { desde, hasta } = this.rango.getRawValue();
    return { desde: desde ? aFechaIso(desde) : undefined, hasta: hasta ? aFechaIso(hasta) : undefined };
  }

  private diasDesdeHoy(dias: number): Date {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + dias);
    return fecha;
  }
}
