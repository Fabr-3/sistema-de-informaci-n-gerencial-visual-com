/** Equipo por unidad reservado para el plan logistico de un evento. */
export interface Reserva {
  readonly id: number;
  readonly idActivo: number;
  readonly codigo: string;
  readonly modelo: string;
  readonly categoria: string;
  readonly salida: string;
  readonly retorno: string;
  readonly estado: 'ACTIVA' | 'LIBERADA';
}

/** Equipo libre durante el periodo de la reserva del evento. */
export interface ActivoReservable {
  readonly id: number;
  readonly codigo: string;
  readonly modelo: string;
  readonly categoria: string;
}

/** Reserva de otro evento que ocupa el equipo en el mismo periodo (respuesta 409). */
export interface ConflictoReserva {
  readonly idActivo: number;
  readonly codigoActivo: string;
  readonly idEvento: number;
  readonly evento: string;
  readonly salida: string;
  readonly retorno: string;
}

export interface FiltroActivosReservables {
  readonly texto?: string;
  readonly idCategoria?: number;
}

/** Agrega o quita un equipo de la seleccion sin mutar la original. */
export function alternarSeleccion(seleccion: ReadonlySet<number>, id: number): ReadonlySet<number> {
  const resultado = new Set(seleccion);
  if (!resultado.delete(id)) {
    resultado.add(id);
  }
  return resultado;
}

/** Categorias presentes en la lista, en orden alfabetico, para filtrar en pantalla. */
export function categoriasDe(activos: readonly ActivoReservable[]): string[] {
  return [...new Set(activos.map((activo) => activo.categoria))].sort((a, b) => a.localeCompare(b, 'es'));
}
