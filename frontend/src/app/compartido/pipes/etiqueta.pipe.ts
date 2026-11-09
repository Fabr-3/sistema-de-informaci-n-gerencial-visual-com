import { Pipe, PipeTransform } from '@angular/core';

/**
 * Traduce un codigo (p. ej. un estado) a su etiqueta visible usando un mapa
 * de etiquetas. Devuelve el propio codigo si no tiene etiqueta y un guion si
 * no hay valor.
 *
 * Uso: {{ activo.estado | etiqueta: estados }}
 */
@Pipe({ name: 'etiqueta' })
export class EtiquetaPipe implements PipeTransform {
  transform(codigo: string | null | undefined, etiquetas: Readonly<Record<string, string>>): string {
    if (codigo === null || codigo === undefined) {
      return '—';
    }
    return etiquetas[codigo] ?? codigo;
  }
}
