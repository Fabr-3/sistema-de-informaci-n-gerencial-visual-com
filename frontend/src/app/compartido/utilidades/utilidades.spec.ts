import { HttpErrorResponse } from '@angular/common/http';

import { formatearTamano } from './archivos';
import { MENSAJE_ERROR_GENERICO, MENSAJE_SIN_CONEXION, mensajeDeError } from './errores-http';
import { aIsoLocal, desdeIsoLocal, finDelDia } from './fechas';
import { parametrosConsulta } from './parametros-http';

describe('mensajeDeError', () => {
  it('usa el detalle de negocio de la API', () => {
    const error = new HttpErrorResponse({ status: 422, error: { detail: 'Debe existir al menos un administrador' } });
    expect(mensajeDeError(error)).toBe('Debe existir al menos un administrador');
  });

  it('agrega el primer error de campo', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Los datos enviados no son validos', errores: { email: 'formato incorrecto' } },
    });
    expect(mensajeDeError(error)).toBe('Los datos enviados no son validos: email formato incorrecto');
  });

  it('distingue la falta de conexion y los errores desconocidos', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toBe(MENSAJE_SIN_CONEXION);
    expect(mensajeDeError(new HttpErrorResponse({ status: 500, error: '<html>' }))).toBe(MENSAJE_ERROR_GENERICO);
    expect(mensajeDeError(new Error('x'))).toBe(MENSAJE_ERROR_GENERICO);
  });
});

describe('fechas', () => {
  it('formatea en ISO local sin zona horaria', () => {
    expect(aIsoLocal(new Date(2026, 0, 5, 7, 8, 9))).toBe('2026-01-05T07:08:09');
  });

  it('el fin del dia incluye el ultimo segundo', () => {
    expect(aIsoLocal(finDelDia(new Date(2026, 10, 8, 10, 0)))).toBe('2026-11-08T23:59:59');
  });

  it('interpreta fechas de la API ignorando fracciones de segundo', () => {
    expect(desdeIsoLocal('2026-11-08T23:59:56.628081683')).toEqual(new Date(2026, 10, 8, 23, 59, 56));
    expect(desdeIsoLocal(null)).toBeNull();
  });
});

describe('parametrosConsulta', () => {
  it('omite valores vacios y conserva los falsos', () => {
    const parametros = parametrosConsulta({ texto: '', idRol: undefined, activo: false, pagina: 0, entidad: null });
    expect(parametros.toString()).toBe('activo=false&pagina=0');
  });
});

describe('formatearTamano', () => {
  it('usa la unidad adecuada', () => {
    expect(formatearTamano(512)).toBe('512 B');
    expect(formatearTamano(160878)).toBe('157,1 KB');
    expect(formatearTamano(5 * 1024 * 1024)).toBe('5 MB');
  });
});
