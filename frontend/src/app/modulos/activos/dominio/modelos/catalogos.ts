export interface Categoria {
  readonly id: number;
  readonly nombre: string;
}

export interface Fabricante {
  readonly id: number;
  readonly nombre: string;
}

export interface Bodega {
  readonly id: number;
  readonly nombre: string;
  readonly direccion: string | null;
  readonly activa: boolean;
}

export interface DatosBodega {
  readonly nombre: string;
  readonly direccion: string | null;
  readonly activa: boolean;
}

export interface TipoMantenimiento {
  readonly id: number;
  readonly nombre: string;
  /** Al finalizar, reinicia el contador de horas del mantenimiento preventivo. */
  readonly reiniciaHoras: boolean;
}
