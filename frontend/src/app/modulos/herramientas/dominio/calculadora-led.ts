/**
 * Calculadora de pantallas LED (herramienta de consulta, no guarda nada).
 * A partir del tamano (ancho y alto, o solo el area), el pixel pitch y el
 * tamano del modulo, estima la resolucion, los modulos necesarios y el costo
 * de alquiler por metro cuadrado y dia.
 */

/** Proporcion supuesta cuando solo se conoce el area (pantalla panoramica 16:9). */
export const PROPORCION_PANORAMICA = 16 / 9;
const MILIMETROS_POR_METRO = 1000;
/** Regla practica: la distancia minima de vision en metros es aproximadamente el pitch en milimetros. */
const METROS_DE_VISION_POR_MM_DE_PITCH = 1;

export type ModoMedida = 'dimensiones' | 'area';

export interface EntradaCalculadoraLed {
  readonly modo: ModoMedida;
  /** Metros (modo dimensiones). */
  readonly ancho: number | null;
  readonly alto: number | null;
  /** Metros cuadrados (modo area). */
  readonly area: number | null;
  readonly pitchMm: number;
  readonly moduloAnchoMm: number;
  readonly moduloAltoMm: number;
  /** Bolivianos por metro cuadrado y dia. */
  readonly precioM2Dia: number;
  readonly dias: number;
}

export interface ResultadoCalculadoraLed {
  /** Medidas pedidas (en modo area, estimadas con proporcion 16:9). */
  readonly anchoPedido: number;
  readonly altoPedido: number;
  readonly areaPedida: number;
  /** La pantalla se arma con modulos completos: medidas reales. */
  readonly columnas: number;
  readonly filas: number;
  readonly modulos: number;
  readonly anchoArmado: number;
  readonly altoArmado: number;
  readonly areaArmada: number;
  readonly pixelesAncho: number;
  readonly pixelesAlto: number;
  readonly pixelesTotales: number;
  readonly distanciaMinimaVision: number;
  readonly costoPorDia: number;
  readonly costoTotal: number;
}

const positivo = (valor: number | null | undefined): valor is number =>
  typeof valor === 'number' && Number.isFinite(valor) && valor > 0;

/** Medidas en metros segun el modo; null si faltan datos. */
function medidas(entrada: EntradaCalculadoraLed): { ancho: number; alto: number } | null {
  if (entrada.modo === 'dimensiones') {
    return positivo(entrada.ancho) && positivo(entrada.alto) ? { ancho: entrada.ancho, alto: entrada.alto } : null;
  }
  if (!positivo(entrada.area)) {
    return null;
  }
  const ancho = Math.sqrt(entrada.area * PROPORCION_PANORAMICA);
  return { ancho, alto: entrada.area / ancho };
}

/** Calcula la pantalla; null mientras falte algun dato o alguno no sea positivo. */
export function calcularPantallaLed(entrada: EntradaCalculadoraLed): ResultadoCalculadoraLed | null {
  const tamano = medidas(entrada);
  if (
    !tamano ||
    !positivo(entrada.pitchMm) ||
    !positivo(entrada.moduloAnchoMm) ||
    !positivo(entrada.moduloAltoMm) ||
    !(entrada.precioM2Dia >= 0) ||
    !positivo(entrada.dias)
  ) {
    return null;
  }
  const columnas = Math.ceil((tamano.ancho * MILIMETROS_POR_METRO) / entrada.moduloAnchoMm - 1e-9);
  const filas = Math.ceil((tamano.alto * MILIMETROS_POR_METRO) / entrada.moduloAltoMm - 1e-9);
  const anchoArmado = (columnas * entrada.moduloAnchoMm) / MILIMETROS_POR_METRO;
  const altoArmado = (filas * entrada.moduloAltoMm) / MILIMETROS_POR_METRO;
  const areaArmada = anchoArmado * altoArmado;
  const pixelesAncho = Math.round((anchoArmado * MILIMETROS_POR_METRO) / entrada.pitchMm);
  const pixelesAlto = Math.round((altoArmado * MILIMETROS_POR_METRO) / entrada.pitchMm);
  const costoPorDia = areaArmada * entrada.precioM2Dia;
  return {
    anchoPedido: tamano.ancho,
    altoPedido: tamano.alto,
    areaPedida: tamano.ancho * tamano.alto,
    columnas,
    filas,
    modulos: columnas * filas,
    anchoArmado,
    altoArmado,
    areaArmada,
    pixelesAncho,
    pixelesAlto,
    pixelesTotales: pixelesAncho * pixelesAlto,
    distanciaMinimaVision: entrada.pitchMm * METROS_DE_VISION_POR_MM_DE_PITCH,
    costoPorDia,
    costoTotal: costoPorDia * entrada.dias,
  };
}

/** Pitch habituales del inventario (mm), como atajo en el formulario. */
export const PITCHES_HABITUALES = [2.6, 2.9, 3.9, 4.8, 5.9] as const;

/** Tamanos de modulo o gabinete habituales (mm). */
export const MODULOS_HABITUALES = [
  { etiqueta: 'Gabinete 500 × 500 mm', ancho: 500, alto: 500 },
  { etiqueta: 'Gabinete 500 × 1000 mm', ancho: 500, alto: 1000 },
  { etiqueta: 'Módulo 250 × 250 mm', ancho: 250, alto: 250 },
] as const;
