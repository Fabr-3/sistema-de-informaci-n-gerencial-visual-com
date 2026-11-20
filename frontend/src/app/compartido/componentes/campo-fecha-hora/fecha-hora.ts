/** Conversion entre "AAAA-MM-DDTHH:mm" y la fecha y la hora que manejan los selectores. */

const dosDigitos = (numero: number): string => String(numero).padStart(2, '0');

/** Une la fecha (dia) y la hora (horas y minutos); vacio si falta alguna o no es valida. */
export function combinarFechaHora(fecha: Date | null, hora: Date | null): string {
  if (!fecha || !hora || Number.isNaN(fecha.getTime()) || Number.isNaN(hora.getTime())) {
    return '';
  }
  return (
    `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}` +
    `T${dosDigitos(hora.getHours())}:${dosDigitos(hora.getMinutes())}`
  );
}

/** Separa "AAAA-MM-DDTHH:mm[:ss]" en fecha y hora locales; nulos si el texto no es valido. */
export function separarFechaHora(valor: string | null | undefined): { fecha: Date | null; hora: Date | null } {
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(valor ?? '');
  if (!partes) {
    return { fecha: null, hora: null };
  }
  const [anio, mes, dia, horas, minutos] = partes.slice(1).map(Number);
  return { fecha: new Date(anio, mes - 1, dia), hora: new Date(anio, mes - 1, dia, horas, minutos) };
}
