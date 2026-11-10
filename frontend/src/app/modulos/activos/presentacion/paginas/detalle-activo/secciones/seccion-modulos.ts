import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { catchError, filter, of, switchMap } from 'rxjs';

import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../../accesos/dominio/modelos/permisos';
import { HistorialActivos } from '../../../../aplicacion/historial-activos';
import { Activo, fueraDeInventario } from '../../../../dominio/modelos/activo';
import { ESTADOS_MODULO, EstadoModuloLed, ModuloLed, tipoVisualModulo } from '../../../../dominio/modelos/historial';

const LONGITUD_CODIGO_MODULO = 50;

/** Modulos LED de una pantalla, rastreados uno por uno. */
@Component({
  selector: 'app-seccion-modulos',
  imports: [KeyValuePipe, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './seccion-modulos.html',
  styleUrl: './seccion-modulos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeccionModulos {
  readonly activo = input.required<Activo>();

  private readonly historial = inject(HistorialActivos);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly estados = ESTADOS_MODULO;
  protected readonly tipoVisualModulo = tipoVisualModulo;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly modulos = signal<ModuloLed[]>([]);
  private readonly version = signal(0);
  protected readonly puedeAgregar = computed(
    () => this.sesion.tienePermiso(Permisos.ACTIVOS_GESTIONAR) && !fueraDeInventario(this.activo().estado),
  );
  protected readonly puedeCambiarEstado = computed(() =>
    this.sesion.tieneAlgunPermiso(Permisos.ACTIVOS_GESTIONAR, Permisos.MANTENIMIENTOS_REGISTRAR),
  );
  /** Cantidad de modulos por estado para el resumen. */
  protected readonly resumen = computed(() => {
    const conteo: Record<EstadoModuloLed, number> = { ACTIVO: 0, DEFECTUOSO: 0, RETIRADO: 0 };
    this.modulos().forEach((modulo) => conteo[modulo.estado]++);
    return conteo;
  });

  constructor() {
    toObservable(computed(() => ({ activo: this.activo(), version: this.version() })))
      .pipe(
        switchMap(({ activo }) =>
          this.historial.listarModulos(activo.id).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((modulos) => this.modulos.set(modulos));
  }

  protected agregar(): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Agregar módulo LED',
          etiqueta: 'Código del módulo',
          textoConfirmar: 'Agregar',
          longitudMaxima: LONGITUD_CODIGO_MODULO,
          indicacion: 'El código identifica al módulo de forma única en todo el inventario.',
        },
        width: '440px',
      })
      .afterClosed()
      .pipe(
        filter((codigo): codigo is string => !!codigo),
        switchMap((codigo) => this.historial.agregarModulo(this.activo().id, codigo)),
      )
      .subscribe({
        next: (modulo) => this.recargar(`Módulo ${modulo.codigo} agregado`),
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected cambiarEstado(modulo: ModuloLed, estado: EstadoModuloLed): void {
    this.historial.cambiarEstadoModulo(this.activo().id, modulo.id, estado).subscribe({
      next: () => this.recargar(`Módulo ${modulo.codigo}: ${ESTADOS_MODULO[estado].toLowerCase()}`),
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }

  private recargar(mensaje: string): void {
    this.notificaciones.exito(mensaje);
    this.version.update((valor) => valor + 1);
  }
}
