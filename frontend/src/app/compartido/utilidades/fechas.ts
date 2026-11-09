const relleno = (valor: number, digitos = 2): string => valor.toString().padStart(digitos, '0');

/**
 * Convierte una fecha a ISO local sin zona horaria (yyyy-MM-ddTHH:mm:ss),
 * formato que espera la API para LocalDateTime.
 */
export function aIsoLocal(fecha: Date): string {
  return (
    `${fecha.getFullYear()}-${relleno(fecha.getMonth() + 1)}-${relleno(fecha.getDate())}` +
    `T${relleno(fecha.getHours())}:${relleno(fecha.getMinutes())}:${relleno(fecha.getSeconds())}`
  );
}

/** Ultimo segundo del dia: una vigencia "hasta el 15" incluye todo el dia 15. */
export function finDelDia(fecha: Date): Date {
  const resultado = new Date(fecha);
  resultado.setHours(23, 59, 59, 0);
  return resultado;
}

export function inicioDelDia(fecha: Date): Date {
  const resultado = new Date(fecha);
  resultado.setHours(0, 0, 0, 0);
  return resultado;
}

/** Interpreta una fecha ISO local de la API (sin zona) como hora local. */
export function desdeIsoLocal(valor: string | null): Date | null {
  return valor ? new Date(valor.substring(0, 19)) : null;
}

/** Valor para inputs datetime-local (yyyy-MM-ddTHH:mm); la API lo acepta como LocalDateTime. */
export function aFechaHoraLocal(fecha: Date): string {
  return aIsoLocal(fecha).substring(0, 16);
}

/** Fecha ISO (yyyy-MM-dd) en hora local, para inputs de fecha y LocalDate. */
export function aFechaIso(fecha: Date): string {
  return aIsoLocal(fecha).substring(0, 10);
}

/** Fecha ISO (yyyy-MM-dd) de la API como Date local a medianoche; nulo si no hay valor. */
export function desdeFechaIso(fechaIso: string | null): Date | null {
  return fechaIso ? new Date(`${fechaIso}T00:00`) : null;
}
