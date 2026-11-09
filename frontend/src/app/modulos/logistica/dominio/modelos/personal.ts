import { ElementoCatalogo } from '../../../comercial/dominio/modelos/catalogos';
import { Cruce, EventoLogistico } from './planificacion';

/** Cuenta del sistema vinculada (solo tecnicos y sub-contratados). */
export interface UsuarioVinculado {
  readonly id: number;
  readonly nombreUsuario: string;
  readonly nombreCompleto: string;
}

export interface PersonalTecnico {
  readonly id: number;
  readonly nombres: string;
  readonly apellidos: string;
  readonly nombreCompleto: string;
  readonly telefono: string | null;
  readonly documento: { readonly tipo: ElementoCatalogo; readonly numero: string };
  readonly especialidades: readonly ElementoCatalogo[];
  readonly usuario: UsuarioVinculado | null;
  readonly activo: boolean;
}

export interface DatosPersonal {
  readonly nombres: string;
  readonly apellidos: string;
  readonly telefono: string | null;
  readonly idTipoDocumento: number;
  readonly numeroDocumento: string;
  readonly idsEspecialidades: readonly number[];
  readonly idUsuario: number | null;
  readonly activo: boolean;
}

export interface FiltroPersonal {
  readonly texto?: string;
  readonly activo?: boolean;
  readonly idEspecialidad?: number;
}

/** Personal activo con lo que ya tiene asignado en el horario consultado. */
export interface PersonalDisponible {
  readonly id: number;
  readonly nombreCompleto: string;
  readonly especialidades: readonly string[];
  readonly cruces: readonly Cruce[];
}

export interface CatalogosLogistica {
  readonly rolesEvento: readonly ElementoCatalogo[];
  readonly especialidades: readonly ElementoCatalogo[];
  readonly tiposDocumento: readonly ElementoCatalogo[];
  /** Cuentas de tecnicos y sub-contratados aun sin vincular. */
  readonly usuarios: readonly UsuarioVinculado[];
}

export interface AsignacionPropia {
  readonly id: number;
  readonly evento: EventoLogistico;
  readonly rol: string;
  readonly inicio: string;
  readonly fin: string;
  readonly detallesTransporte: string | null;
  readonly cruces: readonly Cruce[];
}

export interface MisAsignaciones {
  readonly vinculado: boolean;
  readonly nombre: string | null;
  readonly asignaciones: readonly AsignacionPropia[];
}
