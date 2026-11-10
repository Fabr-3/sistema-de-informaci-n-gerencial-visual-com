import { Permisos } from '../../modulos/accesos/dominio/modelos/permisos';
import { Rutas, rutaAbsoluta } from '../configuracion/rutas';

export interface ElementoMenu {
  readonly etiqueta: string;
  readonly icono: string;
  readonly ruta: string;
  /** Sin permiso: visible para todo usuario autenticado. */
  readonly permiso?: string;
  /** Alternativa a permiso: visible con cualquiera de estos permisos. */
  readonly permisos?: readonly string[];
}

export interface GrupoMenu {
  readonly titulo: string | null;
  readonly elementos: readonly ElementoMenu[];
}

/** Navegacion principal. Cada modulo nuevo agrega aqui su grupo. */
export const MENU_PRINCIPAL: readonly GrupoMenu[] = [
  {
    titulo: null,
    elementos: [{ etiqueta: 'Inicio', icono: 'home', ruta: rutaAbsoluta(Rutas.INICIO) }],
  },
  {
    titulo: 'Comercial y planificación',
    elementos: [
      {
        etiqueta: 'Cotizaciones',
        icono: 'request_quote',
        ruta: rutaAbsoluta(Rutas.COMERCIAL, Rutas.COTIZACIONES),
        permiso: Permisos.COMERCIAL_VER,
      },
      {
        etiqueta: 'Proyectos',
        icono: 'work',
        ruta: rutaAbsoluta(Rutas.COMERCIAL, Rutas.PROYECTOS),
        permiso: Permisos.COMERCIAL_VER,
      },
      {
        etiqueta: 'Cronograma',
        icono: 'calendar_month',
        ruta: rutaAbsoluta(Rutas.COMERCIAL, Rutas.CRONOGRAMA),
        permiso: Permisos.COMERCIAL_VER,
      },
      {
        etiqueta: 'Clientes',
        icono: 'business',
        ruta: rutaAbsoluta(Rutas.COMERCIAL, Rutas.CLIENTES),
        permiso: Permisos.COMERCIAL_VER,
      },
      {
        etiqueta: 'Servicios y lugares',
        icono: 'sell',
        ruta: rutaAbsoluta(Rutas.COMERCIAL, Rutas.CATALOGOS),
        permiso: Permisos.COMERCIAL_VER,
      },
    ],
  },
  {
    titulo: 'Logística',
    elementos: [
      {
        etiqueta: 'Montajes y personal',
        icono: 'local_shipping',
        ruta: rutaAbsoluta(Rutas.LOGISTICA, Rutas.PLANIFICACION),
        permiso: Permisos.LOGISTICA_VER,
      },
      {
        etiqueta: 'Personal técnico',
        icono: 'engineering',
        ruta: rutaAbsoluta(Rutas.LOGISTICA, Rutas.PERSONAL),
        permiso: Permisos.LOGISTICA_VER,
      },
      {
        etiqueta: 'Mis asignaciones',
        icono: 'assignment_ind',
        ruta: rutaAbsoluta(Rutas.LOGISTICA, Rutas.MIS_ASIGNACIONES),
        permiso: Permisos.ASIGNACIONES_VER_PROPIAS,
      },
    ],
  },
  {
    titulo: 'Finanzas',
    elementos: [
      {
        etiqueta: 'Proveedores',
        icono: 'storefront',
        ruta: rutaAbsoluta(Rutas.FINANZAS, Rutas.PROVEEDORES),
        permisos: [Permisos.FINANZAS_VER, Permisos.PROVEEDORES_VER],
      },
    ],
  },
  {
    titulo: 'Activos e inventario',
    elementos: [
      { etiqueta: 'Activos', icono: 'videocam', ruta: rutaAbsoluta(Rutas.ACTIVOS), permiso: Permisos.ACTIVOS_VER },
      {
        etiqueta: 'Inventario por lotes',
        icono: 'inventory_2',
        ruta: rutaAbsoluta(Rutas.ACTIVOS, Rutas.LOTES),
        permiso: Permisos.ACTIVOS_VER,
      },
      {
        etiqueta: 'Catálogos',
        icono: 'category',
        ruta: rutaAbsoluta(Rutas.ACTIVOS, Rutas.CATALOGOS),
        permiso: Permisos.ACTIVOS_VER,
      },
    ],
  },
  {
    titulo: 'Administración de accesos',
    elementos: [
      {
        etiqueta: 'Usuarios',
        icono: 'group',
        ruta: rutaAbsoluta(Rutas.ACCESOS, Rutas.USUARIOS),
        permiso: Permisos.USUARIOS_VER,
      },
      {
        etiqueta: 'Roles y permisos',
        icono: 'admin_panel_settings',
        ruta: rutaAbsoluta(Rutas.ACCESOS, Rutas.ROLES),
        permiso: Permisos.ROLES_VER,
      },
      {
        etiqueta: 'Auditoría',
        icono: 'manage_search',
        ruta: rutaAbsoluta(Rutas.ACCESOS, Rutas.AUDITORIA),
        permiso: Permisos.AUDITORIA_VER,
      },
      {
        etiqueta: 'Respaldos',
        icono: 'backup',
        ruta: rutaAbsoluta(Rutas.ACCESOS, Rutas.RESPALDOS),
        permiso: Permisos.RESPALDOS_GESTIONAR,
      },
    ],
  },
];

function elementoPermitido(elemento: ElementoMenu, tienePermiso: (permiso: string) => boolean): boolean {
  if (elemento.permisos) {
    return elemento.permisos.some(tienePermiso);
  }
  return !elemento.permiso || tienePermiso(elemento.permiso);
}

/** Filtra el menu segun los permisos del usuario y descarta los grupos vacios. */
export function menuPermitido(tienePermiso: (permiso: string) => boolean): GrupoMenu[] {
  return MENU_PRINCIPAL.map((grupo) => ({
    ...grupo,
    elementos: grupo.elementos.filter((elemento) => elementoPermitido(elemento, tienePermiso)),
  })).filter((grupo) => grupo.elementos.length > 0);
}
