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

/** Unidades libres de un modelo durante el periodo de la reserva (pueden ser cero). */
export interface DisponibilidadModelo {
  readonly idModelo: number;
  readonly modelo: string;
  readonly categoria: string;
  readonly disponibles: number;
}

/** Cantidad de unidades de un modelo que se reservan sin elegirlas una por una. */
export interface PedidoModelo {
  readonly idModelo: number;
  readonly cantidad: number;
}

/** Pedido que supera las unidades libres (respuesta 409 o calculo en pantalla). */
export interface FaltanteModelo {
  readonly idModelo: number;
  readonly modelo: string;
  readonly solicitada: number;
  readonly disponibles: number;
  readonly faltante: number;
}

/** Unidades alquiladas a un proveedor porque faltaban para el evento. */
export interface EquipoSubalquilado {
  readonly idSubcontratacion: number;
  readonly modelo: string;
  readonly cantidad: number;
  readonly proveedor: string;
  readonly costo: number;
}

/** Pedidos con cantidad positiva que superan lo disponible, con lo que falta de cada uno. */
export function faltantesDe(
  pedidos: readonly PedidoModelo[],
  disponibilidad: readonly DisponibilidadModelo[],
): FaltanteModelo[] {
  return pedidos.flatMap((pedido) => {
    const libre = disponibilidad.find((modelo) => modelo.idModelo === pedido.idModelo);
    const disponibles = libre?.disponibles ?? 0;
    return pedido.cantidad > disponibles
      ? [
          {
            idModelo: pedido.idModelo,
            modelo: libre?.modelo ?? '',
            solicitada: pedido.cantidad,
            disponibles,
            faltante: pedido.cantidad - disponibles,
          },
        ]
      : [];
  });
}

/** Lo que se reserva de cada pedido: lo pedido, sin pasar de las unidades libres. */
export function pedidosReservables(
  pedidos: readonly PedidoModelo[],
  disponibilidad: readonly DisponibilidadModelo[],
): PedidoModelo[] {
  return pedidos
    .map((pedido) => ({
      idModelo: pedido.idModelo,
      cantidad: Math.min(
        pedido.cantidad,
        disponibilidad.find((modelo) => modelo.idModelo === pedido.idModelo)?.disponibles ?? 0,
      ),
    }))
    .filter((pedido) => pedido.cantidad > 0);
}
