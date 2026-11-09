import { Permisos } from '../../modulos/accesos/dominio/modelos/permisos';
import { Rutas, rutaAbsoluta } from '../configuracion/rutas';

export interface ElementoMenu {
  readonly etiqueta: string;
  readonly icono: string;
  readonly ruta: string;
  /** Sin permiso: visible para todo usuario autenticado. */
  readonly permiso?: string;
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

/** Filtra el menu segun los permisos del usuario y descarta los grupos vacios. */
export function menuPermitido(tienePermiso: (permiso: string) => boolean): GrupoMenu[] {
  return MENU_PRINCIPAL.map((grupo) => ({
    ...grupo,
    elementos: grupo.elementos.filter((elemento) => !elemento.permiso || tienePermiso(elemento.permiso)),
  })).filter((grupo) => grupo.elementos.length > 0);
}
