import { DatePipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, catchError, filter, of, switchMap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { ESTADOS_EVENTO, tipoVisualEvento } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionPersonal } from '../../../aplicacion/gestion-personal';
import { GestionPlanificacion } from '../../../aplicacion/gestion-planificacion';
import { GestionReservas } from '../../../aplicacion/gestion-reservas';
import { Asignacion, Cruce, Jornada, PlanEvento, TIPOS_JORNADA } from '../../../dominio/modelos/planificacion';
import { Reserva } from '../../../dominio/modelos/reservas';
import { DatosDialogoAsignacion, DatosDialogoJornada, DialogoAsignacion, DialogoJornada } from './dialogos-plan';
import { DatosDialogoReserva, DialogoReservarEquipos } from './dialogo-reservar-equipos';

const LONGITUD_TRANSPORTE = 2000;
const LONGITUD_MOTIVO_LIBERACION = 255;
const COLUMNAS_JORNADAS = ['tipo', 'horario', 'descripcion', 'acciones'];
const COLUMNAS_ASIGNACIONES = ['persona', 'rol', 'horario', 'cruces', 'acciones'];
const COLUMNAS_RESERVAS = ['codigo', 'modelo', 'categoria', 'salida', 'retorno', 'acciones'];

/** Plan logistico de un evento: transporte, jornadas de montaje y desmontaje, personal y equipos reservados. */
@Component({
  selector: 'app-pagina-plan-evento',
  imports: [
    DatePipe,
    EtiquetaPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-plan-evento.html',
  styleUrl: './pagina-plan-evento.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaPlanEvento {
  /** Parametro de ruta :idEvento. */
  readonly idEvento = input.required<string>();

  private readonly gestion = inject(GestionPlanificacion);
  private readonly reservasEvento = inject(GestionReservas);
  private readonly personal = inject(GestionPersonal);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly locale = inject(LOCALE_ID);

  protected readonly rutaListado = rutaAbsoluta(Rutas.LOGISTICA, Rutas.PLANIFICACION);
  protected readonly tiposJornada = TIPOS_JORNADA;
  protected readonly estadosEvento = ESTADOS_EVENTO;
  protected readonly tipoVisualEvento = tipoVisualEvento;
  protected readonly columnasJornadas = COLUMNAS_JORNADAS;
  protected readonly columnasAsignaciones = COLUMNAS_ASIGNACIONES;
  protected readonly columnasReservas = COLUMNAS_RESERVAS;
  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly plan = signal<PlanEvento | null>(null);
  protected readonly cargando = signal(true);
  protected readonly puedeGestionar = computed(
    () => this.sesion.tienePermiso(Permisos.LOGISTICA_GESTIONAR) && !!this.plan()?.editable,
  );

  constructor() {
    toObservable(this.idEvento)
      .pipe(
        switchMap((id) =>
          this.gestion.obtener(Number(id)).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((plan) => {
        this.plan.set(plan);
        this.cargando.set(false);
        if (plan) {
          this.cargarReservas(plan.evento.id);
        }
      });
  }

  protected textoCruces(cruces: readonly Cruce[]): string {
    const detalle = cruces.map(
      (cruce) =>
        `${cruce.evento} (${formatDate(cruce.inicio, 'dd/MM HH:mm', this.locale)}–` +
        `${formatDate(cruce.fin, 'HH:mm', this.locale)})`,
    );
    return `También asignado a: ${detalle.join(', ')}`;
  }

  protected editarTransporte(plan: PlanEvento): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Transporte y logística',
          etiqueta: 'Vehículos, horarios de salida, bodega, accesos…',
          textoConfirmar: 'Guardar',
          valorInicial: plan.detallesTransporte ?? '',
          longitudMaxima: LONGITUD_TRANSPORTE,
          multilinea: true,
        },
        width: '560px',
      })
      .afterClosed()
      .pipe(
        filter((texto): texto is string => !!texto),
        switchMap((texto) => this.gestion.guardarTransporte(plan.evento.id, texto)),
      )
      .subscribe({
        next: (actualizado) => this.mostrar(actualizado, 'Transporte actualizado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected editarJornada(plan: PlanEvento, jornada?: Jornada): void {
    this.dialogos
      .open<DialogoJornada, DatosDialogoJornada, PlanEvento>(DialogoJornada, {
        data: { plan, jornada },
        width: '560px',
      })
      .afterClosed()
      .pipe(filter((actualizado): actualizado is PlanEvento => !!actualizado))
      .subscribe((actualizado) => this.mostrar(actualizado, jornada ? 'Jornada actualizada' : 'Jornada registrada'));
  }

  protected quitarJornada(plan: PlanEvento, jornada: Jornada): void {
    this.confirmar({
      titulo: 'Quitar jornada',
      mensaje: `Se quitará la jornada de ${TIPOS_JORNADA[jornada.tipo].toLowerCase()} del ` +
        `${formatDate(jornada.inicio, 'dd/MM/yyyy HH:mm', this.locale)}.`,
      textoConfirmar: 'Quitar',
      destructiva: true,
    })
      .pipe(switchMap(() => this.gestion.quitarJornada(plan.evento.id, jornada.id)))
      .subscribe({
        next: (actualizado) => this.mostrar(actualizado, 'Jornada quitada'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected editarAsignacion(plan: PlanEvento, asignacion?: Asignacion): void {
    this.personal
      .catalogos()
      .pipe(
        switchMap((catalogos) =>
          this.dialogos
            .open<DialogoAsignacion, DatosDialogoAsignacion, Asignacion>(DialogoAsignacion, {
              data: { plan, roles: catalogos.rolesEvento, asignacion },
              width: '600px',
            })
            .afterClosed(),
        ),
        filter(Boolean),
      )
      .subscribe({
        next: (guardada) => this.recargar(plan, this.mensajeAsignacion(guardada, !!asignacion)),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected quitarAsignacion(plan: PlanEvento, asignacion: Asignacion): void {
    this.confirmar({
      titulo: 'Quitar asignación',
      mensaje: `${asignacion.personal.nombreCompleto} dejará de estar asignado como ${asignacion.rol.nombre}.`,
      textoConfirmar: 'Quitar',
      destructiva: true,
    })
      .pipe(switchMap(() => this.gestion.quitarAsignacion(plan.evento.id, asignacion.id)))
      .subscribe({
        next: () => this.recargar(plan, 'Asignación quitada'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected reservarEquipos(plan: PlanEvento): void {
    this.dialogos
      .open<DialogoReservarEquipos, DatosDialogoReserva, Reserva[]>(DialogoReservarEquipos, {
        data: { plan },
        width: '640px',
      })
      .afterClosed()
      .pipe(filter((reservadas): reservadas is Reserva[] => !!reservadas))
      .subscribe((reservadas) => {
        this.cargarReservas(plan.evento.id);
        const mensaje = reservadas.length === 1 ? 'Equipo reservado' : `${reservadas.length} equipos reservados`;
        this.notificaciones.exito(mensaje);
      });
  }

  protected liberarReserva(plan: PlanEvento, reserva: Reserva): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Liberar equipo',
          etiqueta: 'Motivo',
          indicacion: `${reserva.codigo} (${reserva.modelo}) quedará disponible para otros eventos.`,
          textoConfirmar: 'Liberar',
          longitudMaxima: LONGITUD_MOTIVO_LIBERACION,
          multilinea: true,
          destructiva: true,
        },
        width: '480px',
      })
      .afterClosed()
      .pipe(
        filter((motivo): motivo is string => !!motivo),
        switchMap((motivo) => this.reservasEvento.liberar(plan.evento.id, reserva.id, motivo)),
      )
      .subscribe({
        next: () => {
          this.cargarReservas(plan.evento.id);
          this.notificaciones.exito('Equipo liberado');
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private cargarReservas(idEvento: number): void {
    this.reservasEvento.listar(idEvento).subscribe({
      next: (reservas) => this.reservas.set(reservas),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  private mensajeAsignacion(asignacion: Asignacion, edicion: boolean): string {
    const base = edicion ? 'Asignación actualizada' : 'Personal asignado';
    return asignacion.cruces.length ? `${base}. Atención: se cruza con otro evento` : base;
  }

  private recargar(plan: PlanEvento, mensaje: string): void {
    this.gestion.obtener(plan.evento.id).subscribe({
      next: (actualizado) => this.mostrar(actualizado, mensaje),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  /** Las jornadas definen el periodo de las reservas, por eso se vuelven a leer. */
  private mostrar(plan: PlanEvento, mensaje: string): void {
    this.plan.set(plan);
    this.cargarReservas(plan.evento.id);
    this.notificaciones.exito(mensaje);
  }

  private confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, { data: datos, width: '440px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is boolean => confirmado === true));
  }
}
