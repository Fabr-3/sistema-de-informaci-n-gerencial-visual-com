import { extremosDeMargen, variacion } from './indicadores';
import { ResultadoProyecto } from './resultado';

describe('indicadores financieros', () => {
  it('calcula la variacion frente al periodo anterior', () => {
    expect(variacion(12000, 10000)).toEqual({ porcentaje: 20, sentido: 'sube' });
    expect(variacion(7500, 10000)).toEqual({ porcentaje: 25, sentido: 'baja' });
    expect(variacion(-500, -1000)).toEqual({ porcentaje: 50, sentido: 'sube' });
    expect(variacion(500, 0)).toBeNull();
  });

  it('separa los proyectos con margen bajo de los de mayor margen', () => {
    const proyecto = (id: number, porcentaje: number | null) =>
      ({ idProyecto: id, margen: { porcentaje } }) as unknown as ResultadoProyecto;
    const { mejores, enAlerta } = extremosDeMargen(
      [proyecto(1, 87.2), proyecto(2, 56.4), proyecto(3, null), proyecto(4, 14.2), proyecto(5, -8)],
      20,
    );

    expect(mejores.map((p) => p.idProyecto)).toEqual([1, 2, 3]);
    expect(enAlerta.map((p) => p.idProyecto)).toEqual([5, 4]);
  });
});
