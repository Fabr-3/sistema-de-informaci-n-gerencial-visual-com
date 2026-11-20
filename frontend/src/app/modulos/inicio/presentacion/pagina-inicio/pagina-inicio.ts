import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { ElementoMenu, menuPermitido } from '../../../../nucleo/diseno/menu';
import { Rutas, rutaAbsoluta } from '../../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../accesos/dominio/modelos/permisos';
import { GestionPlanificacion } from '../../../logistica/aplicacion/gestion-planificacion';
import { AsignacionPropia } from '../../../logistica/dominio/modelos/personal';

/** Cantidad de asignaciones que se resumen en el inicio del tecnico. */
const ASIGNACIONES_VISIBLES = 5;

@Component({
  selector: 'app-pagina-inicio',
  imports: [DatePipe, RouterLink, MatIconModule],
  templateUrl: './pagina-inicio.html',
  styleUrl: './pagina-inicio.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaInicio {
  private readonly sesion = inject(ServicioSesion);

  protected readonly usuario = this.sesion.usuario;
  protected readonly rutaMisAsignaciones = rutaAbsoluta(Rutas.LOGISTICA, Rutas.MIS_ASIGNACIONES);
  protected readonly veAsignaciones = this.sesion.tienePermiso(Permisos.ASIGNACIONES_VER_PROPIAS);
  /** Accesos directos a las secciones permitidas (sin incluir el propio inicio). */
  protected readonly accesosRapidos = computed<ElementoMenu[]>(() =>
    menuPermitido((permiso) => this.sesion.tienePermiso(permiso))
      .filter((grupo) => grupo.titulo !== null)
      .flatMap((grupo) => grupo.elementos),
  );
  /** Pantallazo de las asignaciones pendientes del tecnico; undefined mientras carga. */
  protected readonly asignaciones = toSignal<AsignacionPropia[] | null>(
    this.veAsignaciones
      ? inject(GestionPlanificacion)
          .misAsignaciones()
          .pipe(
            map((mias) => mias.asignaciones.slice(0, ASIGNACIONES_VISIBLES)),
            catchError(() => of(null)),
          )
      : of(null),
  );
}
