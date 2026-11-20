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

import { guardarYCerrar } from '../../../../../compartido/utilidades/guardar-y-cerrar';
import {
  PERSONA_NATURAL,
  ayudaDocumento,
  documentoValido,
  esCarnet,
  normalizarNumeroDocumento,
  razonSocialValida,
  soloLetras,
} from '../../../../../compartido/utilidades/validadores-identidad';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionProveedores } from '../../../aplicacion/gestion-proveedores';
import {
  CatalogosProveedores,
  DocumentoProveedor,
  Proveedor,
  separarServicios,
} from '../../../dominio/modelos/proveedor';

export interface DatosDialogoProveedor {
  readonly proveedor?: Proveedor;
  readonly catalogos: CatalogosProveedores;
}

const LONGITUD_RAZON_SOCIAL = 150;
const LONGITUD_DIRECCION = 500;
const LONGITUD_DOCUMENTO = 50;
const LONGITUD_COMPLEMENTO = 2;
const LONGITUD_TELEFONO = 30;
const LONGITUD_EMAIL = 150;
const MAXIMO_DOCUMENTOS = 10;
const TIPO_POR_DEFECTO = 'Empresa privada';

/** Registro o edicion de un proveedor con los mismos datos y reglas que un cliente; los documentos son opcionales. */
@Component({
  selector: 'app-dialogo-proveedor',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dialogo-proveedor.html',
  styleUrl: './dialogo-proveedor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoProveedor {
  protected readonly datos = inject<DatosDialogoProveedor>(MAT_DIALOG_DATA);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly gestion = inject(GestionProveedores);
  private readonly dialogo = inject(MatDialogRef<DialogoProveedor, Proveedor>);
  private readonly formularios = inject(NonNullableFormBuilder);
  protected readonly proveedor = this.datos.proveedor;
  protected readonly maximoDocumentos = MAXIMO_DOCUMENTOS;
  protected readonly guardando = signal(false);

  protected readonly esCarnet = (idTipo: number): boolean => esCarnet(this.nombreTipoDocumento(idTipo));
  protected readonly ayudaDocumento = (idTipo: number): string => ayudaDocumento(this.nombreTipoDocumento(idTipo));
  protected readonly formulario = this.formularios.group({
    idTipoProveedor: [
      this.proveedor?.tipo.id ??
        this.datos.catalogos.tiposProveedor.find((tipo) => tipo.nombre === TIPO_POR_DEFECTO)?.id ??
        0,
      Validators.min(1),
    ],
    razonSocial: [this.proveedor?.razonSocial ?? '', [Validators.required, Validators.maxLength(LONGITUD_RAZON_SOCIAL)]],
    direccion: [this.proveedor?.direccion ?? '', Validators.maxLength(LONGITUD_DIRECCION)],
    documentos: this.formularios.array((this.proveedor?.documentos ?? []).map((documento) => this.grupoDocumento(documento))),
    telefono: [this.proveedor?.telefono ?? '', Validators.maxLength(LONGITUD_TELEFONO)],
    email: [this.proveedor?.email ?? '', [Validators.email, Validators.maxLength(LONGITUD_EMAIL)]],
    servicios: [this.proveedor?.servicios.join(', ') ?? ''],
  });

  /** Una persona natural se nombra solo con letras; una empresa o institucion admite numeros y signos. */
  constructor() {
    this.formulario.controls.idTipoProveedor.valueChanges
      .pipe(
        startWith(this.formulario.controls.idTipoProveedor.value),
        map(
          (idTipo) =>
            this.datos.catalogos.tiposProveedor.find((tipo) => tipo.id === idTipo)?.nombre === PERSONA_NATURAL,
        ),
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

  protected agregarDocumento(): void {
    this.documentos.push(this.grupoDocumento());
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    guardarYCerrar(
      this.gestion.guardar(this.proveedor?.id ?? null, {
        ...valores,
        documentos: valores.documentos.map((documento) => ({
          ...documento,
          numero: normalizarNumeroDocumento(documento.numero),
          complemento: this.esCarnet(documento.idTipoDocumento) ? documento.complemento : null,
        })),
        servicios: separarServicios(valores.servicios),
      }),
      this.dialogo,
      this.guardando,
      (error) => this.notificaciones.error(error),
    );
  }

  private grupoDocumento(documento?: DocumentoProveedor) {
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

  private nombreTipoDocumento(idTipo: number): string | undefined {
    return this.datos.catalogos.tiposDocumento.find((tipo) => tipo.id === idTipo)?.nombre;
  }
}
