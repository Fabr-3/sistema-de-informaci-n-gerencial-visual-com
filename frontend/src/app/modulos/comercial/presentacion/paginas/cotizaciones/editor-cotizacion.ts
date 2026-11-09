import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { descargarArchivo } from '../../../../../compartido/utilidades/archivos';
import { aFechaIso, desdeFechaIso } from '../../../../../compartido/utilidades/fechas';
import { requiereSeleccion } from '../../../../../compartido/utilidades/validadores-seleccion';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { GestionCotizaciones } from '../../../aplicacion/gestion-cotizaciones';
import { Servicio } from '../../../dominio/modelos/catalogos';
import { ResumenCliente } from '../../../dominio/modelos/cliente';
import {
  Cotizacion,
  DatosLinea,
  ESTADOS_COTIZACION,
  LineaCotizacion,
  subtotalLinea,
  tipoVisualCotizacion,
  totalCotizacion,
} from '../../../dominio/modelos/cotizacion';

/** Sugerencias del buscador de clientes y espera tras cada tecla. */
const CLIENTES_SUGERIDOS = 10;
const ESPERA_BUSQUEDA_MS = 300;
const DIAS_VALIDEZ_POR_DEFECTO = 15;
const LONGITUD_TITULO = 200;
const LONGITUD_CONDICIONES = 4000;
const LONGITUD_MOTIVO = 1000;
const MAXIMO_LINEAS = 50;
const CANTIDAD_MAXIMA = 100_000;

/**
 * Registro y revision de una cotizacion. En borrador se edita; la gerencia
 * la acepta (generando el proyecto) o la rechaza con un motivo.
 */
