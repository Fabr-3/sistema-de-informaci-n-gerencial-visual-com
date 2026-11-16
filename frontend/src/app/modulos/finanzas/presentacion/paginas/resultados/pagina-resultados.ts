import { CurrencyPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { busquedaRemota } from '../../../../../compartido/utilidades/busqueda-remota';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionComercial } from '../../../../comercial/aplicacion/gestion-comercial';
import { ResumenCliente } from '../../../../comercial/dominio/modelos/cliente';
import { ESTADOS_PROYECTO, tipoVisualProyecto } from '../../../../comercial/dominio/modelos/proyecto';
import { GestionResultados } from '../../../aplicacion/gestion-resultados';
import {
  FiltroResultados,
  ORDENES_RESULTADOS,
  ResultadoProyecto,
  rangoValido,
  textoPorcentaje,
  tipoVisualMargen,
} from '../../../dominio/modelos/resultado';

const RETARDO_BUSQUEDA_MS = 300;
const CLIENTES_SUGERIDOS = 10;
const COLUMNAS = ['proyecto', 'estado', 'contratado', 'ingresos', 'costos', 'margen', 'porCobrar'];

interface CriteriosResultados {
  readonly filtro: FiltroResultados;
  readonly solicitud: SolicitudPagina;
}

/** Ingresos, costos directos, margen y saldo por cobrar de cada proyecto; ordenable por rentabilidad. */
@Component({
  selector: 'app-pagina-resultados',
  imports: [
    CurrencyPipe,
    EtiquetaPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatAutocompleteModule,
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
  private readonly comercial = inject(GestionComercial);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly estados = ESTADOS_PROYECTO;
  protected readonly ordenes = ORDENES_RESULTADOS;
  protected readonly ordenOriginal = (): number => 0;
  protected readonly tipoVisualProyecto = tipoVisualProyecto;
  protected readonly tipoVisualMargen = tipoVisualMargen;
  protected readonly textoPorcentaje = textoPorcentaje;
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly cliente = new FormControl<ResumenCliente | string | null>(null);
  protected readonly desde = new FormControl('', { nonNullable: true });
  protected readonly hasta = new FormControl('', { nonNullable: true });
  protected readonly clientesSugeridos = signal<ResumenCliente[]>([]);
  protected readonly mostrarCliente = (valor: ResumenCliente | string | null): string =>
    typeof valor === 'string' ? valor : (valor?.razonSocial ?? '');
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
    busquedaRemota(this.cliente, (texto) =>
      this.comercial
        .listarClientes({ texto }, { pagina: 0, tamano: CLIENTES_SUGERIDOS })
        .pipe(map((pagina): ResumenCliente[] => [...pagina.contenido])),
    )
      .pipe(takeUntilDestroyed())
      .subscribe((clientes) => this.clientesSugeridos.set(clientes));
    this.cliente.valueChanges
      .pipe(
        filter((valor) => valor === null || valor === ''),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.filtrar({ idCliente: undefined }));

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

  protected elegirCliente(evento: MatAutocompleteSelectedEvent): void {
    this.filtrar({ idCliente: (evento.option.value as ResumenCliente).id });
  }

  /** Solo se aplica un rango completo y valido; uno invertido se advierte en pantalla. */
  protected aplicarPeriodo(): void {
    const desde = this.desde.value || undefined;
    const hasta = this.hasta.value || undefined;
    if (rangoValido(desde, hasta)) {
      this.filtrar({ desde, hasta });
    }
  }

  protected periodoInvalido(): boolean {
    return !rangoValido(this.desde.value || undefined, this.hasta.value || undefined);
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected abrir(resultado: ResultadoProyecto): void {
    void this.router.navigateByUrl(rutaAbsoluta(Rutas.FINANZAS, Rutas.RESULTADOS, String(resultado.idProyecto)));
  }
}
