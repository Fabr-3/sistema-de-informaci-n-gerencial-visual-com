import { GastoEvento, montoRegistrado, problemaComprobante } from './gasto';

describe('gastos del evento', () => {
  it('acepta fotos y PDF de hasta 5 MB como comprobante', () => {
    expect(problemaComprobante({ type: 'image/jpeg', size: 1024 })).toBeNull();
    expect(problemaComprobante({ type: 'application/pdf', size: 5 * 1024 * 1024 })).toBeNull();
    expect(problemaComprobante({ type: 'text/plain', size: 10 })).toContain('foto');
    expect(problemaComprobante({ type: 'image/png', size: 0 })).toContain('vacío');
    expect(problemaComprobante({ type: 'image/png', size: 6 * 1024 * 1024 })).toContain('5 MB');
  });

  it('suma solo los gastos registrados', () => {
    const gasto = (monto: number, estado: GastoEvento['estado']) => ({ monto, estado }) as GastoEvento;

    expect(montoRegistrado([gasto(30, 'REGISTRADO'), gasto(20.5, 'REGISTRADO'), gasto(100, 'ANULADO')])).toBe(50.5);
  });
});
