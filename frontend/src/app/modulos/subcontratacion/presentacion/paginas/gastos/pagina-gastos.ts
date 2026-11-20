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
import { GestionGastos } from '../../../aplicacion/gestion-subcontratacion';
import { ESTADOS_CONSUMO } from '../../../dominio/modelos/consumo';
import { FiltroGastos, GastoEvento, montoRegistrado } from '../../../dominio/modelos/gasto';
import { DatosDialogoGasto, DialogoGasto } from './dialogo-gasto';

const RETARDO_FILTRO_MS = 300;
const LONGITUD_MOTIVO = 500;
const COLUMNAS = ['fecha', 'descripcion', 'destino', 'monto', 'comprobante', 'registradoPor', 'estado', 'acciones'];

interface Criterios {
  readonly filtro: FiltroGastos;
  readonly solicitud: SolicitudPagina;
}

/** Gastos del evento con comprobante; el tecnico ve solo los de sus eventos. */
@Component({
  selector: 'app-pagina-gastos',
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
  templateUrl: './pagina-gastos.html',
  styleUrl: '../subcontratacion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaGastos {
  private readonly gestion = inject(GestionGastos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);
  private readonly sesion = inject(ServicioSesion);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_CONSUMO;
  protected readonly montoRegistrado = montoRegistrado;
  protected readonly puedeRegistrar = this.sesion.tienePermiso(Permisos.CONSUMIBLES_REGISTRAR);
  protected readonly limitado = !this.sesion.tieneAlgunPermiso(Permisos.PROVEEDORES_VER, Permisos.PROVEEDORES_GESTIONAR);
  protected readonly periodo = inject(NonNullableFormBuilder).group({ desde: [''], hasta: [''] });
  protected readonly resultado = signal<Pagina<GastoEvento> | null>(null);
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
      .open<DialogoGasto, DatosDialogoGasto, GastoEvento>(DialogoGasto, {
        data: { limitadoAEventosAsignados: this.limitado },
        width: '600px',
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.recargar('Gasto registrado como egreso del evento'));
  }

  /** Abre el comprobante (foto o PDF) en otra pestana. */
  protected verComprobante(gasto: GastoEvento): void {
    const ventana = window.open('', '_blank');
    this.gestion.comprobante(gasto.id).subscribe({
      next: (contenido) => {
        const url = URL.createObjectURL(contenido);
        if (ventana) {
          ventana.location.href = url;
        } else {
          window.open(url, '_blank');
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (error: unknown) => {
        ventana?.close();
        this.notificaciones.error(error);
      },
    });
  }

  protected anular(gasto: GastoEvento): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Anular gasto',
          etiqueta: 'Motivo',
          indicacion: `Se anulará también su egreso en Finanzas (${gasto.descripcion}).`,
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
        switchMap((motivo) => this.gestion.anular(gasto.id, motivo)),
      )
      .subscribe({
        next: () => this.recargar('Gasto y egreso anulados'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.criterios.update((actuales) => ({ ...actuales }));
  }
}
