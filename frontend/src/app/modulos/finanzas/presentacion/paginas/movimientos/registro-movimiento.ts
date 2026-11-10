import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { busquedaRemota } from '../../../../../compartido/utilidades/busqueda-remota';
import { aFechaIso } from '../../../../../compartido/utilidades/fechas';
import { requiereSeleccion, seleccionOpcional } from '../../../../../compartido/utilidades/validadores-seleccion';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionActivos } from '../../../../activos/aplicacion/gestion-activos';
import { InventarioLotes } from '../../../../activos/aplicacion/inventario-lotes';
import { Activo } from '../../../../activos/dominio/modelos/activo';
import { Existencia } from '../../../../activos/dominio/modelos/lotes';
import { GestionProyectos } from '../../../../comercial/aplicacion/gestion-proyectos';
import { Evento, Proyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionMovimientos } from '../../../aplicacion/gestion-movimientos';
import { GestionProveedores } from '../../../aplicacion/gestion-proveedores';
import { NATURALEZAS, Naturaleza, TipoMovimiento, tiposDe } from '../../../dominio/modelos/movimiento';
import { Proveedor } from '../../../dominio/modelos/proveedor';

const SUGERENCIAS = 10;
const LONGITUD_CONCEPTO = 200;
const LONGITUD_FACTURA = 30;
const MONTO_MINIMO = 0.01;

interface LoteVendido {
  readonly existencia: Existencia;
  readonly cantidad: number;
}

/**
 * Registro de un ingreso o egreso. En las ventas de equipos se indican las
 * unidades y cantidades de lote que salen del inventario.
 */
