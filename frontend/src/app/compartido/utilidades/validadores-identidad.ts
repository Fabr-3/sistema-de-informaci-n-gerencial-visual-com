import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Nombres de los tipos de documento y de cliente (catalogos) con reglas propias. */
export const CARNET_IDENTIDAD = 'Carnet de identidad';
export const NIT = 'NIT';
export const PASAPORTE = 'Pasaporte';
export const PERSONA_NATURAL = 'Persona natural';

/** Mismas reglas que el backend (ReglasTexto y ReglasDocumento). */
const NOMBRE_PERSONA = /^\p{L}[\p{L} .'-]*$/u;
const RAZON_SOCIAL = /^[\p{L}\d][\p{L}\d .,&'()/-]*$/u;
const FORMATOS_DOCUMENTO: Readonly<Record<string, RegExp>> = {
  [CARNET_IDENTIDAD]: /^\d{4,10}( [A-Z]{2})?$/,
  [NIT]: /^\d{6,15}$/,
  [PASAPORTE]: /^[A-Z0-9]{5,15}$/,
};
const COMPLEMENTO = /^[0-9A-Z]{1,2}$/;
const AYUDA_DOCUMENTO: Readonly<Record<string, string>> = {
  [CARNET_IDENTIDAD]: 'Solo números, con la sigla opcional del departamento (4876512 LP)',
  [NIT]: 'Solo números (6 a 15)',
  [PASAPORTE]: 'De 5 a 15 letras o números',
};

const texto = (valor: unknown): string => (typeof valor === 'string' ? valor.trim() : '');

/** Solo letras, espacios, apostrofos, puntos y guiones (nombres de personas). */
export const soloLetras: ValidatorFn = (control) => {
  const valor = texto(control.value);
  return !valor || NOMBRE_PERSONA.test(valor) ? null : { soloLetras: true };
};

/** Razon social de empresas e instituciones: letras, numeros y signos habituales. */
export const razonSocialValida: ValidatorFn = (control) => {
  const valor = texto(control.value);
  return !valor || RAZON_SOCIAL.test(valor) ? null : { razonSocial: true };
};

export function esCarnet(nombreTipo: string | undefined): boolean {
  return nombreTipo === CARNET_IDENTIDAD;
}

/** Numero en mayusculas y con un solo espacio, igual que lo guarda el backend. */
export function normalizarNumeroDocumento(numero: string): string {
  return numero.trim().replace(/\s+/g, ' ').toUpperCase();
}

export function ayudaDocumento(nombreTipo: string | undefined): string {
  return (nombreTipo && AYUDA_DOCUMENTO[nombreTipo]) || '';
}

/**
 * Valida un grupo con numero, complemento y el tipo de documento. nombreTipo
 * obtiene el nombre del catalogo a partir del id elegido.
 */
export function documentoValido(
  nombreTipo: (idTipo: number) => string | undefined,
  campos = { tipo: 'idTipoDocumento', numero: 'numero', complemento: 'complemento' },
): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const tipo = nombreTipo(Number(grupo.get(campos.tipo)?.value));
    const numero = normalizarNumeroDocumento(texto(grupo.get(campos.numero)?.value));
    const complemento = texto(grupo.get(campos.complemento)?.value).toUpperCase();
    const errores: ValidationErrors = {};
    const formato = tipo ? FORMATOS_DOCUMENTO[tipo] : undefined;
    if (numero && formato && !formato.test(numero)) {
      errores['numeroDocumento'] = true;
    }
    if (complemento && (!esCarnet(tipo) || !COMPLEMENTO.test(complemento))) {
      errores['complemento'] = true;
    }
    return Object.keys(errores).length ? errores : null;
  };
}

const FORMATOS_CONTACTO: Readonly<Record<string, RegExp>> = {
  Celular: /^\+?\d{7,15}$/,
  'Teléfono fijo': /^\+?[\d ]{6,20}( int\. ?\d{1,5})?$/,
  'Correo electrónico': /^[^@\s]+@[^@\s]+\.[^@\s]+$/,
};

/** Valida el valor de un contacto segun su tipo (celular, telefono fijo o correo). */
export function contactoValido(
  nombreTipo: (idTipo: number) => string | undefined,
  campos = { tipo: 'idTipoContacto', valor: 'valor' },
): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const tipo = nombreTipo(Number(grupo.get(campos.tipo)?.value));
    const valor = texto(grupo.get(campos.valor)?.value);
    const formato = tipo ? FORMATOS_CONTACTO[tipo] : undefined;
    return valor && formato && !formato.test(valor) ? { formatoContacto: true } : null;
  };
}
