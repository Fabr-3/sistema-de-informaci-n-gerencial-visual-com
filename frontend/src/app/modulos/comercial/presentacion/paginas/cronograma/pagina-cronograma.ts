import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import {
  CalendarDatePipe,
  CalendarDayViewComponent,
  CalendarEvent,
  CalendarMonthViewComponent,
  CalendarNextViewDirective,
  CalendarPreviousViewDirective,
  CalendarTodayDirective,
  CalendarView,
  CalendarWeekViewComponent,
} from 'angular-calendar';
import { addDays, endOfMonth, endOfWeek, startOfMonth, startOfWeek } from 'date-fns';
import { catchError, of, switchMap, tap } from 'rxjs';

import { aIsoLocal } from '../../../../../compartido/utilidades/fechas';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionProyectos } from '../../../aplicacion/gestion-proyectos';
import { ESTADOS_EVENTO, EstadoEvento, EventoCalendario } from '../../../dominio/modelos/proyecto';

/** Configuracion regional del calendario: semana de lunes a domingo, en espanol. */
const LOCALE = 'es-BO';
const LUNES = 1;
const HORA_INICIO_DIA = 6;

/** Colores por estado, alineados con la paleta corporativa. */
const COLORES: Readonly<Record<EstadoEvento, { primary: string; secondary: string }>> = {
  PROGRAMADO: { primary: '#b71c1c', secondary: '#fdeeee' },
  EJECUTADO: { primary: '#2e7d32', secondary: '#e8f5e9' },
  CANCELADO: { primary: '#9e9e9e', secondary: '#eeeeee' },
};

/**
 * Cronograma general de eventos de todos los proyectos (mes, semana o dia).
 * Al hacer clic en un evento se abre su proyecto.
 */
@Component({
  selector: 'app-pagina-cronograma',
  imports: [
    CalendarDatePipe,
    CalendarDayViewComponent,
    CalendarMonthViewComponent,
    CalendarNextViewDirective,
    CalendarPreviousViewDirective,
    CalendarTodayDirective,
    CalendarWeekViewComponent,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './pagina-cronograma.html',
  styleUrl: './pagina-cronograma.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCronograma {
  private readonly gestion = inject(GestionProyectos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly Vista = CalendarView;
  protected readonly locale = LOCALE;
  protected readonly inicioSemana = LUNES;
  protected readonly horaInicio = HORA_INICIO_DIA;
  protected readonly leyenda = (Object.keys(ESTADOS_EVENTO) as EstadoEvento[]).map((estado) => ({
    etiqueta: ESTADOS_EVENTO[estado],
    color: COLORES[estado].primary,
  }));
  protected readonly vista = signal<CalendarView>(CalendarView.Month);
  protected readonly fecha = signal(new Date());
  protected readonly diaAbierto = signal(false);
  protected readonly eventos = signal<CalendarEvent<EventoCalendario>[]>([]);
  protected readonly cargando = signal(false);

  /** Rango visible (con margen de una semana para los dias de meses vecinos). */
  private readonly rango = computed(() => {
    const fecha = this.fecha();
    const opciones = { weekStartsOn: LUNES } as const;
    return {
      desde: startOfWeek(startOfMonth(fecha), opciones),
      hasta: addDays(endOfWeek(endOfMonth(fecha), opciones), 1),
    };
  });

  constructor() {
    toObservable(this.rango)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ desde, hasta }) =>
          this.gestion.cronograma(aIsoLocal(desde), aIsoLocal(hasta)).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((eventos) => {
        this.eventos.set(eventos.map(aEventoCalendario));
        this.cargando.set(false);
      });
  }

  protected cambiarFecha(fecha: Date): void {
    this.diaAbierto.set(false);
    this.fecha.set(fecha);
  }

  /** En la vista mensual, un clic abre la lista de eventos del dia (o la cierra). */
  protected alSeleccionarDia({ date, events }: { date: Date; events: CalendarEvent[] }): void {
    const mismoDia = date.toDateString() === this.fecha().toDateString();
    this.diaAbierto.set(events.length > 0 && !(mismoDia && this.diaAbierto()));
    this.fecha.set(date);
  }

  protected abrirProyecto({ event }: { event: CalendarEvent<EventoCalendario> }): void {
    if (event.meta) {
      void this.router.navigateByUrl(rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS, String(event.meta.idProyecto)));
    }
  }
}

function aEventoCalendario(evento: EventoCalendario): CalendarEvent<EventoCalendario> {
  const lugar = evento.ciudad ? `${evento.lugar}, ${evento.ciudad}` : evento.lugar;
  return {
    id: evento.id,
    start: new Date(evento.fechaInicio),
    end: new Date(evento.fechaFin),
    title: `${evento.nombre} · ${evento.cliente} (${lugar})`,
    color: COLORES[evento.estado],
    cssClass: evento.estado === 'CANCELADO' ? 'evento-cancelado' : undefined,
    meta: evento,
  };
}
