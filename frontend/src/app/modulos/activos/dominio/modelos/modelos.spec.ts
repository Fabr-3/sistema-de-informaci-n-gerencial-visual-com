import { activo, modelo } from '../../pruebas/datos-prueba';
import { avanceMantenimiento, reglasActivo, tipoVisualAlerta, tipoVisualEstado } from './activo';
import { EntradaHistorialModulo, ModuloLed, detalleCambio, horasEntre, reglasModulo, tipoVisualModulo } from './historial';
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

describe('modulos LED', () => {
  const modulo = (estado: ModuloLed['estado']): ModuloLed => ({
    id: 1,
    idActivoPadre: 5,
    codigo: 'MOD-A1',
    estado,
    fechaCreacion: '2026-03-01T09:00:00',
  });
  const entrada = (datos: Partial<EntradaHistorialModulo>): EntradaHistorialModulo => ({
    tipo: 'ALTA',
    pantallaOrigen: null,
    pantallaDestino: 'ABPL-0001',
    estadoAnterior: null,
    estadoNuevo: 'ACTIVO',
    moduloRelacionado: null,
    motivo: null,
    fecha: '2026-03-01T09:00:00',
    registradoPor: 'Marcelo Vargas',
    ...datos,
  });

  it('refleja las reglas de reemplazo y reasignacion del backend', () => {
    expect(reglasModulo.puedeReemplazarse(modulo('DEFECTUOSO'))).toBe(true);
    expect(reglasModulo.puedeReemplazarse(modulo('RETIRADO'))).toBe(false);
    expect(reglasModulo.puedeReasignarse(modulo('ACTIVO'))).toBe(true);
    expect(reglasModulo.puedeReasignarse(modulo('DEFECTUOSO'))).toBe(false);
  });

  it('describe cada cambio del historial', () => {
    expect(detalleCambio(entrada({}))).toBe('Registrado en ABPL-0001 (activo)');
    expect(detalleCambio(entrada({ tipo: 'CAMBIO_ESTADO', estadoAnterior: 'ACTIVO', estadoNuevo: 'DEFECTUOSO' }))).toBe(
      'De activo a defectuoso',
    );
    expect(
      detalleCambio(entrada({ tipo: 'REEMPLAZO', estadoAnterior: 'DEFECTUOSO', moduloRelacionado: 'MOD-A2' })),
    ).toBe('Reemplazado por MOD-A2 (estaba defectuoso)');
    expect(detalleCambio(entrada({ tipo: 'REEMPLAZO', moduloRelacionado: 'MOD-A1' }))).toBe('Reemplaza a MOD-A1');
    expect(
      detalleCambio(entrada({ tipo: 'REASIGNACION', pantallaOrigen: 'ABPL-0001', pantallaDestino: 'ABPL-0002' })),
    ).toBe('De ABPL-0001 a ABPL-0002');
  });
});
