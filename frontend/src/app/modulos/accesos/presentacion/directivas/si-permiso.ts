import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { ServicioSesion } from '../../aplicacion/servicio-sesion';

/**
 * Muestra el contenido solo si el usuario tiene el permiso indicado. Es una
 * ayuda visual: la autorizacion real la aplica siempre el backend.
 *
 * Uso: <button *siPermiso="permisos.USUARIOS_GESTIONAR">...</button>
 */
@Directive({ selector: '[siPermiso]' })
export class SiPermiso {
  readonly siPermiso = input.required<string>();

  private readonly sesion = inject(ServicioSesion);
  private readonly plantilla = inject(TemplateRef<unknown>);
  private readonly contenedor = inject(ViewContainerRef);
  private visible = false;

  constructor() {
    effect(() => {
      const permitido = this.sesion.tienePermiso(this.siPermiso());
      if (permitido && !this.visible) {
        this.contenedor.createEmbeddedView(this.plantilla);
      } else if (!permitido && this.visible) {
        this.contenedor.clear();
      }
      this.visible = permitido;
    });
  }
}
