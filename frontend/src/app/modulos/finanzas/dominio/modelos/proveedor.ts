import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';

export interface DocumentoProveedor {
  readonly id: number | null;
  readonly tipo: ElementoCatalogo;
  readonly numero: string;
  /** Solo en el carnet de identidad (por ejemplo, 1A). */
  readonly complemento: string | null;
}

/** Proveedor con los mismos datos que un cliente; anulado no se elige en operaciones nuevas. */
export interface Proveedor {
  readonly id: number;
  readonly tipo: ElementoCatalogo;
  readonly razonSocial: string;
  readonly direccion: string | null;
  readonly documentos: readonly DocumentoProveedor[];
  /** NIT o primer documento, listo para mostrar; nulo si no tiene documentos. */
  readonly documentoPrincipal: string | null;
  readonly telefono: string | null;
  readonly email: string | null;
  readonly servicios: readonly string[];
  readonly activo: boolean;
}

export interface DatosDocumentoProveedor {
  readonly id: number | null;
  readonly idTipoDocumento: number;
  readonly numero: string;
  readonly complemento: string | null;
}

export interface DatosProveedor {
  readonly idTipoProveedor: number;
  readonly razonSocial: string;
  readonly direccion: string | null;
  readonly documentos: readonly DatosDocumentoProveedor[];
  readonly telefono: string | null;
  readonly email: string | null;
  readonly servicios: readonly string[];
}

export interface CatalogosProveedores {
  readonly tiposProveedor: readonly ElementoCatalogo[];
  readonly tiposDocumento: readonly ElementoCatalogo[];
}

/** activo indefinido lista todos; true, solo los que se pueden elegir en operaciones nuevas. */
export interface FiltroProveedores {
  readonly texto?: string;
  readonly activo?: boolean;
}

/** Separa los servicios escritos por comas o en lineas distintas. */
export function separarServicios(texto: string): string[] {
  return texto
    .split(/[,\n]/)
    .map((servicio) => servicio.trim())
    .filter((servicio) => servicio.length > 0);
}
