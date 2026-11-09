import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, filter, forkJoin, switchMap } from 'rxjs';

import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { Bodega, Categoria, Fabricante } from '../../../dominio/modelos/catalogos';
import { ModeloActivo, TIPOS_CONTROL } from '../../../dominio/modelos/modelo-activo';
import { DialogoBodega } from './dialogo-bodega';
import { DatosDialogoModelo, DialogoModelo } from './dialogo-modelo';

/** Catalogo simple identificado solo por su nombre. */
type TipoCatalogoSimple = 'categoria' | 'fabricante';

const LONGITUD_NOMBRE_CATALOGO = 100;
const TITULOS_CATALOGO: Readonly<Record<TipoCatalogoSimple, { nuevo: string; editar: string }>> = {
  categoria: { nuevo: 'Nueva categoría', editar: 'Editar categoría' },
  fabricante: { nuevo: 'Nuevo fabricante', editar: 'Editar fabricante' },
};

/** Modelos de equipo, categorias, fabricantes y bodegas del inventario. */
@Component({
  selector: 'app-pagina-catalogos',
  imports: [NgTemplateOutlet, EtiquetaPipe, RouterLink, MatButtonModule, MatIconModule, MatTableModule, MatTabsModule, MatTooltipModule, SiPermiso],
  templateUrl: './pagina-catalogos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaCatalogos {
  private readonly catalogos = inject(CatalogosActivos);
  private readonly sesion = inject(ServicioSesion);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly permisos = Permisos;
  protected readonly tiposControl = TIPOS_CONTROL;
  protected readonly rutaActivos = rutaAbsoluta(Rutas.ACTIVOS);
  protected readonly rutaLotes = rutaAbsoluta(Rutas.ACTIVOS, Rutas.LOTES);
  protected readonly modelos = signal<ModeloActivo[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly fabricantes = signal<Fabricante[]>([]);
  protected readonly bodegas = signal<Bodega[]>([]);
  private readonly version = signal(0);

  protected readonly puedeGestionar = computed(() => this.sesion.tienePermiso(Permisos.ACTIVOS_GESTIONAR));
  protected readonly columnasModelos = computed(() => {
    const base = ['fabricante', 'nombre', 'categoria', 'tipoControl', 'prefijo', 'horasMantenimiento', 'inventario'];
    return this.puedeGestionar() ? [...base, 'acciones'] : base;
  });
  protected readonly columnasSimples = computed(() => (this.puedeGestionar() ? ['nombre', 'acciones'] : ['nombre']));
  protected readonly columnasBodegas = computed(() => {
    const base = ['nombre', 'direccion', 'estado'];
    return this.puedeGestionar() ? [...base, 'acciones'] : base;
  });

  constructor() {
    toObservable(this.version)
      .pipe(
        switchMap(() =>
          forkJoin({
            modelos: this.catalogos.listarModelos(),
            categorias: this.catalogos.listarCategorias(),
            fabricantes: this.catalogos.listarFabricantes(),
            bodegas: this.catalogos.listarBodegas(),
          }),
        ),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: ({ modelos, categorias, fabricantes, bodegas }) => {
          this.modelos.set(modelos);
          this.categorias.set(categorias);
          this.fabricantes.set(fabricantes);
          this.bodegas.set(bodegas);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }

  protected editarModelo(modelo?: ModeloActivo): void {
    this.alCerrar(
      this.dialogos
        .open<DialogoModelo, DatosDialogoModelo, ModeloActivo>(DialogoModelo, {
          data: { modelo, fabricantes: this.fabricantes(), categorias: this.categorias() },
          width: '680px',
        })
        .afterClosed(),
      modelo ? 'Modelo actualizado' : 'Modelo registrado',
    );
  }

  protected editarBodega(bodega?: Bodega): void {
    this.alCerrar(
      this.dialogos.open(DialogoBodega, { data: bodega ?? null, width: '520px' }).afterClosed(),
      bodega ? 'Bodega actualizada' : 'Bodega registrada',
    );
  }

  protected editarCatalogo(tipo: TipoCatalogoSimple, elemento?: Categoria | Fabricante): void {
    const titulos = TITULOS_CATALOGO[tipo];
    const guardado = this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: elemento ? titulos.editar : titulos.nuevo,
          etiqueta: 'Nombre',
          textoConfirmar: 'Guardar',
          valorInicial: elemento?.nombre,
          longitudMaxima: LONGITUD_NOMBRE_CATALOGO,
        },
        width: '440px',
      })
      .afterClosed()
      .pipe(
        filter((nombre): nombre is string => !!nombre),
        switchMap((nombre) =>
          tipo === 'categoria'
            ? this.catalogos.guardarCategoria(elemento?.id ?? null, nombre)
            : this.catalogos.guardarFabricante(elemento?.id ?? null, nombre),
        ),
      );
    this.alCerrar(guardado, 'Catálogo actualizado');
  }

  /** Recarga los catalogos cuando la operacion devuelve un resultado. */
  private alCerrar(resultado: Observable<unknown>, mensaje: string): void {
    resultado.pipe(filter(Boolean)).subscribe({
      next: () => {
        this.notificaciones.exito(mensaje);
        this.version.update((valor) => valor + 1);
      },
      error: (error: unknown) => this.notificaciones.error(error),
    });
  }
}
