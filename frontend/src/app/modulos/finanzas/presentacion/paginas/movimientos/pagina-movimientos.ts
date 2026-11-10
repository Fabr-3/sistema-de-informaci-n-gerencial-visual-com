import { CurrencyPipe, DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { SolicitudPagina, TAMANOS_PAGINA, TAMANO_PAGINA_POR_DEFECTO } from '../../../../../compartido/modelos/pagina';
import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionMovimientos } from '../../../aplicacion/gestion-movimientos';
import {
  ESTADOS_MOVIMIENTO,
  FiltroMovimientos,
  ListadoMovimientos,
  Movimiento,
  NATURALEZAS,
  montoConSigno,
  tipoVisualNaturaleza,
} from '../../../dominio/modelos/movimiento';
import { DialogoDetalleMovimiento } from './dialogo-detalle-movimiento';

const RETARDO_BUSQUEDA_MS = 300;
const LONGITUD_MOTIVO = 1000;
const COLUMNAS = ['fecha', 'tipo', 'concepto', 'factura', 'monto', 'acciones'];

interface CriteriosMovimientos {
  readonly filtro: FiltroMovimientos;
  readonly solicitud: SolicitudPagina;
}

/** Ingresos y egresos con totales del periodo; por defecto, el mes en curso. */
@Component({
  selector: 'app-pagina-movimientos',
  imports: [
    CurrencyPipe,
    DatePipe,
    KeyValuePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './pagina-movimientos.html',
  styleUrl: './pagina-movimientos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaMovimientos {
  /** Parametro de consulta: muestra solo los movimientos de ese proyecto (todas las fechas). */
  readonly idProyecto = input<string>();

  private readonly gestion = inject(GestionMovimientos);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly naturalezas = NATURALEZAS;
  protected readonly estados = ESTADOS_MOVIMIENTO;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly montoConSigno = montoConSigno;
  protected readonly tipoVisualNaturaleza = tipoVisualNaturaleza;
  protected readonly rutaRegistro = rutaAbsoluta(Rutas.FINANZAS, Rutas.MOVIMIENTOS, Rutas.NUEVA);
  protected readonly puedeGestionar = inject(ServicioSesion).tienePermiso(Permisos.FINANZAS_GESTIONAR);
  protected readonly tipos = toSignal(this.gestion.tipos(), { initialValue: [] });
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(this.inicioDeMes()),
    hasta: new FormControl<Date | null>(new Date()),
  });
  protected readonly resultado = signal<ListadoMovimientos | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosMovimientos>({
    filtro: this.filtroDeRango(),
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  protected readonly tiposFiltrados = computed(() => {
    const naturaleza = this.criterios().filtro.naturaleza;
    return naturaleza ? this.tipos().filter((tipo) => tipo.naturaleza === naturaleza) : this.tipos();
  });

  constructor() {
    toObservable(this.idProyecto)
      .pipe(
        filter((id): id is string => !!id),
        takeUntilDestroyed(),
      )
      .subscribe((id) => {
        this.rango.setValue({ desde: null, hasta: null }, { emitEvent: false });
        this.filtrar({ idProyecto: Number(id), desde: undefined, hasta: undefined });
      });
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));
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

  protected filtrar(cambios: Partial<FiltroMovimientos>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  /** Al cambiar la naturaleza se descarta un tipo que ya no corresponde. */
  protected filtrarNaturaleza(naturaleza: FiltroMovimientos['naturaleza']): void {
    this.filtrar({ naturaleza, idTipo: undefined });
  }

  protected quitarFiltroProyecto(): void {
    this.filtrar({ idProyecto: undefined });
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected verDetalle(movimiento: Movimiento): void {
    this.dialogos.open(DialogoDetalleMovimiento, { data: movimiento, width: '520px' });
  }

  protected anular(movimiento: Movimiento): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: `Anular movimiento N.º ${movimiento.id}`,
          etiqueta: 'Motivo de la anulación',
          indicacion: movimiento.tipo.descuentaInventario
            ? 'Al anular la venta, los equipos vuelven al inventario.'
            : undefined,
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
        switchMap((motivo) => this.gestion.anular(movimiento.id, motivo)),
      )
      .subscribe({
        next: () => {
          this.notificaciones.exito('Movimiento anulado');
          this.criterios.update((actuales) => ({ ...actuales }));
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private filtroDeRango(): FiltroMovimientos {
    const { desde, hasta } = this.rango.getRawValue();
    return { desde: desde ? aFechaIso(desde) : undefined, hasta: hasta ? aFechaIso(hasta) : undefined };
  }

  private inicioDeMes(): Date {
    const hoy = new Date();
    return new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  }
}
