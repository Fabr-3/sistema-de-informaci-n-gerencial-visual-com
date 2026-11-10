import { activo, modelo } from '../../pruebas/datos-prueba';
import { avanceMantenimiento, reglasActivo, tipoVisualAlerta, tipoVisualEstado } from './activo';
import { horasEntre, tipoVisualModulo } from './historial';
import { nombreCompletoModelo } from './modelo-activo';

describe('reglas de activos', () => {
  it('calcula el avance del intervalo de mantenimiento con tope de 100 %', () => {
    expect(avanceMantenimiento(activo())).toBeNull();
    expect(avanceMantenimiento(activo({ horasMantenimiento: 200, horasDesdeMantenimiento: 50 }))).toBe(25);
    expect(avanceMantenimiento(activo({ horasMantenimiento: 100, horasDesdeMantenimiento: 130 }))).toBe(100);
  });

  it('refleja las transiciones permitidas por el backend', () => {
    expect(reglasActivo.puedeRegistrarUso(activo())).toBe(true);
    expect(reglasActivo.puedeRegistrarUso(activo({ estado: 'EN_MANTENIMIENTO' }))).toBe(false);
    expect(reglasActivo.puedeRegistrarUso(activo({ estado: 'VENDIDO' }))).toBe(false);
    expect(reglasActivo.puedeIniciarMantenimiento(activo({ estado: 'RESERVADO' }))).toBe(false);
    expect(reglasActivo.puedeDarDeBaja(activo({ estado: 'EN_USO' }))).toBe(false);
    expect(reglasActivo.puedeDarDeBaja(activo({ estado: 'BAJA' }))).toBe(false);
    expect(reglasActivo.puedeDarDeBaja(activo({ estado: 'EN_MANTENIMIENTO' }))).toBe(false);
    expect(reglasActivo.puedeDarDeBaja(activo())).toBe(true);
  });

  it('asigna el estilo visual segun estado y alerta', () => {
    expect(tipoVisualEstado('BAJA')).toBe('peligro');
    expect(tipoVisualAlerta('PROXIMO')).toBe('alerta');
    expect(tipoVisualAlerta('VENCIDO')).toBe('peligro');
    expect(tipoVisualModulo('DEFECTUOSO')).toBe('peligro');
  });

  it('calcula horas entre fechas locales y arma el nombre del modelo', () => {
    expect(horasEntre('2026-11-01T08:00', '2026-11-01T17:30')).toBe(9.5);
    expect(nombreCompletoModelo(modelo())).toBe('Absen PL3.9');
  });
});
