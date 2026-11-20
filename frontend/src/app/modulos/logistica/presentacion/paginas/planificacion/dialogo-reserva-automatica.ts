import { DatePipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Observable, catchError, filter, finalize, forkJoin, map, of, switchMap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import { conflictosDeError } from '../../../../../compartido/utilidades/errores-http';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionProyectos } from '../../../../comercial/aplicacion/gestion-proyectos';
import { GestionProveedores } from '../../../../finanzas/aplicacion/gestion-proveedores';
import { Proveedor } from '../../../../finanzas/dominio/modelos/proveedor';
import { GestionSubcontrataciones } from '../../../../subcontratacion/aplicacion/gestion-subcontratacion';
import { GestionReservas } from '../../../aplicacion/gestion-reservas';
import { PlanEvento } from '../../../dominio/modelos/planificacion';
import {
  DisponibilidadModelo,
  FaltanteModelo,
  PedidoModelo,
  faltantesDe,
  pedidosReservables,
} from '../../../dominio/modelos/reservas';

const PROVEEDORES_SUGERIDOS = 100;
const MAXIMO_POR_MODELO = 100;

export interface DatosDialogoReservaAutomatica {
  readonly plan: PlanEvento;
}

/** Lo que hizo el dialogo, para el mensaje y la recarga del plan. */
export interface ResultadoReservaAutomatica {
  readonly reservadas: number;
  readonly subalquiladas: number;
  readonly eventoCancelado: boolean;
}

type GrupoSubalquiler = FormGroup<{ idProveedor: FormControl<number>; costo: FormControl<number | null> }>;

/**
 * Reserva por cantidades: se indica cuantas unidades de cada modelo hacen
 * falta y el sistema elige las libres. Si un modelo no alcanza, se ofrece
 * subalquilar lo que falta (egreso del proyecto y del evento) o cancelar el
 * evento.
 */
