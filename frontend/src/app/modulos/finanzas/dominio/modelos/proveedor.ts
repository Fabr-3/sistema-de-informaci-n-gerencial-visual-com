export interface Proveedor {
  readonly id: number;
  readonly razonSocial: string;
  readonly nit: string | null;
  readonly telefono: string | null;
  readonly email: string | null;
  readonly servicios: readonly string[];
}

export interface DatosProveedor {
  readonly razonSocial: string;
  readonly nit: string | null;
  readonly telefono: string | null;
  readonly email: string | null;
  readonly servicios: readonly string[];
}

export interface FiltroProveedores {
  readonly texto?: string;
}

/** Separa los servicios escritos por comas o en lineas distintas. */
export function separarServicios(texto: string): string[] {
  return texto
    .split(/[,\n]/)
    .map((servicio) => servicio.trim())
    .filter((servicio) => servicio.length > 0);
}
