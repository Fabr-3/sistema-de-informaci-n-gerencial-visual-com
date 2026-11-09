const UNIDADES = ['B', 'KB', 'MB', 'GB'] as const;
const BYTES_POR_UNIDAD = 1024;

/** Tamano legible de un archivo (por ejemplo, 157,1 KB). */
export function formatearTamano(bytes: number): string {
  let valor = bytes;
  let unidad = 0;
  while (valor >= BYTES_POR_UNIDAD && unidad < UNIDADES.length - 1) {
    valor /= BYTES_POR_UNIDAD;
    unidad++;
  }
  const decimales = unidad === 0 ? 0 : 1;
  return `${valor.toLocaleString('es-BO', { maximumFractionDigits: decimales })} ${UNIDADES[unidad]}`;
}

/** Ofrece al navegador un archivo para descargar. */
export function descargarArchivo(contenido: Blob, nombreArchivo: string): void {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(url);
}
