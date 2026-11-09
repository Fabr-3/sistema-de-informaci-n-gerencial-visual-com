import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../aplicacion/servicio-sesion';
import {
  DESCRIPCION_POLITICA_CONTRASENA,
  LONGITUD_MAXIMA_CONTRASENA,
  camposIguales,
  politicaContrasena,
} from '../../validadores/validadores-contrasena';

/**
 * Cambio de la contrasena propia. Es obligatorio tras recibir una contrasena
 * temporal; en ese caso no se ofrece volver.
 */
@Component({
  selector: 'app-pagina-cambio-contrasena',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './pagina-cambio-contrasena.html',
  styleUrl: './pagina-cambio-contrasena.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCambioContrasena {
  private readonly sesion = inject(ServicioSesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly politica = DESCRIPCION_POLITICA_CONTRASENA;
  protected readonly obligatorio = this.sesion.debeCambiarContrasena();
  protected readonly enviando = signal(false);
  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      contrasenaActual: ['', [Validators.required, Validators.maxLength(LONGITUD_MAXIMA_CONTRASENA)]],
      nuevaContrasena: ['', [Validators.required, politicaContrasena]],
      confirmacion: ['', Validators.required],
    },
    { validators: camposIguales('nuevaContrasena', 'confirmacion') },
  );

  protected cambiar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { contrasenaActual, nuevaContrasena } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.sesion.cambiarContrasena({ contrasenaActual, nuevaContrasena }).subscribe({
      next: () => {
        this.notificaciones.exito('Contraseña actualizada');
        void this.router.navigateByUrl(rutaAbsoluta(Rutas.INICIO));
      },
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.enviando.set(false);
      },
    });
  }

  protected volver(): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.INICIO));
  }

  protected cerrarSesion(): void {
    this.sesion.cerrarSesion().subscribe(() => void this.router.navigateByUrl(rutaAbsoluta(Rutas.INICIO_SESION)));
  }
}
