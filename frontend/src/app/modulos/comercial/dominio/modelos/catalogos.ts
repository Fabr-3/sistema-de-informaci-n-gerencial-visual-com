/** Elemento de un catalogo simple (tipos de cliente, documento y contacto). */
export interface ElementoCatalogo {
  readonly id: number;
  readonly nombre: string;
}

export interface Servicio {
  readonly id: number;
  readonly nombre: string;
  readonly descripcion: string | null;
  readonly unidadMedida: string;
  /** Precio referencial en Bs por unidad de medida. */
  readonly precioReferencial: number;
  readonly activo: boolean;
}

export type DatosServicio = Omit<Servicio, 'id'>;

export interface Lugar {
  readonly id: number;
  readonly nombre: string;
  readonly direccion: string;
  readonly ciudad: string | null;
}

export type DatosLugar = Omit<Lugar, 'id'>;

/** Texto visible de un lugar: "Nombre (Ciudad)". */
export function etiquetaLugar(lugar: Lugar): string {
  return lugar.ciudad ? `${lugar.nombre} (${lugar.ciudad})` : lugar.nombre;
}