@Component({
  selector: 'app-editor-cotizacion',
  imports: [
    CurrencyPipe,
    DatePipe,
    EtiquetaPipe,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './editor-cotizacion.html',
  styleUrl: './editor-cotizacion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorCotizacion {
  /** Parametro de ruta :id; ausente en "nueva". */
  readonly id = input<string>();

  private readonly gestion = inject(GestionCotizaciones);
  private readonly comercial = inject(GestionComercial);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);
  private readonly formularios = inject(NonNullableFormBuilder);

  protected readonly estados = ESTADOS_COTIZACION;
  protected readonly tipoVisual = tipoVisualCotizacion;
  protected readonly maximoLineas = MAXIMO_LINEAS;
  protected readonly rutaListado = rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES);
  protected readonly cotizacion = signal<Cotizacion | null>(null);
  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly cargando = signal(true);
  protected readonly procesando = signal(false);

  protected readonly formulario = this.formularios.group({
    cliente: [null as ResumenCliente | string | null, requiereSeleccion],
    titulo: ['', [Validators.required, Validators.maxLength(LONGITUD_TITULO)]],
    fechaEmision: [new Date() as Date | null, Validators.required],
    fechaValidez: [this.diasDesdeHoy(DIAS_VALIDEZ_POR_DEFECTO) as Date | null, Validators.required],
    fechaEventoInicio: [null as Date | null],
    fechaEventoFin: [null as Date | null],
    condiciones: ['', Validators.maxLength(LONGITUD_CONDICIONES)],
    lineas: this.formularios.array([this.grupoLinea()], Validators.minLength(1)),
  });

  /** Busca en el servidor mientras se escribe; un cliente elegido no dispara busquedas. */
  protected readonly clientesSugeridos = toSignal(
    this.formulario.controls.cliente.valueChanges.pipe(
      startWith(''),
      filter((valor): valor is string => typeof valor === 'string'),
      debounceTime(ESPERA_BUSQUEDA_MS),
      distinctUntilChanged(),
      switchMap((texto) =>
        this.comercial.listarClientes({ texto }, { pagina: 0, tamano: CLIENTES_SUGERIDOS }).pipe(
          map((pagina): ResumenCliente[] => [...pagina.contenido]),
          catchError(() => of<ResumenCliente[]>([])),
        ),
      ),
    ),
    { initialValue: [] },
  );

  protected readonly mostrarCliente = (cliente: ResumenCliente | string | null): string =>
    typeof cliente === 'string' ? cliente : (cliente?.razonSocial ?? '');

  private readonly lineasActuales = toSignal(this.formulario.controls.lineas.valueChanges, {
    initialValue: this.formulario.controls.lineas.getRawValue(),
  });
  protected readonly total = computed(() => totalCotizacion(this.lineasValidas(this.lineasActuales())));

  protected readonly puedeEditar = computed(() => {
    const actual = this.cotizacion();
    return this.sesion.tienePermiso(Permisos.COMERCIAL_GESTIONAR) && (!actual || actual.estado === 'BORRADOR');
  });
  protected readonly puedeResolver = computed(
    () => this.sesion.tienePermiso(Permisos.COTIZACIONES_APROBAR) && this.cotizacion()?.estado === 'BORRADOR',
  );

  constructor() {
    this.comercial
      .listarServicios()
      .pipe(
        switchMap((servicios) => {
          this.servicios.set(servicios);
          const id = Number(this.id());
          return id ? this.gestion.obtener(id) : of(null);
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (cotizacion) => {
          if (cotizacion) {
            this.cargar(cotizacion);
          }
          this.cargando.set(false);
        },
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.cargando.set(false);
        },
      });
  }

  protected get lineas() {
    return this.formulario.controls.lineas;
  }

  /** Servicios activos, mas los inactivos que la cotizacion ya incluia. */
  protected readonly serviciosDisponibles = computed(() => {
    const incluidos = new Set(this.cotizacion()?.lineas.map((linea) => linea.idServicio) ?? []);
    return this.servicios().filter((servicio) => servicio.activo || incluidos.has(servicio.id));
  });

  protected subtotal(indice: number): number {
    const linea = this.lineasActuales()[indice];
    return linea ? subtotalLinea(this.normalizar(linea)) : 0;
  }

  protected unidad(idServicio: number): string {
    return this.servicios().find((servicio) => servicio.id === idServicio)?.unidadMedida ?? '';
  }

  /** Al elegir un servicio se propone su precio referencial. */
  protected alElegirServicio(indice: number, idServicio: number): void {
    const servicio = this.servicios().find((candidato) => candidato.id === idServicio);
    if (servicio) {
      this.lineas.at(indice).controls.precioUnitario.setValue(servicio.precioReferencial);
    }
  }

  protected agregarLinea(): void {
    this.lineas.push(this.grupoLinea());
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.procesando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.ejecutar(
      this.gestion.guardar(this.cotizacion()?.id ?? null, {
        idCliente: (valores.cliente as ResumenCliente).id,
        titulo: valores.titulo,
        fechaEmision: aFechaIso(valores.fechaEmision!),
        fechaValidez: aFechaIso(valores.fechaValidez!),
        fechaEventoInicio: valores.fechaEventoInicio ? aFechaIso(valores.fechaEventoInicio) : null,
        fechaEventoFin: valores.fechaEventoFin ? aFechaIso(valores.fechaEventoFin) : null,
        condiciones: valores.condiciones,
        lineas: valores.lineas.map((linea) => this.normalizar(linea)),
      }),
      'Cotización guardada',
    );
  }

  protected aceptar(cotizacion: Cotizacion): void {
    this.confirmar({
      titulo: 'Aceptar cotización',
      mensaje: `Se aceptará ${cotizacion.numero} por ${cotizacion.cliente.razonSocial} y se generará el proyecto.`,
      textoConfirmar: 'Aceptar',
    })
      .pipe(switchMap(() => this.gestion.aceptar(cotizacion.id)))
      .subscribe({
        next: (aceptada) => {
          this.cargar(aceptada);
          this.notificaciones.exito('Cotización aceptada: proyecto generado');
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected rechazar(cotizacion: Cotizacion): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: `Rechazar ${cotizacion.numero}`,
          etiqueta: 'Motivo del rechazo',
          textoConfirmar: 'Rechazar',
          longitudMaxima: LONGITUD_MOTIVO,
          multilinea: true,
          destructiva: true,
        },
        width: '480px',
      })
      .afterClosed()
      .pipe(
        filter((motivo): motivo is string => !!motivo),
        switchMap((motivo) => this.gestion.rechazar(cotizacion.id, motivo)),
      )
      .subscribe({
        next: (rechazada) => {
          this.cargar(rechazada);
          this.notificaciones.exito('Cotización rechazada');
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected eliminar(cotizacion: Cotizacion): void {
    this.confirmar({
      titulo: 'Eliminar borrador',
      mensaje: `Se eliminará el borrador ${cotizacion.numero}.`,
      textoConfirmar: 'Eliminar',
      destructiva: true,
    })
      .pipe(switchMap(() => this.gestion.eliminar(cotizacion.id)))
      .subscribe({
        next: () => {
          this.notificaciones.exito('Borrador eliminado');
          void this.router.navigateByUrl(this.rutaListado);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected descargarPdf(cotizacion: Cotizacion): void {
    this.gestion.descargarPdf(cotizacion.id).subscribe({
      next: (contenido) => descargarArchivo(contenido, `${cotizacion.numero}.pdf`),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  protected rutaProyecto(idProyecto: number): string {
    return rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS, String(idProyecto));
  }

  private ejecutar(operacion: Observable<Cotizacion>, mensaje: string): void {
    const esNueva = !this.cotizacion();
    this.procesando.set(true);
    operacion.subscribe({
      next: (guardada) => {
        this.procesando.set(false);
        this.notificaciones.exito(mensaje);
        if (esNueva) {
          void this.router.navigateByUrl(rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES, String(guardada.id)));
        } else {
          this.cargar(guardada);
        }
      },
      error: (error: unknown) => {
        this.procesando.set(false);
        this.notificaciones.error(error);
      },
    });
  }

  /** Carga la cotizacion en el formulario; fuera de borrador queda en solo lectura. */
  private cargar(cotizacion: Cotizacion): void {
    this.cotizacion.set(cotizacion);
    this.lineas.clear();
    cotizacion.lineas.forEach((linea) => this.lineas.push(this.grupoLinea(linea)));
    this.formulario.patchValue({
      cliente: cotizacion.cliente,
      titulo: cotizacion.titulo,
      fechaEmision: desdeFechaIso(cotizacion.fechaEmision),
      fechaValidez: desdeFechaIso(cotizacion.fechaValidez),
      fechaEventoInicio: cotizacion.fechaEventoInicio ? desdeFechaIso(cotizacion.fechaEventoInicio) : null,
      fechaEventoFin: cotizacion.fechaEventoFin ? desdeFechaIso(cotizacion.fechaEventoFin) : null,
      condiciones: cotizacion.condiciones ?? '',
    });
    if (this.puedeEditar()) {
      this.formulario.enable();
    } else {
      this.formulario.disable();
    }
  }

  private confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, { data: datos, width: '460px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is boolean => confirmado === true));
  }

  private grupoLinea(linea?: LineaCotizacion) {
    return this.formularios.group({
      idServicio: [linea?.idServicio ?? 0, Validators.min(1)],
      cantidad: [linea?.cantidad ?? 1, [Validators.required, Validators.min(1), Validators.max(CANTIDAD_MAXIMA)]],
      precioUnitario: [linea?.precioUnitario ?? 0, [Validators.required, Validators.min(0)]],
      descuento: [linea?.descuento ?? 0, Validators.min(0)],
    });
  }

  private normalizar(linea: Partial<DatosLinea>): DatosLinea {
    return {
      idServicio: Number(linea.idServicio) || 0,
      cantidad: Number(linea.cantidad) || 0,
      precioUnitario: Number(linea.precioUnitario) || 0,
      descuento: Number(linea.descuento) || 0,
    };
  }

  private lineasValidas(lineas: Partial<DatosLinea>[]): DatosLinea[] {
    return lineas.map((linea) => this.normalizar(linea)).filter((linea) => linea.idServicio > 0);
  }

  private diasDesdeHoy(dias: number): Date {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + dias);
    return fecha;
  }
}
