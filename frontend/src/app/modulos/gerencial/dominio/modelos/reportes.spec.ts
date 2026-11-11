import { REPORTES, nombreArchivoReporte } from './reportes';

describe('reportes operativos', () => {
  const reporte = (tipo: string) => REPORTES.find((r) => r.tipo === tipo)!;

  it('nombra el archivo con el periodo o con la fecha de emision', () => {
    expect(nombreArchivoReporte(reporte('EVENTOS'), '2026-09-01', '2026-10-31', '2026-11-10')).toBe(
      'eventos_2026-09-01_2026-10-31.pdf',
    );
    expect(nombreArchivoReporte(reporte('ESTADO_ACTIVOS'), '2026-09-01', '2026-10-31', '2026-11-10')).toBe(
      'estado-activos_2026-11-10.pdf',
    );
  });

  it('ofrece cinco reportes y solo el estado de activos prescinde del periodo', () => {
    expect(REPORTES).toHaveLength(5);
    expect(REPORTES.filter((r) => !r.usaPeriodo).map((r) => r.tipo)).toEqual(['ESTADO_ACTIVOS']);
  });
});
