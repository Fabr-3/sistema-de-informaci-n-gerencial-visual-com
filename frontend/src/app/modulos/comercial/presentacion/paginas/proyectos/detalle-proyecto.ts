import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, catchError, filter, of, switchMap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { descargarArchivo } from '../../../../../compartido/utilidades/archivos';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { GestionProyectos } from '../../../aplicacion/gestion-proyectos';
import { etiquetaLugar } from '../../../dominio/modelos/catalogos';
import {
  Contrato,
  DetalleProyecto,
  ESTADOS_EVENTO,
  ESTADOS_PROYECTO,
  EstadoEvento,
  EstadoProyecto,
  Evento,
  Proyecto,
  TRANSICIONES_PROYECTO,
  problemaArchivoFirmado,
  proyectoVigente,
  tipoVisualEvento,
  tipoVisualProyecto,
} from '../../../dominio/modelos/proyecto';
import {
  DatosDialogoContrato,
  DatosDialogoEvento,
  DialogoContrato,
  DialogoEvento,
  DialogoProyecto,
} from './dialogos-proyecto';

/** Textos de las acciones de cambio de estado del proyecto. */
const ACCIONES_ESTADO: Readonly<Record<EstadoProyecto, string>> = {
  PLANIFICACION: 'Volver a planificación',
  EN_EJECUCION: 'Iniciar ejecución',
  FINALIZADO: 'Finalizar proyecto',
  CANCELADO: 'Cancelar proyecto',
};

/** Ficha del proyecto: datos, cotizacion de origen, cronograma de eventos y contratos. */
@Component({
  selector: 'app-detalle-proyecto',
  imports: [
    CurrencyPipe,
    DatePipe,
    EtiquetaPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './detalle-proyecto.html',
  styleUrl: './detalle-proyecto.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaDetalleProyecto {
  /** Parametro de ruta :id. */
  readonly id = input.required<string>();

  private readonly gestion = inject(GestionProyectos);
  private readonly comercial = inject(GestionComercial);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly estadosProyecto = ESTADOS_PROYECTO;
  protected readonly estadosEvento = ESTADOS_EVENTO;
  protected readonly accionesEstado = ACCIONES_ESTADO;
  protected readonly tipoVisualProyecto = tipoVisualProyecto;
  protected readonly tipoVisualEvento = tipoVisualEvento;
  protected readonly etiquetaLugar = etiquetaLugar;
  protected readonly columnasEventos = ['nombre', 'lugar', 'horario', 'estado', 'acciones'];
  protected readonly rutaListado = rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS);
  protected readonly detalle = signal<DetalleProyecto | null>(null);
  protected readonly noEncontrado = signal(false);
  private readonly version = signal(0);

  protected readonly puedeGestionar = computed(() => this.sesion.tienePermiso(Permisos.COMERCIAL_GESTIONAR));
  protected readonly vigente = computed(() => {
    const actual = this.detalle();
    return !!actual && proyectoVigente(actual.proyecto.estado);
  });
  protected readonly transiciones = computed<readonly EstadoProyecto[]>(() => {
    const actual = this.detalle();
    return actual ? TRANSICIONES_PROYECTO[actual.proyecto.estado] : [];
  });

  constructor() {
    toObservable(computed(() => ({ id: Number(this.id()), version: this.version() })))
      .pipe(
        switchMap(({ id }) =>
          this.gestion.obtener(id).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              this.noEncontrado.set(true);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((detalle) => this.detalle.set(detalle));
  }

  protected rutaCotizacion(idCotizacion: number): string {
    return rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES, String(idCotizacion));
  }

  protected editar(proyecto: Proyecto): void {
    this.alCerrar(this.dialogos.open(DialogoProyecto, { data: proyecto, width: '560px' }).afterClosed(),
      'Proyecto actualizado');
  }

  protected cambiarEstado(proyecto: Proyecto, estado: EstadoProyecto): void {
    this.confirmar({
      titulo: ACCIONES_ESTADO[estado],
      mensaje: `El proyecto «${proyecto.nombre}» pasará a ${ESTADOS_PROYECTO[estado].toLowerCase()}.`,
      textoConfirmar: 'Confirmar',
      destructiva: estado === 'CANCELADO',
    })
      .pipe(switchMap(() => this.gestion.cambiarEstado(proyecto.id, estado)))
      .subscribe({
        next: () => this.recargar(`Proyecto en estado ${ESTADOS_PROYECTO[estado].toLowerCase()}`),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected editarEvento(idProyecto: number, evento?: Evento): void {
    this.comercial
      .listarLugares()
      .pipe(
        switchMap((lugares) =>
          this.dialogos
            .open<DialogoEvento, DatosDialogoEvento, Evento>(DialogoEvento, {
              data: { idProyecto, evento, lugares },
              width: '560px',
            })
            .afterClosed(),
        ),
        filter(Boolean),
      )
      .subscribe({
        next: () => this.recargar(evento ? 'Evento actualizado' : 'Evento programado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected cambiarEstadoEvento(evento: Evento, estado: EstadoEvento): void {
    this.gestion.cambiarEstadoEvento(evento.idProyecto, evento.id, estado).subscribe({
      next: () => this.recargar(`Evento ${ESTADOS_EVENTO[estado].toLowerCase()}`),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  protected editarContrato(idProyecto: number, contrato?: Contrato): void {
    this.alCerrar(
      this.dialogos
        .open<DialogoContrato, DatosDialogoContrato, Contrato>(DialogoContrato, {
          data: { idProyecto, contrato },
          width: '560px',
        })
        .afterClosed(),
      contrato ? 'Contrato actualizado' : 'Contrato registrado',
    );
  }

  protected adjuntarFirmado(contrato: Contrato, selector: HTMLInputElement): void {
    const archivo = selector.files?.[0];
    selector.value = '';
    if (!archivo) {
      return;
    }
    const problema = problemaArchivoFirmado(archivo);
    if (problema) {
      this.notificaciones.advertencia(problema);
      return;
    }
    this.gestion.adjuntarFirmado(contrato.idProyecto, contrato.id, archivo).subscribe({
      next: () => this.recargar('Copia firmada adjunta'),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  protected descargarFirmado(contrato: Contrato): void {
    this.gestion.descargarFirmado(contrato.idProyecto, contrato.id).subscribe({
      next: (contenido) => descargarArchivo(contenido, `${contrato.numero}-firmado.pdf`),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  protected descargarContrato(contrato: Contrato): void {
    this.gestion.descargarContrato(contrato.idProyecto, contrato.id).subscribe({
      next: (contenido) => descargarArchivo(contenido, `${contrato.numero}.pdf`),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  private alCerrar(resultado: Observable<unknown>, mensaje: string): void {
    resultado.pipe(filter(Boolean)).subscribe(() => this.recargar(mensaje));
  }

  private confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, { data: datos, width: '440px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is boolean => confirmado === true));
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.version.update((valor) => valor + 1);
  }
}
