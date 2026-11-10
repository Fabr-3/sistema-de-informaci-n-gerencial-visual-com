import { CurrencyPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ESTADOS_PROYECTO, tipoVisualProyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionResultados } from '../../../aplicacion/gestion-resultados';
import {
  FiltroResultados,
  ResultadoProyecto,
  textoPorcentaje,
  tipoVisualMargen,
} from '../../../dominio/modelos/resultado';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['proyecto', 'estado', 'contratado', 'ingresos', 'costos', 'margen', 'porCobrar'];

interface CriteriosResultados {
  readonly filtro: FiltroResultados;
  readonly solicitud: SolicitudPagina;
}

/** Ingresos, costos directos, margen y saldo por cobrar de cada proyecto. */
@Component({
  selector: 'app-pagina-resultados',
  imports: [
    CurrencyPipe,
    EtiquetaPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './pagina-resultados.html',
  styleUrl: './resultados.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaResultados {
  private readonly gestion = inject(GestionResultados);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly router = inject(Router);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_PROYECTO;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly tipoVisualProyecto = tipoVisualProyecto;
  protected readonly tipoVisualMargen = tipoVisualMargen;
  protected readonly textoPorcentaje = textoPorcentaje;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly resultado = signal<Pagina<ResultadoProyecto> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosResultados>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });

  constructor() {
    this.busqueda.valueChanges
      .pipe(debounceTime(RETARDO_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((texto) => this.filtrar({ texto }));

    toObservable(this.criterios)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ filtro, solicitud }) =>
          this.gestion.listar(filtro, solicitud).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
      });
  }

  protected filtrar(cambios: Partial<FiltroResultados>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(resultado: ResultadoProyecto): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.FINANZAS, Rutas.RESULTADOS, String(resultado.idProyecto)));
  }
}
