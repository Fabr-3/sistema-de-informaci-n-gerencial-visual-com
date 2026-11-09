import { EtiquetaPipe } from './etiqueta.pipe';

describe('EtiquetaPipe', () => {
  const pipe = new EtiquetaPipe();
  const etiquetas = { DISPONIBLE: 'Disponible' };

  it('traduce codigos conocidos y conserva los desconocidos', () => {
    expect(pipe.transform('DISPONIBLE', etiquetas)).toBe('Disponible');
    expect(pipe.transform('OTRO', etiquetas)).toBe('OTRO');
  });

  it('muestra un guion si no hay valor', () => {
    expect(pipe.transform(null, etiquetas)).toBe('—');
    expect(pipe.transform(undefined, etiquetas)).toBe('—');
  });
});
