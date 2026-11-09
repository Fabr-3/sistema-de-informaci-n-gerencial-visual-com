import { DatePipe } from '@angular/common';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

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
import { GestionRoles } from '../../../aplicacion/gestion-roles';
import { GestionUsuarios } from '../../../aplicacion/gestion-usuarios';
import { ServicioSesion } from '../../../aplicacion/servicio-sesion';
import { Permisos } from '../../../dominio/modelos/permisos';
import { Rol } from '../../../dominio/modelos/rol';
import { FiltroUsuarios, Usuario, estadoUsuario } from '../../../dominio/modelos/usuario';
import { SiPermiso } from '../../directivas/si-permiso';
import { DatosDialogoUsuario, DialogoUsuario } from './dialogo-usuario/dialogo-usuario';
import { DialogoRestablecerContrasena } from './dialogo-restablecer-contrasena/dialogo-restablecer-contrasena';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS_BASE = ['nombreUsuario', 'nombreCompleto', 'email', 'rol', 'estado', 'ultimoAcceso'] as const;
const COLUMNA_ACCIONES = 'acciones';

interface CriteriosBusqueda {
  readonly filtro: FiltroUsuarios;
  readonly solicitud: SolicitudPagina;
}

@Component({
  selector: 'app-pagina-usuarios',
  imports: [
    DatePipe,
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
    MatTooltipModule,
    SiPermiso,
  ],
  templateUrl: './pagina-usuarios.html',
  styleUrl: './pagina-usuarios.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaUsuarios {
  private readonly gestion = inject(GestionUsuarios);
  private readonly gestionRoles = inject(GestionRoles);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly permisos = Permisos;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estadoUsuario = estadoUsuario;
  protected readonly busqueda = new FormControl('', { nonNullable: true });

  protected readonly roles = signal<Rol[]>([]);
  protected readonly resultado = signal<Pagina<Usuario> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosBusqueda>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  /** Se incrementa para volver a consultar con los mismos criterios. */
  private readonly version = signal(0);

  protected readonly columnas = computed<string[]>(() =>
    this.sesion.tienePermiso(Permisos.USUARIOS_GESTIONAR) ? [...COLUMNAS_BASE, COLUMNA_ACCIONES] : [...COLUMNAS_BASE],
  );
  protected readonly idUsuarioActual = computed(() => this.sesion.usuario()?.id);

  constructor() {
    this.gestionRoles
      .listarRoles()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (roles) => this.roles.set(roles), error: (error: unknown) => this.notificaciones.error(error) });

    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(computed(() => ({ criterios: this.criterios(), version: this.version() })))
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ criterios }) => this.consultar(criterios)),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
      });
  }

  protected filtrar(cambios: Partial<FiltroUsuarios>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({
      filtro,
      solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize },
    }));
  }

  protected crear(): void {
    this.abrirFormulario({ roles: this.roles() }, 'Usuario creado');
  }

  protected editar(usuario: Usuario): void {
    this.abrirFormulario({ usuario, roles: this.roles() }, 'Usuario actualizado');
  }

  protected restablecerContrasena(usuario: Usuario): void {
    this.dialogos
      .open(DialogoRestablecerContrasena, { data: usuario, width: '480px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.finalizarOperacion('Contraseña restablecida'));
  }

  protected cambiarEstado(usuario: Usuario): void {
    const desactivar = usuario.activo;
    this.confirmar({
      titulo: desactivar ? 'Desactivar usuario' : 'Activar usuario',
      mensaje: desactivar
        ? `${usuario.nombreCompleto} no podrá ingresar al sistema y se cerrarán sus sesiones.`
        : `${usuario.nombreCompleto} podrá volver a ingresar al sistema.`,
      textoConfirmar: desactivar ? 'Desactivar' : 'Activar',
      destructiva: desactivar,
    })
      .pipe(switchMap(() => this.gestion.cambiarEstado(usuario)))
      .subscribe({
        next: () => this.finalizarOperacion(desactivar ? 'Usuario desactivado' : 'Usuario activado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected eliminar(usuario: Usuario): void {
    this.confirmar({
      titulo: 'Eliminar usuario',
      mensaje: `Se eliminará la cuenta de ${usuario.nombreCompleto}. Su historial de auditoría se conserva.`,
      textoConfirmar: 'Eliminar',
      destructiva: true,
    })
      .pipe(switchMap(() => this.gestion.eliminar(usuario.id)))
      .subscribe({
        next: () => this.finalizarOperacion('Usuario eliminado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private consultar(criterios: CriteriosBusqueda): Observable<Pagina<Usuario> | null> {
    return this.gestion.listar(criterios.filtro, criterios.solicitud).pipe(
      catchError((error: unknown) => {
        this.notificaciones.error(error);
        return of(null);
      }),
    );
  }

  private abrirFormulario(datos: DatosDialogoUsuario, mensajeExito: string): void {
    this.dialogos
      .open<DialogoUsuario, DatosDialogoUsuario, Usuario>(DialogoUsuario, { data: datos, width: '640px' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.finalizarOperacion(mensajeExito));
  }

  /** Emite solo si el usuario confirma. */
  private confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, { data: datos, width: '440px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is boolean => confirmado === true));
  }

  private finalizarOperacion(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.version.update((valor) => valor + 1);
  }
}
