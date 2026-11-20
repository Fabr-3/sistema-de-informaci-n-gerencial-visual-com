import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { map, startWith } from 'rxjs';

import {
  PERSONA_NATURAL,
  ayudaDocumento,
  contactoValido,
  documentoValido,
  esCarnet,
  normalizarNumeroDocumento,
  razonSocialValida,
  soloLetras,
} from '../../../../../compartido/utilidades/validadores-identidad';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionComercial } from '../../../aplicacion/gestion-comercial';
import { ElementoCatalogo } from '../../../dominio/modelos/catalogos';
import { Cliente, ContactoCliente, DocumentoCliente } from '../../../dominio/modelos/cliente';

export interface DatosDialogoCliente {
  readonly cliente?: Cliente;
  readonly tiposCliente: readonly ElementoCatalogo[];
  readonly tiposDocumento: readonly ElementoCatalogo[];
  readonly tiposContacto: readonly ElementoCatalogo[];
}

const LONGITUD_RAZON_SOCIAL = 150;
const LONGITUD_DIRECCION = 500;
const LONGITUD_DOCUMENTO = 50;
const LONGITUD_COMPLEMENTO = 2;
const LONGITUD_CONTACTO = 150;
const LONGITUD_REFERENCIA = 100;
const MAXIMO_DOCUMENTOS = 10;
const MAXIMO_CONTACTOS = 20;

/** Alta y edicion de clientes; al menos un documento (CI o NIT) es obligatorio. */
@Component({
  selector: 'app-dialogo-cliente',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-cliente.html',
  styleUrl: './dialogo-cliente.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoCliente {
  protected readonly datos = inject<DatosDialogoCliente>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoCliente, Cliente>);
  private readonly gestion = inject(GestionComercial);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly formularios = inject(NonNullableFormBuilder);

  protected readonly maximoDocumentos = MAXIMO_DOCUMENTOS;
  protected readonly maximoContactos = MAXIMO_CONTACTOS;
  protected readonly guardando = signal(false);
  private readonly cliente = this.datos.cliente;

  protected readonly esCarnet = (idTipo: number): boolean => esCarnet(this.nombreTipoDocumento(idTipo));
  protected readonly ayudaDocumento = (idTipo: number): string => ayudaDocumento(this.nombreTipoDocumento(idTipo));
  protected readonly formulario = this.formularios.group({
    idTipoCliente: [this.cliente?.tipo.id ?? 0, Validators.min(1)],
    razonSocial: [this.cliente?.razonSocial ?? '', [Validators.required, Validators.maxLength(LONGITUD_RAZON_SOCIAL)]],
    direccion: [this.cliente?.direccion ?? '', Validators.maxLength(LONGITUD_DIRECCION)],
    documentos: this.formularios.array(
      (this.cliente?.documentos.length ? this.cliente.documentos : [undefined]).map((documento) =>
        this.grupoDocumento(documento),
      ),
      Validators.minLength(1),
    ),
    contactos: this.formularios.array((this.cliente?.contactos ?? []).map((contacto) => this.grupoContacto(contacto))),
  });

  /** Una persona natural se nombra solo con letras; una empresa o institucion admite numeros y signos. */
  constructor() {
    this.formulario.controls.idTipoCliente.valueChanges
      .pipe(
        startWith(this.formulario.controls.idTipoCliente.value),
        map((idTipo) => this.datos.tiposCliente.find((tipo) => tipo.id === idTipo)?.nombre === PERSONA_NATURAL),
        takeUntilDestroyed(),
      )
      .subscribe((persona) => {
        const control = this.formulario.controls.razonSocial;
        control.setValidators([
          Validators.required,
          Validators.maxLength(LONGITUD_RAZON_SOCIAL),
          persona ? soloLetras : razonSocialValida,
        ]);
        control.updateValueAndValidity({ emitEvent: false });
      });
  }

  protected get documentos() {
    return this.formulario.controls.documentos;
  }

  protected get contactos() {
    return this.formulario.controls.contactos;
  }

  protected agregarDocumento(): void {
    this.documentos.push(this.grupoDocumento());
  }

  protected agregarContacto(): void {
    this.contactos.push(this.grupoContacto());
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.guardando.set(true);
    this.gestion
      .guardarCliente(this.cliente?.id ?? null, {
        idTipoCliente: valores.idTipoCliente,
        razonSocial: valores.razonSocial,
        direccion: valores.direccion,
        documentos: valores.documentos.map((documento) => ({
          ...documento,
          numero: normalizarNumeroDocumento(documento.numero),
          complemento: this.esCarnet(documento.idTipoDocumento) ? documento.complemento.trim().toUpperCase() || null : null,
        })),
        contactos: valores.contactos,
      })
      .subscribe({
        next: (cliente) => this.dialogo.close(cliente),
        error: (error: unknown) => {
          this.notificaciones.error(error);
          this.guardando.set(false);
        },
      });
  }

  private grupoDocumento(documento?: DocumentoCliente) {
    return this.formularios.group(
      {
        id: [documento?.id ?? (null as number | null)],
        idTipoDocumento: [documento?.tipo.id ?? 0, Validators.min(1)],
        numero: [documento?.numero ?? '', [Validators.required, Validators.maxLength(LONGITUD_DOCUMENTO)]],
        complemento: [documento?.complemento ?? '', Validators.maxLength(LONGITUD_COMPLEMENTO)],
      },
      { validators: documentoValido((idTipo) => this.nombreTipoDocumento(idTipo)) },
    );
  }

  private grupoContacto(contacto?: ContactoCliente) {
    return this.formularios.group(
      {
        id: [contacto?.id ?? (null as number | null)],
        idTipoContacto: [contacto?.tipo.id ?? 0, Validators.min(1)],
        valor: [contacto?.valor ?? '', [Validators.required, Validators.maxLength(LONGITUD_CONTACTO)]],
        referencia: [contacto?.referencia ?? '', [Validators.maxLength(LONGITUD_REFERENCIA), razonSocialValida]],
      },
      {
        validators: contactoValido(
          (idTipo) => this.datos.tiposContacto.find((tipo) => tipo.id === idTipo)?.nombre,
        ),
      },
    );
  }

  private nombreTipoDocumento(idTipo: number): string | undefined {
    return this.datos.tiposDocumento.find((tipo) => tipo.id === idTipo)?.nombre;
  }
}
