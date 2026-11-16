import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, filter, of, switchMap, tap } from 'rxjs';

import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionSubcontrataciones } from '../../../aplicacion/gestion-subcontratacion';
import {
  ESTADOS_SUBCONTRATACION,
  FiltroSubcontrataciones,
  Subcontratacion,
  tipoVisualSubcontratacion,
  totalVigente,
} from '../../../dominio/modelos/subcontratacion';
import { DialogoSubcontratacion } from './dialogo-subcontratacion';

const RETARDO_FILTRO_MS = 300;
const LONGITUD_MOTIVO = 500;
const COLUMNAS = ['fecha', 'proveedor', 'proyecto', 'descripcion', 'costo', 'estado', 'acciones'];

interface Criterios {
  readonly filtro: FiltroSubcontrataciones;
  readonly solicitud: SolicitudPagina;
}

/** Servicios subcontratados a proveedores para los proyectos, con su egreso financiero. */
@Component({
  selector: 'app-pagina-subcontrataciones',
  imports: [
    CurrencyPipe,
    DatePipe,
    EtiquetaPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-subcontrataciones.html',
  styleUrl: '../subcontratacion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaSubcontrataciones {
  private readonly gestion = inject(GestionSubcontrataciones);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_SUBCONTRATACION;
  protected readonly tipoVisual = tipoVisualSubcontratacion;
  protected readonly totalVigente = totalVigente;
  protected readonly puedeGestionar = inject(ServicioSesion).tienePermiso(Permisos.PROVEEDORES_GESTIONAR);
  protected readonly periodo = inject(NonNullableFormBuilder).group({ desde: [''], hasta: [''] });
  protected readonly resultado = signal<Pagina<Subcontratacion> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<Criterios>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.periodo.valueChanges.pipe(debounceTime(RETARDO_FILTRO_MS), takeUntilDestroyed()).subscribe(() => {
      const { desde, hasta } = this.periodo.getRawValue();
      this.criterios.update(({ solicitud }) => ({
        filtro: { desde: desde || undefined, hasta: hasta || undefined },
        solicitud: { ...solicitud, pagina: 0 },
      }));
    });
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

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected registrar(): void {
    this.dialogos
      .open<DialogoSubcontratacion, void, Subcontratacion>(DialogoSubcontratacion, { width: '640px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.recargar('Subcontratación registrada y su egreso imputado al proyecto'));
  }

  protected anular(subcontratacion: Subcontratacion): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Anular subcontratación',
          etiqueta: 'Motivo',
          indicacion: `También se anulará su egreso en Finanzas (${subcontratacion.proveedor.nombre}).`,
          textoConfirmar: 'Anular',
          longitudMaxima: LONGITUD_MOTIVO,
          multilinea: true,
          destructiva: true,
        },
        width: '480px',
      })
      .afterClosed()
      .pipe(
        filter((motivo): motivo is string => !!motivo),
        switchMap((motivo) => this.gestion.anular(subcontratacion.id, motivo)),
      )
      .subscribe({
        next: () => this.recargar('Subcontratación y egreso anulados'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.criterios.update((actuales) => ({ ...actuales }));
  }
}
