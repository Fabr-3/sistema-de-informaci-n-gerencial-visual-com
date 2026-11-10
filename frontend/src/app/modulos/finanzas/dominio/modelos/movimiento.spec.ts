import { Movimiento, TipoMovimiento, montoConSigno, tipoVisualNaturaleza, tiposDe } from './movimiento';

describe('movimientos financieros', () => {
  const cobro: TipoMovimiento = { id: 1, nombre: 'Cobro de servicios', naturaleza: 'INGRESO', requiereProveedor: false, descuentaInventario: false };
  const compra: TipoMovimiento = { id: 5, nombre: 'Compra de equipos y materiales', naturaleza: 'EGRESO', requiereProveedor: true, descuentaInventario: false };
  const movimiento = (tipo: TipoMovimiento) => ({ tipo, monto: 1250.5 }) as Movimiento;

  it('expresa los egresos con signo negativo', () => {
    expect(montoConSigno(movimiento(cobro))).toBe(1250.5);
    expect(montoConSigno(movimiento(compra))).toBe(-1250.5);
    expect(tipoVisualNaturaleza('EGRESO')).toBe('peligro');
  });

  it('filtra los tipos por naturaleza', () => {
    expect(tiposDe([cobro, compra], 'EGRESO')).toEqual([compra]);
  });
});
