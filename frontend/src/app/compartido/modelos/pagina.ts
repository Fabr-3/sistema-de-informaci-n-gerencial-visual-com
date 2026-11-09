/** Resultado paginado devuelto por la API. */
export interface Pagina<T> {
  readonly contenido: readonly T[];
  readonly pagina: number;
  readonly tamano: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
}

export interface SolicitudPagina {
  readonly pagina: number;
  readonly tamano: number;
}

export const TAMANOS_PAGINA = [10, 20, 50] as const;
export const TAMANO_PAGINA_POR_DEFECTO = 20;
