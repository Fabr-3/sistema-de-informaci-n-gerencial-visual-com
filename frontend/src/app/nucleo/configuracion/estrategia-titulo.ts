import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

const NOMBRE_SISTEMA = 'Visual COM';

/** Titulo de la pestana: "<Pagina> | Visual COM". */
@Injectable()
export class EstrategiaTitulo extends TitleStrategy {
  private readonly titulo = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const tituloPagina = this.buildTitle(estado);
    this.titulo.setTitle(tituloPagina ? `${tituloPagina} | ${NOMBRE_SISTEMA}` : NOMBRE_SISTEMA);
  }
}
