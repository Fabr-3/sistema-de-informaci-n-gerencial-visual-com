import { Activo } from '../dominio/modelos/activo';
import { ModeloActivo } from '../dominio/modelos/modelo-activo';

/** Datos de prueba reutilizables del modulo de activos. */
export function activo(cambios: Partial<Activo> = {}): Activo {
  return {
    id: 1,
    codigoInventario: 'LED-0001',
    numeroSerie: null,
    idModelo: 1,
    modelo: 'Absen PL3.9',
    idCategoria: 1,
    categoria: 'Pantalla LED',
    idBodega: 1,
    bodega: 'Bodega 1',
    estado: 'DISPONIBLE',
    fechaAdquisicion: null,
    costoAdquisicion: null,
    observaciones: null,
    motivoBaja: null,
    horasTotales: 0,
    horasDesdeMantenimiento: 0,
    horasMantenimiento: null,
    fechaUltimoMantenimiento: null,
    nivelAlerta: 'SIN_PLAN',
    especificaciones: [],
    fechaCreacion: '2026-11-09T10:00:00',
    ...cambios,
  };
}

export function modelo(cambios: Partial<ModeloActivo> = {}): ModeloActivo {
  return {
    id: 1,
    nombre: 'PL3.9',
    fabricante: { id: 1, nombre: 'Absen' },
    categoria: { id: 1, nombre: 'Pantalla LED' },
    tipoControl: 'UNIDAD',
    horasMantenimiento: 500,
    prefijoCodigo: 'ABS',
    especificaciones: [],
    cantidadInventario: 0,
    ...cambios,
  };
}