@Component({
  selector: 'app-registro-movimiento',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './registro-movimiento.html',
  styleUrl: './registro-movimiento.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroMovimiento {
  private readonly gestion = inject(GestionMovimientos);
  private readonly proveedores = inject(GestionProveedores);
  private readonly proyectos = inject(GestionProyectos);
  private readonly activos = inject(GestionActivos);
  private readonly lotes = inject(InventarioLotes);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly naturalezas = Object.keys(NATURALEZAS) as Naturaleza[];
  protected readonly etiquetasNaturaleza = NATURALEZAS;
  protected readonly rutaListado = rutaAbsoluta(Rutas.FINANZAS, Rutas.MOVIMIENTOS);
  protected readonly hoy = new Date();
  protected readonly guardando = signal(false);
  protected readonly eventos = signal<Evento[]>([]);
  protected readonly unidades = signal<Activo[]>([]);
  protected readonly lotesVendidos = signal<LoteVendido[]>([]);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    naturaleza: ['INGRESO' as Naturaleza],
    idTipo: [0, Validators.min(1)],
    fecha: [new Date() as Date | null, Validators.required],
    concepto: ['', [Validators.required, Validators.maxLength(LONGITUD_CONCEPTO)]],
    monto: [null as number | null, [Validators.required, Validators.min(MONTO_MINIMO)]],
    numeroFactura: ['', Validators.maxLength(LONGITUD_FACTURA)],
    proyecto: [null as Proyecto | string | null, seleccionOpcional],
    idEvento: [{ value: null as number | null, disabled: true }],
    proveedor: [null as Proveedor | string | null, seleccionOpcional],
  });
  protected readonly busquedaUnidad = new FormControl<Activo | string | null>(null);
  protected readonly loteElegido = new FormControl<Existencia | null>(null);
  protected readonly cantidadLote = new FormControl<number | null>(1, [Validators.min(1)]);

  protected readonly tipos = toSignal(this.gestion.tipos(), { initialValue: [] as TipoMovimiento[] });
  private readonly naturaleza = toSignal(this.formulario.controls.naturaleza.valueChanges, {
    initialValue: this.formulario.controls.naturaleza.value,
  });
  private readonly idTipo = toSignal(this.formulario.controls.idTipo.valueChanges, {
    initialValue: this.formulario.controls.idTipo.value,
  });
  protected readonly tiposDisponibles = computed(() => tiposDe(this.tipos(), this.naturaleza()));
  protected readonly tipo = computed(() => this.tipos().find((tipo) => tipo.id === this.idTipo()) ?? null);
  protected readonly esVenta = computed(() => !!this.tipo()?.descuentaInventario);

  protected readonly proyectosSugeridos = toSignal(
    busquedaRemota(this.formulario.controls.proyecto, (texto) =>
      this.proyectos.listar({ texto }, { pagina: 0, tamano: SUGERENCIAS }).pipe(map((pagina) => [...pagina.contenido])),
    ),
    { initialValue: [] as Proyecto[] },
  );
  protected readonly proveedoresSugeridos = toSignal(
    busquedaRemota(this.formulario.controls.proveedor, (texto) =>
      this.proveedores.listar({ texto }, { pagina: 0, tamano: SUGERENCIAS }).pipe(map((pagina) => [...pagina.contenido])),
    ),
    { initialValue: [] as Proveedor[] },
  );
  protected readonly unidadesSugeridas = toSignal(
    busquedaRemota(this.busquedaUnidad, (texto) =>
      this.activos
        .listar({ texto, estado: 'DISPONIBLE' }, { pagina: 0, tamano: SUGERENCIAS })
        .pipe(map((pagina) => pagina.contenido.filter((activo) => !this.unidades().some((u) => u.id === activo.id)))),
    ),
    { initialValue: [] as Activo[] },
  );
  protected readonly existencias = toSignal(
    this.lotes.listar({}).pipe(
      map((existencias) => existencias.filter((existencia) => existencia.cantidad > 0)),
      catchError(() => of<Existencia[]>([])),
    ),
    { initialValue: [] as Existencia[] },
  );

  protected readonly mostrarProyecto = (proyecto: Proyecto | string | null): string =>
    typeof proyecto === 'string' ? proyecto : proyecto ? `${proyecto.nombre} · ${proyecto.cliente.razonSocial}` : '';
  protected readonly mostrarProveedor = (proveedor: Proveedor | string | null): string =>
    typeof proveedor === 'string' ? proveedor : (proveedor?.razonSocial ?? '');
  protected readonly mostrarUnidad = (activo: Activo | string | null): string =>
    typeof activo === 'string' ? activo : activo ? `${activo.codigoInventario} · ${activo.modelo}` : '';

  constructor() {
    this.formulario.controls.naturaleza.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.formulario.patchValue({ idTipo: 0, proveedor: null });
    });
    this.formulario.controls.idTipo.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.ajustarProveedor());
    this.formulario.controls.proyecto.valueChanges.pipe(takeUntilDestroyed()).subscribe((proyecto) => {
      this.formulario.controls.idEvento.setValue(null);
      this.cargarEventos(typeof proyecto === 'object' && proyecto ? proyecto.id : null);
    });
  }

  protected agregarUnidad(evento: MatAutocompleteSelectedEvent): void {
    const activo = evento.option.value as Activo;
    this.unidades.update((actuales) => [...actuales, activo]);
    this.busquedaUnidad.setValue('');
  }

  protected quitarUnidad(activo: Activo): void {
    this.unidades.update((actuales) => actuales.filter((unidad) => unidad.id !== activo.id));
  }

  protected agregarLote(): void {
    const existencia = this.loteElegido.value;
    const cantidad = Number(this.cantidadLote.value);
    if (!existencia || !cantidad || cantidad < 1) {
      return;
    }
    if (cantidad > existencia.cantidad) {
      this.notificaciones.advertencia(`Solo hay ${existencia.cantidad} unidades de ${existencia.modelo} en ${existencia.bodega}`);
      return;
    }
    this.lotesVendidos.update((actuales) => [
      ...actuales.filter((lote) => lote.existencia.id !== existencia.id),
      { existencia, cantidad },
    ]);
    this.loteElegido.setValue(null);
    this.cantidadLote.setValue(1);
  }

  protected quitarLote(lote: LoteVendido): void {
    this.lotesVendidos.update((actuales) => actuales.filter((actual) => actual !== lote));
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    if (this.esVenta() && !this.unidades().length && !this.lotesVendidos().length) {
      this.notificaciones.advertencia('Indique los equipos o cantidades que se venden');
      return;
    }
    const valores = this.formulario.getRawValue();
    const proyecto = typeof valores.proyecto === 'object' ? valores.proyecto : null;
    const proveedor = typeof valores.proveedor === 'object' ? valores.proveedor : null;
    this.guardando.set(true);
    this.gestion
      .registrar({
        idTipo: valores.idTipo,
        concepto: valores.concepto,
        monto: Number(valores.monto),
        fecha: aFechaIso(valores.fecha!),
        idProyecto: proyecto?.id ?? null,
        idEvento: proyecto ? valores.idEvento : null,
        idProveedor: proveedor?.id ?? null,
        numeroFactura: valores.numeroFactura,
        items: this.esVenta()
          ? [
              ...this.unidades().map((activo) => ({ idActivo: activo.id })),
              ...this.lotesVendidos().map((lote) => ({ idExistencia: lote.existencia.id, cantidad: lote.cantidad })),
            ]
          : [],
      })
      .subscribe({
        next: () => {
          this.notificaciones.exito('Movimiento registrado');
          void this.router.navigateByUrl(this.rutaListado);
        },
        error: (error: unknown) => {
          this.guardando.set(false);
          this.notificaciones.error(error);
        },
      });
  }

  /** El proveedor es obligatorio en compras, subalquileres y subcontrataciones. */
  private ajustarProveedor(): void {
    const control = this.formulario.controls.proveedor;
    control.setValidators(this.tipo()?.requiereProveedor ? requiereSeleccion : seleccionOpcional);
    control.updateValueAndValidity();
  }

  /** El evento solo se elige cuando el proyecto tiene eventos. */
  private cargarEventos(idProyecto: number | null): void {
    const control = this.formulario.controls.idEvento;
    control.disable();
    if (idProyecto === null) {
      this.eventos.set([]);
      return;
    }
    this.proyectos.obtener(idProyecto).subscribe({
      next: (detalle) => {
        this.eventos.set([...detalle.eventos]);
        if (detalle.eventos.length) {
          control.enable();
        }
      },
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }
}
