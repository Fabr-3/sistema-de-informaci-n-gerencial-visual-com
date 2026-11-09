import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { catchError, debounceTime, distinctUntilChanged, filter, forkJoin, of, switchMap, tap } from 'rxjs';

import {
  DatosConfirmacion,
  DialogoConfirmacion,
} from '../../../../../compartido/componentes/dialogo-confirmacion/dialogo-confirmacion';
import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { ElementoCatalogo } from '../../../dominio/modelos/catalogos';
import { Cliente, FiltroClientes, documentoPrincipal } from '../../../dominio/modelos/cliente';
import { DatosDialogoCliente, DialogoCliente } from './dialogo-cliente';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS_BASE = ['razonSocial', 'tipo', 'documento', 'contacto'];

interface CriteriosClientes {
  readonly filtro: FiltroClientes;
  readonly solicitud: SolicitudPagina;
}

/** Cartera de clientes con sus documentos y contactos. */
@Component({
  selector: 'app-pagina-clientes',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    SiPermiso,
  ],
  templateUrl: './pagina-clientes.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaClientes {
  private readonly gestion = inject(GestionComercial);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly permisos = Permisos;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly documentoPrincipal = documentoPrincipal;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly tiposCliente = signal<ElementoCatalogo[]>([]);
  private readonly tiposDocumento = signal<ElementoCatalogo[]>([]);
  private readonly tiposContacto = signal<ElementoCatalogo[]>([]);
  protected readonly resultado = signal<Pagina<Cliente> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosClientes>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  private readonly version = signal(0);
  protected readonly columnas = computed(() =>
    this.sesion.tienePermiso(Permisos.COMERCIAL_GESTIONAR) ? [...COLUMNAS_BASE, 'acciones'] : COLUMNAS_BASE,
  );

  constructor() {
    forkJoin({
      clientes: this.gestion.listarTiposCliente(),
      documentos: this.gestion.listarTiposDocumento(),
      contactos: this.gestion.listarTiposContacto(),
    })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: ({ clientes, documentos, contactos }) => {
          this.tiposCliente.set(clientes);
          this.tiposDocumento.set(documentos);
          this.tiposContacto.set(contactos);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });

    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(computed(() => ({ criterios: this.criterios(), version: this.version() })))
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ criterios }) =>
          this.gestion.listarClientes(criterios.filtro, criterios.solicitud).pipe(
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

  protected filtrar(cambios: Partial<FiltroClientes>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected editar(cliente?: Cliente): void {
    this.dialogos
      .open<DialogoCliente, DatosDialogoCliente, Cliente>(DialogoCliente, {
        data: {
          cliente,
          tiposCliente: this.tiposCliente(),
          tiposDocumento: this.tiposDocumento(),
          tiposContacto: this.tiposContacto(),
        },
        width: '720px',
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe((guardado) => this.finalizar(`Cliente ${guardado.razonSocial} guardado`));
  }

  protected eliminar(cliente: Cliente): void {
    this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: {
          titulo: 'Eliminar cliente',
          mensaje: `Se eliminará ${cliente.razonSocial}. Solo es posible si no tiene cotizaciones registradas.`,
          textoConfirmar: 'Eliminar',
          destructiva: true,
        },
        width: '440px',
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => this.gestion.eliminarCliente(cliente.id)),
      )
      .subscribe({
        next: () => this.finalizar('Cliente eliminado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private finalizar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.version.update((valor) => valor + 1);
  }
}
