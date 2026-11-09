import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, forkJoin, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { SiPermiso } from '../../../../accesos/presentacion/directivas/si-permiso';
import { CatalogosActivos } from '../../../aplicacion/catalogos-activos';
import { GestionActivos } from '../../../aplicacion/gestion-activos';
import {
  Activo,
  ESTADOS_OPERATIVOS,
  FiltroActivos,
  NIVELES_ALERTA,
  tipoVisualAlerta,
  tipoVisualEstado,
} from '../../../dominio/modelos/activo';
import { Bodega, Categoria } from '../../../dominio/modelos/catalogos';
import { ModeloActivo } from '../../../dominio/modelos/modelo-activo';
import { DatosDialogoActivo, DialogoActivo } from './dialogo-activo/dialogo-activo';

const RETARDO_BUSQUEDA_MS = 300;
/** Parametro de consulta para abrir el listado filtrado por modelo (desde Catalogos). */
const PARAMETRO_MODELO = 'idModelo';
const COLUMNAS = ['codigoInventario', 'modelo', 'categoria', 'bodega', 'estado', 'horas', 'mantenimiento'];

interface CriteriosActivos {
  readonly filtro: FiltroActivos;
  readonly solicitud: SolicitudPagina;
}

/** Listado de activos por unidad con filtros y alertas de mantenimiento preventivo. */
@Component({
  selector: 'app-pagina-activos',
  imports: [
    DecimalPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
    SiPermiso,
    EtiquetaPipe,
  ],
  templateUrl: './pagina-activos.html',
  styleUrl: './pagina-activos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaActivos {
  private readonly gestion = inject(GestionActivos);
  private readonly catalogos = inject(CatalogosActivos);
  private readonly dialogos = inject(MatDialog);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly permisos = Permisos;
  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_OPERATIVOS;
  protected readonly niveles = NIVELES_ALERTA;
  protected readonly tipoVisualEstado = tipoVisualEstado;
  protected readonly tipoVisualAlerta = tipoVisualAlerta;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly busqueda = new FormControl('', { nonNullable: true });

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly bodegas = signal<Bodega[]>([]);
  protected readonly modelosUnidad = signal<ModeloActivo[]>([]);
  protected readonly alertas = signal<Activo[]>([]);
  protected readonly resultado = signal<Pagina<Activo> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosActivos>({
    filtro: { idModelo: Number(inject(ActivatedRoute).snapshot.queryParamMap.get(PARAMETRO_MODELO)) || undefined },
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  private readonly version = signal(0);

  protected readonly alertasVencidas = computed(
    () => this.alertas().filter((activo) => activo.nivelAlerta === 'VENCIDO').length,
  );

  constructor() {
    forkJoin({
      categorias: this.catalogos.listarCategorias(),
      bodegas: this.catalogos.listarBodegas(),
      modelos: this.catalogos.listarModelos('UNIDAD'),
    })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: ({ categorias, bodegas, modelos }) => {
          this.categorias.set(categorias);
          this.bodegas.set(bodegas);
          this.modelosUnidad.set(modelos);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });

    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(computed(() => ({ criterios: this.criterios(), version: this.version() })))
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ criterios }) =>
          forkJoin({
            pagina: this.gestion.listar(criterios.filtro, criterios.solicitud),
            alertas: this.gestion.listarAlertasMantenimiento(),
          }).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((respuesta) => {
        this.resultado.set(respuesta?.pagina ?? null);
        this.alertas.set(respuesta?.alertas ?? []);
        this.cargando.set(false);
      });
  }

  protected filtrar(cambios: Partial<FiltroActivos>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(activo: Activo): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.ACTIVOS, String(activo.id)));
  }

  protected crear(): void {
    this.dialogos
      .open<DialogoActivo, DatosDialogoActivo, Activo[]>(DialogoActivo, {
        data: {
          modelos: this.modelosUnidad(),
          bodegas: this.bodegas(),
          idModelo: this.criterios().filtro.idModelo,
        },
        width: '640px',
      })
      .afterClosed()
      .pipe(filter((activos): activos is Activo[] => !!activos?.length))
      .subscribe((activos) => {
        this.notificaciones.exito(mensajeRegistro(activos));
        this.version.update((valor) => valor + 1);
      });
  }
}

/** Resumen de los codigos registrados (primero y ultimo si son varios). */
function mensajeRegistro(activos: readonly Activo[]): string {
  if (activos.length === 1) {
    return `Activo ${activos[0].codigoInventario} registrado`;
  }
  return `${activos.length} activos registrados: ${activos[0].codigoInventario} a ${activos[activos.length - 1].codigoInventario}`;
}
