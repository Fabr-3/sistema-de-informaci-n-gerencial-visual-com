import { PersonalDisponible, asignadoEnEvento, crucesConOtrosEventos } from './personal';

describe('disponibilidad del personal', () => {
  const cruce = (idEvento: number, evento: string) => ({
    idAsignacion: idEvento * 10,
    idPersonal: 3,
    idEvento,
    evento,
    inicio: '2026-12-11T14:00:00',
    fin: '2026-12-11T22:00:00',
  });
  const daniela: PersonalDisponible = {
    id: 3,
    nombreCompleto: 'Daniela Gutiérrez Arce',
    especialidades: ['Iluminación'],
    cruces: [cruce(13, 'Montaje de pantallas e iluminación'), cruce(14, 'Cena de gala de fin de año')],
  };

  it('distingue la asignacion en el mismo evento de los cruces con otros eventos', () => {
    expect(asignadoEnEvento(daniela, 14)).toBe(true);
    expect(asignadoEnEvento(daniela, 16)).toBe(false);
    expect(crucesConOtrosEventos(daniela, 14).map((c) => c.evento)).toEqual(['Montaje de pantallas e iluminación']);
  });
});
