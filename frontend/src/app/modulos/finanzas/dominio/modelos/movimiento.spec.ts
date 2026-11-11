import { Movimiento, TipoMovimiento, montoConSigno, tipoVisualNaturaleza, tiposRegistrables } from './movimiento';

describe('movimientos financieros', () => {
  const cobro: TipoMovimiento = { id: 1, nombre: 'Cobro de servicios', naturaleza: 'INGRESO', requiereProveedor: false, descuentaInventario: false, codigoSistema: null };
  const compra: TipoMovimiento = { id: 5, nombre: 'Compra de equipos y materiales', naturaleza: 'EGRESO', requiereProveedor: true, descuentaInventario: false, codigoSistema: null };
  const mantenimiento: TipoMovimiento = { ...compra, id: 11, nombre: 'Mantenimiento de equipos', requiereProveedor: false, codigoSistema: 'MANTENIMIENTO' };
  const movimiento = (tipo: TipoMovimiento) => ({ tipo, monto: 1250.5 }) as Movimiento;

  it('expresa los egresos con signo negativo', () => {
    expect(montoConSigno(movimiento(cobro))).toBe(1250.5);
    expect(montoConSigno(movimiento(compra))).toBe(-1250.5);
    expect(tipoVisualNaturaleza('EGRESO')).toBe('peligro');
  });

  it('ofrece los tipos de la naturaleza sin los automaticos', () => {
    expect(tiposRegistrables([cobro, compra, mantenimiento], 'EGRESO')).toEqual([compra]);
  });
});
