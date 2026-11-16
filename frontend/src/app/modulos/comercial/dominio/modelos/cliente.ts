import { ElementoCatalogo } from './catalogos';

export interface DocumentoCliente {
  readonly id: number | null;
  readonly tipo: ElementoCatalogo;
  readonly numero: string;
}

export interface ContactoCliente {
  readonly id: number | null;
  readonly tipo: ElementoCatalogo;
  readonly valor: string;
  readonly referencia: string | null;
}

export interface Cliente {
  readonly id: number;
  readonly tipo: ElementoCatalogo;
  readonly razonSocial: string;
  readonly direccion: string | null;
  readonly documentos: readonly DocumentoCliente[];
  readonly contactos: readonly ContactoCliente[];
  readonly fechaCreacion: string;
}

/** Cliente resumido en cotizaciones y proyectos. */
export interface ResumenCliente {
  readonly id: number;
  readonly razonSocial: string;
}

export interface DatosCliente {
  readonly idTipoCliente: number;
  readonly razonSocial: string;
  readonly direccion: string | null;
  readonly documentos: readonly { id: number | null; idTipoDocumento: number; numero: string }[];
  readonly contactos: readonly { id: number | null; idTipoContacto: number; valor: string; referencia: string | null }[];
}

export interface FiltroClientes {
  readonly texto?: string;
  readonly idTipoCliente?: number;
}

/** NIT si existe; si no, el primer documento ("NIT 1020304025"). */
export function documentoPrincipal(cliente: Cliente): string {
  const documento =
    cliente.documentos.find((candidato) => candidato.tipo.nombre === 'NIT') ?? cliente.documentos[0];
  return documento ? `${documento.tipo.nombre} ${documento.numero}` : '—';
}

/** Cotizacion (por su numero) o proyecto (por su nombre) reciente del cliente. */
export interface RecienteCliente {
  readonly id: number;
  readonly referencia: string;
  readonly estado: string;
  readonly fecha: string | null;
}

/** Historia comercial del cliente: contratado = cotizaciones aceptadas. */
export interface ResumenComercialCliente {
  readonly idCliente: number;
  readonly cotizacionesPorEstado: Readonly<Record<string, number>>;
  readonly importeCotizado: number;
  readonly importeContratado: number;
  readonly proyectosPorEstado: Readonly<Record<string, number>>;
  readonly ultimasCotizaciones: readonly RecienteCliente[];
  readonly ultimosProyectos: readonly RecienteCliente[];
}

/** Porcentaje de lo cotizado que se llego a contratar; nulo si no hubo cotizaciones. */
export function tasaConversion(resumen: ResumenComercialCliente): number | null {
  return resumen.importeCotizado > 0
    ? Math.round((resumen.importeContratado / resumen.importeCotizado) * 1000) / 10
    : null;
}
