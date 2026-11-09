/** Rutas de navegacion de la aplicacion (sin barra inicial, como las usa el Router). */
export const Rutas = {
  INICIO: '',
  INICIO_SESION: 'inicio-sesion',
  CAMBIO_CONTRASENA: 'cambio-contrasena',
  ACCESOS: 'accesos',
  USUARIOS: 'usuarios',
  ROLES: 'roles',
  AUDITORIA: 'auditoria',
  RESPALDOS: 'respaldos',
} as const;

/** Parametro de consulta con la URL a la que se vuelve tras iniciar sesion. */
export const PARAMETRO_RETORNO = 'retorno';

export const rutaAbsoluta = (...segmentos: string[]): string => '/' + segmentos.filter(Boolean).join('/');
