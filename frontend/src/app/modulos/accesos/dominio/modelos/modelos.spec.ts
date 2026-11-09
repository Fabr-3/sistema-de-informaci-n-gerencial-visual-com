import { usuario } from '../../pruebas/datos-prueba';
import { esAccionCritica } from './auditoria';
import { requiereFechaExpiracion } from './rol';
import { estadoUsuario } from './usuario';

describe('estadoUsuario', () => {
  it('prioriza inactivo sobre los demas estados', () => {
    expect(estadoUsuario(usuario({ activo: false, bloqueado: true })).etiqueta).toBe('Inactivo');
  });

  it('distingue expirado, bloqueado y contrasena temporal', () => {
    expect(estadoUsuario(usuario({ expirado: true })).etiqueta).toBe('Expirado');
    expect(estadoUsuario(usuario({ bloqueado: true })).tipo).toBe('peligro');
    expect(estadoUsuario(usuario({ debeCambiarContrasena: true })).tipo).toBe('alerta');
    expect(estadoUsuario(usuario()).etiqueta).toBe('Activo');
  });
});

describe('reglas de roles y auditoria', () => {
  it('solo el sub-contratado temporal requiere fecha de expiracion', () => {
    expect(requiereFechaExpiracion('SUBCONTRATADO_TEMPORAL')).toBe(true);
    expect(requiereFechaExpiracion('TECNICO')).toBe(false);
    expect(requiereFechaExpiracion(undefined)).toBe(false);
  });

  it('marca como criticas las acciones de seguridad', () => {
    expect(esAccionCritica('BLOQUEO_CUENTA')).toBe(true);
    expect(esAccionCritica('INICIO_SESION')).toBe(false);
  });
});
