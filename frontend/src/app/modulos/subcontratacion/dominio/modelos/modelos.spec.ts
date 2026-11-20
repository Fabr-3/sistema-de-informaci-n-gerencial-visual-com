import { Consumible, stockTrasAjuste } from './consumible';
import { Consumo, costoRegistrado } from './consumo';
import { Subcontratacion, tipoVisualSubcontratacion, totalVigente } from './subcontratacion';

const subcontratacion = (costo: number, estado: Subcontratacion['estado']): Subcontratacion => ({
  id: costo,
  proveedor: { id: 1, nombre: 'Generadores Illimani S.R.L.' },
  servicio: null,
  modelo: null,
  cantidad: null,
  proyecto: { id: 2, nombre: 'Concierto aniversario' },
  evento: null,
  descripcion: 'Generador de 60 kVA',
  costo,
  fecha: '2026-11-10',
  estado,
  motivoAnulacion: null,
  idMovimiento: 5,
});

const consumo = (costoTotal: number, estado: Consumo['estado']): Consumo => ({
  id: costoTotal,
  consumible: { id: 1, nombre: 'Cinta gaffer negra' },
  unidadMedida: 'rollo',
  proyecto: { id: 2, nombre: 'Concierto aniversario' },
  evento: null,
  cantidad: 1,
  costoUnitario: costoTotal,
  costoTotal,
  fecha: '2026-11-10',
  estado,
  motivoAnulacion: null,
  registradoPor: 'Juan Mamani',
});

describe('subcontratacion y consumibles', () => {
  it('solo suma las subcontrataciones y los consumos vigentes', () => {
    expect(totalVigente([subcontratacion(1800, 'REGISTRADA'), subcontratacion(500, 'ANULADA')])).toBe(1800);
    expect(costoRegistrado([consumo(90, 'REGISTRADO'), consumo(135, 'ANULADO'), consumo(13, 'REGISTRADO')])).toBe(103);
    expect(tipoVisualSubcontratacion('ANULADA')).toBe('neutro');
  });

  it('anticipa el stock que deja un ajuste', () => {
    const cinta: Consumible = {
      id: 1,
      nombre: 'Cinta gaffer negra',
      unidadMedida: 'rollo',
      stockActual: 4.5,
      stockMinimo: 2,
      costoUnitario: 45,
      activo: true,
      stockBajo: false,
    };
    expect(stockTrasAjuste(cinta, -1.25)).toBe(3.25);
    expect(stockTrasAjuste(cinta, -5)).toBe(-0.5);
  });
});
