import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';
import { Observable, catchError, filter, forkJoin, of, switchMap } from 'rxjs';

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
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { GestionActivos } from '../../../aplicacion/gestion-activos';
import {
  Activo,
  ESTADOS_OPERATIVOS,
  NIVELES_ALERTA,
  avanceMantenimiento,
  reglasActivo,
  tipoVisualAlerta,
  tipoVisualEstado,
} from '../../../dominio/modelos/activo';
import { DatosDialogoActivo, DialogoActivo } from '../activos/dialogo-activo/dialogo-activo';
import { SeccionMantenimientos } from './secciones/seccion-mantenimientos';
import { SeccionModulos } from './secciones/seccion-modulos';
import { SeccionUsos } from './secciones/seccion-usos';

const LONGITUD_MOTIVO_BAJA = 500;

/** Ficha de un activo: datos, horas de uso, mantenimiento y modulos LED. */
@Component({
  selector: 'app-pagina-detalle-activo',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    EtiquetaPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTabsModule,
    SeccionMantenimientos,
    SeccionModulos,
    SeccionUsos,
    SiPermiso,
  ],
  templateUrl: './pagina-detalle-activo.html',
  styleUrl: './pagina-detalle-activo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaDetalleActivo {
  /** Parametro de ruta :id (enlazado por withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly gestion = inject(GestionActivos);
  private readonly catalogos = inject(CatalogosActivos);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly permisos = Permisos;
  protected readonly estados = ESTADOS_OPERATIVOS;
  protected readonly niveles = NIVELES_ALERTA;
  protected readonly tipoVisualEstado = tipoVisualEstado;
  protected readonly tipoVisualAlerta = tipoVisualAlerta;
  protected readonly rutaListado = rutaAbsoluta(Rutas.ACTIVOS);
  protected readonly activo = signal<Activo | null>(null);
  protected readonly noEncontrado = signal(false);
  private readonly version = signal(0);

  protected readonly avance = computed(() => {
    const activo = this.activo();
    return activo ? avanceMantenimiento(activo) : null;
  });
  protected readonly puedeDarDeBaja = computed(() => {
    const activo = this.activo();
    return !!activo && reglasActivo.puedeDarDeBaja(activo);
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
      .subscribe((activo) => this.activo.set(activo));
  }

  protected recargar(): void {
    this.version.update((valor) => valor + 1);
  }

  protected editar(activo: Activo): void {
    forkJoin({ modelos: this.catalogos.listarModelos('UNIDAD'), bodegas: this.catalogos.listarBodegas() })
      .pipe(
        switchMap(({ modelos, bodegas }) =>
          this.dialogos
            .open<DialogoActivo, DatosDialogoActivo, Activo[]>(DialogoActivo, {
              data: { activo, modelos, bodegas },
              width: '640px',
            })
            .afterClosed(),
        ),
        filter(Boolean),
      )
      .subscribe({
        next: () => this.finalizarOperacion('Activo actualizado'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected darDeBaja(activo: Activo): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: `Dar de baja ${activo.codigoInventario}`,
          etiqueta: 'Motivo de la baja',
          textoConfirmar: 'Dar de baja',
          longitudMaxima: LONGITUD_MOTIVO_BAJA,
          indicacion: 'El activo dejará de estar disponible. Su historial se conserva.',
          multilinea: true,
          destructiva: true,
        },
        width: '480px',
      })
      .afterClosed()
      .pipe(
        filter((motivo): motivo is string => !!motivo),
        switchMap((motivo) => this.gestion.darDeBaja(activo.id, motivo)),
      )
      .subscribe({
        next: () => this.finalizarOperacion('Activo dado de baja'),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected eliminar(activo: Activo): void {
    this.confirmar({
      titulo: 'Eliminar activo',
      mensaje: `Se eliminará ${activo.codigoInventario}. Solo es posible si no tiene historial; de lo contrario, dele de baja.`,
      textoConfirmar: 'Eliminar',
      destructiva: true,
    })
      .pipe(switchMap(() => this.gestion.eliminar(activo.id)))
      .subscribe({
        next: () => {
          this.notificaciones.exito('Activo eliminado');
          void this.router.navigateByUrl(this.rutaListado);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  private confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogos
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, { data: datos, width: '440px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is boolean => confirmado === true));
  }

  private finalizarOperacion(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.recargar();
  }
}
