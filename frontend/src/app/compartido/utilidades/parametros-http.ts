import { HttpParams } from '@angular/common/http';

type ValorParametro = string | number | boolean | null | undefined;

/** Construye parametros de consulta omitiendo los valores vacios. */
export function parametrosConsulta(valores: Readonly<Record<string, ValorParametro>>): HttpParams {
  let parametros = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      parametros = parametros.set(clave, String(valor));
    }
  }
  return parametros;
}
