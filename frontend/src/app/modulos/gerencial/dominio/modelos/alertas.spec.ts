import { AlertaActivo, contarPorSeveridad, filtrarAlertas, tipoVisualSeveridad } from './alertas';

describe('alertas de activos', () => {
  const alerta = (codigo: string, severidad: AlertaActivo['severidad'], tipo: AlertaActivo['tipo']): AlertaActivo => ({
    tipo,
    severidad,
    idActivo: 1,
    codigo,
    modelo: 'Robe MegaPointe',
    categoria: 'Iluminación',
    detalle: '',
  });
  const alertas = [
    alerta('RBMP-0005', 'CRITICA', 'MANTENIMIENTO_VENCIDO'),
    alerta('ABPL-0003', 'ADVERTENCIA', 'MODULOS_DEFECTUOSOS'),
    alerta('RBMP-0001', 'ADVERTENCIA', 'MANTENIMIENTO_PROXIMO'),
    alerta('SHULX-0007', 'INFORMATIVA', 'SIN_USO'),
  ];

  it('filtra por severidad y por tipo', () => {
    expect(filtrarAlertas(alertas, null, null)).toHaveLength(4);
    expect(filtrarAlertas(alertas, 'ADVERTENCIA', null).map((a) => a.codigo)).toEqual(['ABPL-0003', 'RBMP-0001']);
    expect(filtrarAlertas(alertas, 'ADVERTENCIA', 'MODULOS_DEFECTUOSOS')).toHaveLength(1);
    expect(filtrarAlertas(alertas, 'CRITICA', 'SIN_USO')).toHaveLength(0);
  });

  it('cuenta por severidad y asigna su estilo', () => {
    expect(contarPorSeveridad(alertas)).toEqual({ CRITICA: 1, ADVERTENCIA: 2, INFORMATIVA: 1 });
    expect(tipoVisualSeveridad('CRITICA')).toBe('peligro');
    expect(tipoVisualSeveridad('INFORMATIVA')).toBe('neutro');
  });
});
