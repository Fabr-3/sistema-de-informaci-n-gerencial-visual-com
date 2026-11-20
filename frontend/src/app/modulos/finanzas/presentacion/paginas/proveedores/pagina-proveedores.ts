import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ListaProveedores } from '../../componentes/lista-proveedores';

/** Proveedores de compras, alquileres y servicios externos. */
@Component({
  selector: 'app-pagina-proveedores',
  imports: [ListaProveedores],
  templateUrl: './pagina-proveedores.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaProveedores {}
