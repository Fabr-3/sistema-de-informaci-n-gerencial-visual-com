import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { ServicioSesion } from '../../../modulos/accesos/aplicacion/servicio-sesion';
import { Rutas, rutaAbsoluta } from '../../configuracion/rutas';
import { GrupoMenu, menuPermitido } from '../menu';

/** Ancho por debajo del cual el menu lateral se superpone al contenido. */
const CONSULTA_PANTALLA_ANGOSTA = '(max-width: 959.98px)';

@Component({
  selector: 'app-plantilla-principal',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    MatMenuModule,
    MatSidenavModule,
    MatToolbarModule,
  ],
  templateUrl: './plantilla-principal.html',
  styleUrl: './plantilla-principal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlantillaPrincipal {
  private readonly sesion = inject(ServicioSesion);
  private readonly router = inject(Router);

  protected readonly usuario = this.sesion.usuario;
  protected readonly rutaCambioContrasena = rutaAbsoluta(Rutas.CAMBIO_CONTRASENA);
  protected readonly pantallaAngosta = toSignal(
    inject(BreakpointObserver)
      .observe(CONSULTA_PANTALLA_ANGOSTA)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  protected readonly menu = computed<GrupoMenu[]>(() => menuPermitido((permiso) => this.sesion.tienePermiso(permiso)));

  protected readonly iniciales = computed(() =>
    (this.usuario()?.nombreCompleto ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0].toUpperCase())
      .join(''),
  );

  protected alNavegar(menuLateral: MatSidenav): void {
    if (this.pantallaAngosta()) {
      void menuLateral.close();
    }
  }

  protected cerrarSesion(): void {
    this.sesion.cerrarSesion().subscribe(() => void this.router.navigateByUrl(rutaAbsoluta(Rutas.INICIO_SESION)));
  }
}
