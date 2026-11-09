import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';

import { aIsoLocal, desdeIsoLocal, finDelDia, inicioDelDia } from '../../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionUsuarios } from '../../../../aplicacion/gestion-usuarios';
import { Rol, requiereFechaExpiracion } from '../../../../dominio/modelos/rol';
import { DatosUsuario, Usuario } from '../../../../dominio/modelos/usuario';
import {
  DESCRIPCION_POLITICA_CONTRASENA,
  politicaContrasena,
} from '../../../validadores/validadores-contrasena';

export interface DatosDialogoUsuario {
  /** Ausente al crear un usuario nuevo. */
  readonly usuario?: Usuario;
  readonly roles: readonly Rol[];
}

const FORMATO_NOMBRE_USUARIO = /^[a-zA-Z0-9._-]{3,50}$/;
const LONGITUD_MAXIMA_NOMBRE = 100;
const LONGITUD_MAXIMA_EMAIL = 150;

/**
 * Alta y edicion de usuarios. El nombre de usuario y la contrasena temporal
 * solo se piden al crear; la fecha de expiracion es obligatoria para los
 * sub-contratados temporales.
 */
@Component({
  selector: 'app-dialogo-usuario',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-usuario.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoUsuario {
  private readonly datos = inject<DatosDialogoUsuario>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoUsuario, Usuario>);
  private readonly gestion = inject(GestionUsuarios);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly fechaOriginal = desdeIsoLocal(this.datos.usuario?.fechaExpiracion ?? null);

  protected readonly esEdicion = this.datos.usuario !== undefined;
  protected readonly roles = this.datos.roles;
  protected readonly politica = DESCRIPCION_POLITICA_CONTRASENA;
  protected readonly hoy = inicioDelDia(new Date());
  protected readonly guardando = signal(false);
  protected readonly requiereExpiracion = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombreUsuario: ['', [Validators.required, Validators.pattern(FORMATO_NOMBRE_USUARIO)]],
    nombres: ['', [Validators.required, Validators.maxLength(LONGITUD_MAXIMA_NOMBRE)]],
    apellidoPaterno: ['', [Validators.required, Validators.maxLength(LONGITUD_MAXIMA_NOMBRE)]],
    apellidoMaterno: ['', Validators.maxLength(LONGITUD_MAXIMA_NOMBRE)],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(LONGITUD_MAXIMA_EMAIL)]],
    idRol: [0, [Validators.required, Validators.min(1)]],
    fechaExpiracion: [null as Date | null, fechaFuturaSiCambia(this.fechaOriginal)],
    contrasenaTemporal: ['', [Validators.required, politicaContrasena]],
  });

  constructor() {
    this.cargarUsuario();
    this.formulario.controls.idRol.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((idRol) => this.ajustarVigencia(idRol));
    this.ajustarVigencia(this.formulario.controls.idRol.value);
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.operacion().subscribe({
      next: (usuario) => this.dialogo.close(usuario),
      error: (error: unknown) => {
        this.notificaciones.error(error);
        this.guardando.set(false);
      },
    });
  }

  private operacion(): Observable<Usuario> {
    const valores = this.formulario.getRawValue();
    const datosUsuario: DatosUsuario = {
      nombres: valores.nombres,
      apellidoPaterno: valores.apellidoPaterno,
      apellidoMaterno: valores.apellidoMaterno || null,
      email: valores.email,
      idRol: valores.idRol,
      fechaExpiracion: valores.fechaExpiracion ? aIsoLocal(finDelDia(valores.fechaExpiracion)) : null,
    };
    const usuario = this.datos.usuario;
    return usuario
      ? this.gestion.actualizar(usuario.id, datosUsuario)
      : this.gestion.crear({
          ...datosUsuario,
          nombreUsuario: valores.nombreUsuario,
          contrasenaTemporal: valores.contrasenaTemporal,
        });
  }

  /** En edicion se cargan los datos actuales y se omiten los campos exclusivos del alta. */
  private cargarUsuario(): void {
    const usuario = this.datos.usuario;
    if (!usuario) {
      return;
    }
    this.formulario.patchValue({
      nombreUsuario: usuario.nombreUsuario,
      nombres: usuario.nombres,
      apellidoPaterno: usuario.apellidoPaterno,
      apellidoMaterno: usuario.apellidoMaterno ?? '',
      email: usuario.email,
      idRol: usuario.idRol,
      fechaExpiracion: this.fechaOriginal,
    });
    this.formulario.controls.nombreUsuario.disable();
    this.formulario.controls.contrasenaTemporal.disable();
  }

  private ajustarVigencia(idRol: number): void {
    const obligatoria = requiereFechaExpiracion(this.roles.find((rol) => rol.id === idRol)?.codigo);
    const control = this.formulario.controls.fechaExpiracion;
    if (obligatoria) {
      control.addValidators(Validators.required);
    } else {
      control.removeValidators(Validators.required);
    }
    control.updateValueAndValidity({ emitEvent: false });
    this.requiereExpiracion.set(obligatoria);
  }
}

/**
 * La fecha debe ser hoy o posterior, salvo que no se haya modificado: asi se
 * pueden editar otros datos de un sub-contratado cuya vigencia ya termino.
 */
function fechaFuturaSiCambia(original: Date | null): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const fecha = control.value as Date | null;
    if (!fecha || (original && fecha.getTime() === original.getTime())) {
      return null;
    }
    return finDelDia(fecha) > new Date() ? null : { fechaPasada: true };
  };
}