@Component({
  selector: 'app-dialogo-reserva-automatica',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-reserva-automatica.html',
  styleUrl: './dialogo-reserva-automatica.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoReservaAutomatica {
  protected readonly datos = inject<DatosDialogoReservaAutomatica>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoReservaAutomatica, ResultadoReservaAutomatica>);
  private readonly dialogos = inject(MatDialog);
  private readonly reservas = inject(GestionReservas);
  private readonly subcontrataciones = inject(GestionSubcontrataciones);
  private readonly proyectos = inject(GestionProyectos);
  private readonly proveedores = inject(GestionProveedores);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly formularios = inject(NonNullableFormBuilder);
  private readonly sesion = inject(ServicioSesion);

  protected readonly maximo = MAXIMO_POR_MODELO;
  protected readonly puedeSubalquilar = this.sesion.tienePermiso(Permisos.PROVEEDORES_GESTIONAR);
  protected readonly puedeCancelar = this.sesion.tienePermiso(Permisos.COMERCIAL_GESTIONAR);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly disponibilidad = signal<readonly DisponibilidadModelo[]>([]);
  protected readonly cantidades = signal<ReadonlyMap<number, number>>(new Map());
  protected readonly texto = signal('');
  protected readonly subalquilar = signal(false);
  private readonly gruposSubalquiler = new Map<number, GrupoSubalquiler>();

  protected readonly proveedoresActivos = toSignal(
    this.puedeSubalquilar
      ? this.proveedores
          .listar({ activo: true }, { pagina: 0, tamano: PROVEEDORES_SUGERIDOS })
          .pipe(
            map((pagina) => [...pagina.contenido]),
            catchError(() => of<Proveedor[]>([])),
          )
      : of<Proveedor[]>([]),
    { initialValue: [] as Proveedor[] },
  );

  protected readonly visibles = computed(() => {
    const texto = this.texto().trim().toLowerCase();
    return texto
      ? this.disponibilidad().filter((modelo) => `${modelo.modelo} ${modelo.categoria}`.toLowerCase().includes(texto))
      : this.disponibilidad();
  });
  protected readonly pedidos = computed<PedidoModelo[]>(() =>
    [...this.cantidades()]
      .filter(([, cantidad]) => cantidad > 0)
      .map(([idModelo, cantidad]) => ({ idModelo, cantidad })),
  );
  protected readonly faltantes = computed(() => faltantesDe(this.pedidos(), this.disponibilidad()));
  protected readonly totalPedido = computed(() => this.pedidos().reduce((suma, pedido) => suma + pedido.cantidad, 0));

  constructor() {
    this.cargarDisponibilidad();
  }

  protected cantidad(idModelo: number): number {
    return this.cantidades().get(idModelo) ?? 0;
  }

  protected cambiarCantidad(idModelo: number, valor: string): void {
    const numero = Math.max(0, Math.min(MAXIMO_POR_MODELO, Math.trunc(Number(valor) || 0)));
    this.cantidades.update((actuales) => new Map(actuales).set(idModelo, numero));
  }

  /** Cada faltante conserva su proveedor y costo aunque cambien las cantidades. */
  protected grupo(faltante: FaltanteModelo): GrupoSubalquiler {
    let grupo = this.gruposSubalquiler.get(faltante.idModelo);
    if (!grupo) {
      grupo = this.formularios.group({
        idProveedor: [0, Validators.min(1)],
        costo: this.formularios.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
      });
      this.gruposSubalquiler.set(faltante.idModelo, grupo);
    }
    return grupo;
  }

  /** Sin faltantes basta con pedir algo; con faltantes hay que subalquilarlos con proveedor y costo. */
  protected listo(): boolean {
    if (!this.totalPedido() || this.guardando()) {
      return false;
    }
    const faltantes = this.faltantes();
    return !faltantes.length || (this.subalquilar() && faltantes.every((faltante) => this.grupo(faltante).valid));
  }

  protected confirmar(): void {
    const faltantes = this.faltantes();
    if (faltantes.length) {
      faltantes.forEach((faltante) => this.grupo(faltante).markAllAsTouched());
    }
    if (!this.listo()) {
      return;
    }
    this.guardando.set(true);
    const reservables = pedidosReservables(this.pedidos(), this.disponibilidad());
    const reservar$ = reservables.length
      ? this.reservas.reservarAutomaticamente(this.datos.plan.evento.id, reservables)
      : of([]);
    reservar$
      .pipe(
        switchMap((reservadas) =>
          (faltantes.length ? forkJoin(faltantes.map((faltante) => this.registrarSubalquiler(faltante))) : of([])).pipe(
            map((subalquileres) => ({
              reservadas: reservadas.length,
              subalquiladas: subalquileres.length,
              eventoCancelado: false,
            })),
          ),
        ),
        finalize(() => this.guardando.set(false)),
      )
      .subscribe({
        next: (resultado) => this.dialogo.close(resultado),
        error: (error: unknown) => {
          if (conflictosDeError<FaltanteModelo>(error).length) {
            this.notificaciones.advertencia('Otro evento ocupó algunos equipos mientras tanto; revise las cantidades.');
            this.cargarDisponibilidad();
          } else {
            this.notificaciones.error(error);
          }
        },
      });
  }

  protected cancelarEvento(): void {
    const evento = this.datos.plan.evento;
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: {
          titulo: 'Cancelar el evento',
          mensaje: `No hay equipos suficientes para ${evento.nombre}. Al cancelarlo se liberan sus reservas.`,
          textoConfirmar: 'Cancelar evento',
          destructiva: true,
        },
        width: '460px',
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => this.proyectos.cambiarEstadoEvento(evento.idProyecto, evento.id, 'CANCELADO')),
      )
      .subscribe({
        next: () => this.dialogo.close({ reservadas: 0, subalquiladas: 0, eventoCancelado: true }),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private registrarSubalquiler(faltante: FaltanteModelo): Observable<unknown> {
    const { idProveedor, costo } = this.grupo(faltante).getRawValue();
    const evento = this.datos.plan.evento;
    return this.subcontrataciones.registrar({
      idProveedor,
      idServicio: null,
      idProyecto: evento.idProyecto,
      idEvento: evento.id,
      descripcion: `Subalquiler para ${evento.nombre}: faltaban ${faltante.faltante} de ${faltante.solicitada} unidades`,
      costo: costo ?? 0,
      fecha: formatDate(new Date(), 'yyyy-MM-dd', 'en-US'),
      idModelo: faltante.idModelo,
      cantidad: faltante.faltante,
    });
  }

  private cargarDisponibilidad(): void {
    this.cargando.set(true);
    this.reservas
      .disponibilidadPorModelo(this.datos.plan.evento.id)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (modelos) => this.disponibilidad.set(modelos),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }
}
