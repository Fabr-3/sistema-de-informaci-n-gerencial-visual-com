import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { ElementoMenu, menuPermitido } from '../../../../nucleo/diseno/menu';
import { ServicioSesion } from '../../../accesos/aplicacion/servicio-sesion';

@Component({
  selector: 'app-pagina-inicio',
  imports: [RouterLink, MatIconModule],
  templateUrl: './pagina-inicio.html',
  styleUrl: './pagina-inicio.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaInicio {
  private readonly sesion = inject(ServicioSesion);

  protected readonly usuario = this.sesion.usuario;
  /** Accesos directos a las secciones permitidas (sin incluir el propio inicio). */
  protected readonly accesosRapidos = computed<ElementoMenu[]>(() =>
    menuPermitido((permiso) => this.sesion.tienePermiso(permiso))
      .filter((grupo) => grupo.titulo !== null)
      .flatMap((grupo) => grupo.elementos),
  );
}
