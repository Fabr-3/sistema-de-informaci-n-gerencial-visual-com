import { etiquetaLugar } from './catalogos';
import { Cliente, ResumenComercialCliente, documentoPrincipal, tasaConversion } from './cliente';
import { subtotalLinea, tipoVisualCotizacion, totalCotizacion } from './cotizacion';
import {
  DetalleProyecto,
  TAMANO_MAXIMO_FIRMADO,
  TRANSICIONES_PROYECTO,
  pasosProyecto,
  problemaArchivoFirmado,
  proyectoVigente,
  tipoVisualEvento,
} from './proyecto';

describe('reglas comerciales', () => {
  it('calcula subtotales y total con descuento en bolivianos', () => {
    const pantalla = { idServicio: 1, cantidad: 24, precioUnitario: 180, descuento: 320 };
    const sonido = { idServicio: 2, cantidad: 2, precioUnitario: 4500, descuento: 0 };

    expect(subtotalLinea(pantalla)).toBe(4000);
    expect(totalCotizacion([pantalla, sonido])).toBe(13000);
    expect(totalCotizacion([{ idServicio: 3, cantidad: 3, precioUnitario: 0.1, descuento: 0 }])).toBe(0.3);
  });

  it('prefiere el NIT como documento principal', () => {
    const cliente = {
      documentos: [
        { id: 1, tipo: { id: 1, nombre: 'Carnet de identidad' }, numero: '4512870 LP' },
        { id: 2, tipo: { id: 2, nombre: 'NIT' }, numero: '411029015' },
      ],
    } as unknown as Cliente;

    expect(documentoPrincipal(cliente)).toBe('NIT 411029015');
    expect(documentoPrincipal({ documentos: [] } as unknown as Cliente)).toBe('—');
  });

  it('refleja las transiciones de estado del proyecto', () => {
    expect(TRANSICIONES_PROYECTO.PLANIFICACION).toEqual(['EN_EJECUCION', 'CANCELADO']);
    expect(TRANSICIONES_PROYECTO.FINALIZADO).toEqual([]);
    expect(proyectoVigente('EN_EJECUCION')).toBe(true);
    expect(proyectoVigente('CANCELADO')).toBe(false);
  });

  it('asigna estilos y etiquetas visibles', () => {
    expect(tipoVisualCotizacion('RECHAZADA')).toBe('peligro');
    expect(tipoVisualEvento('EJECUTADO')).toBe('activo');
    expect(etiquetaLugar({ id: 1, nombre: 'Recinto Ferial', direccion: 'Av. Roca', ciudad: 'Santa Cruz' })).toBe(
      'Recinto Ferial (Santa Cruz)',
    );
  });

  it('solo admite como contrato firmado un PDF de hasta 10 MB', () => {
    const archivo = (tipo: string, tamano: number) =>
      ({ type: tipo, size: tamano, name: 'contrato.pdf' }) as unknown as File;

    expect(problemaArchivoFirmado(archivo('application/pdf', TAMANO_MAXIMO_FIRMADO))).toBeNull();
    expect(problemaArchivoFirmado(archivo('image/jpeg', 1024))).toContain('PDF');
    expect(problemaArchivoFirmado(archivo('application/pdf', TAMANO_MAXIMO_FIRMADO + 1))).toContain('10 MB');
  });
});

describe('resumen comercial del cliente', () => {
  const resumen = (cotizado: number, contratado: number): ResumenComercialCliente => ({
    idCliente: 1,
    cotizacionesPorEstado: {},
    importeCotizado: cotizado,
    importeContratado: contratado,
    proyectosPorEstado: {},
    ultimasCotizaciones: [],
    ultimosProyectos: [],
  });

  it('calcula la conversion de lo cotizado en contratado', () => {
    expect(tasaConversion(resumen(8000, 4000))).toBe(50);
    expect(tasaConversion(resumen(3000, 1000))).toBe(33.3);
    expect(tasaConversion(resumen(0, 0))).toBeNull();
  });
});

describe('avance del proyecto', () => {
  const detalle = (contratos: number, estados: string[]): DetalleProyecto =>
    ({
      contratos: Array.from({ length: contratos }, () => ({})),
      eventos: estados.map((estado) => ({ estado })),
    }) as unknown as DetalleProyecto;

  it('marca los pasos cumplidos del flujo comercial', () => {
    expect(pasosProyecto(detalle(0, [])).map((paso) => paso.completo)).toEqual([true, false, false, false]);
    expect(pasosProyecto(detalle(1, ['PROGRAMADO', 'CANCELADO'])).map((paso) => paso.completo)).toEqual([
      true,
      true,
      true,
      false,
    ]);
    expect(pasosProyecto(detalle(1, ['EJECUTADO', 'CANCELADO'])).every((paso) => paso.completo)).toBe(true);
  });
});
