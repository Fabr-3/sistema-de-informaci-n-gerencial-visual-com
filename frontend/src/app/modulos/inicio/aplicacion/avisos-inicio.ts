import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';

import { Rutas, rutaAbsoluta } from '../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../accesos/dominio/modelos/permisos';
import { GestionGerencial } from '../../gerencial/aplicacion/gestion-gerencial';
import { GestionPlanificacion } from '../../logistica/aplicacion/gestion-planificacion';
import { avisoAlertas, avisoAsignaciones } from '../dominio/avisos';

const DURACION_AVISO_MS = 12_000;
const CLAVE_MOSTRADO = 'vc-avisos-mostrados-';

interface Aviso {
  readonly mensaje: string;
  readonly ruta: string;
}

/**
 * Avisos temporales al ingresar: alertas pendientes (quien ve reportes) y
 * asignaciones inminentes o con cruce (tecnicos). Se muestran una vez por
 * sesion del navegador.
 */
@Injectable({ providedIn: 'root' })
export class AvisosInicio {
  private readonly sesion = inject(ServicioSesion);
  private readonly gerencial = inject(GestionGerencial);
  private readonly planificacion = inject(GestionPlanificacion);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  mostrarUnaVez(): void {
    const usuario = this.sesion.usuario();
    if (!usuario || this.yaMostrado(usuario.id)) {
      return;
    }
    this.marcarMostrado(usuario.id);
    this.aviso().subscribe((aviso) => {
      if (!aviso) {
        return;
      }
      this.snackBar
        .open(aviso.mensaje, 'Ver', { duration: DURACION_AVISO_MS })
        .onAction()
        .subscribe(() => void this.router.navigateByUrl(aviso.ruta));
    });
  }

  private aviso(): Observable<Aviso | null> {
    if (this.sesion.tienePermiso(Permisos.REPORTES_VER)) {
      return this.gerencial.alertas('PENDIENTE').pipe(
        map((alertas) => avisoAlertas(alertas)),
        map((mensaje) => (mensaje ? { mensaje, ruta: rutaAbsoluta(Rutas.GERENCIAL, Rutas.ALERTAS) } : null)),
        catchError(() => of(null)),
      );
    }
    if (this.sesion.tienePermiso(Permisos.ASIGNACIONES_VER_PROPIAS)) {
      return this.planificacion.misAsignaciones().pipe(
        map((mias) => avisoAsignaciones(mias.asignaciones, new Date())),
        map((mensaje) => (mensaje ? { mensaje, ruta: rutaAbsoluta(Rutas.LOGISTICA, Rutas.MIS_ASIGNACIONES) } : null)),
        catchError(() => of(null)),
      );
    }
    return of(null);
  }

  /** sessionStorage puede no estar disponible (modo privado): en ese caso se avisa cada vez. */
  private yaMostrado(idUsuario: number): boolean {
    try {
      return sessionStorage.getItem(CLAVE_MOSTRADO + idUsuario) !== null;
    } catch {
      return false;
    }
  }

  private marcarMostrado(idUsuario: number): void {
    try {
      sessionStorage.setItem(CLAVE_MOSTRADO + idUsuario, '1');
    } catch {
      // Sin almacenamiento de sesion el aviso se repetira al recargar.
    }
  }
}
