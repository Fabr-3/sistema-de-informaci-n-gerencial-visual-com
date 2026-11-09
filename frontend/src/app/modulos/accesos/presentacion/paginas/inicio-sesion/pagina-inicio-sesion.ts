import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';

import { mensajeDeError } from '../../../../../compartido/utilidades/errores-http';
import { PARAMETRO_RETORNO, Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioSesion } from '../../../aplicacion/servicio-sesion';

@Component({
  selector: 'app-pagina-inicio-sesion',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pagina-inicio-sesion.html',
  styleUrl: './pagina-inicio-sesion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaInicioSesion {
  private readonly sesion = inject(ServicioSesion);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombreUsuario: ['', [Validators.required, Validators.maxLength(50)]],
    contrasena: ['', [Validators.required, Validators.maxLength(72)]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrarContrasena = signal(false);

  protected iniciarSesion(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.sesion.iniciarSesion(this.formulario.getRawValue()).subscribe({
      next: (sesion) => {
        const destino = sesion.usuario.debeCambiarContrasena
          ? rutaAbsoluta(Rutas.CAMBIO_CONTRASENA)
          : this.urlRetorno();
        void this.router.navigateByUrl(destino);
      },
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.enviando.set(false);
      },
    });
  }

  /** Solo se aceptan rutas internas para evitar redirecciones abiertas. */
  private urlRetorno(): string {
    const retorno = this.ruta.snapshot.queryParamMap.get(PARAMETRO_RETORNO);
    return retorno?.startsWith('/') && !retorno.startsWith('//') ? retorno : rutaAbsoluta(Rutas.INICIO);
  }
}
