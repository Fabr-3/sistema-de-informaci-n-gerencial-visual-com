import { DatePipe, JsonPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { catchError, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { aIsoLocal, finDelDia, inicioDelDia } from '../../../../../compartido/utilidades/fechas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ConsultaAuditoria } from '../../../aplicacion/consulta-auditoria';
import {
  ACCIONES_AUDITORIA,
  AccionAuditoria,
  FiltroAuditoria,
  RegistroAuditoria,
  esAccionCritica,
  etiquetaAccion,
} from '../../../dominio/modelos/auditoria';

const COLUMNAS = ['fecha', 'usuario', 'accion', 'entidad', 'detalle', 'direccionIp', 'expandir'];

interface CriteriosAuditoria {
  readonly filtro: FiltroAuditoria;
  readonly solicitud: SolicitudPagina;
}

/** Consulta del registro de auditoria (solo lectura: los registros son inmutables). */
@Component({
  selector: 'app-pagina-auditoria',
  imports: [
    DatePipe,
    JsonPipe,
    KeyValuePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './pagina-auditoria.html',
  styleUrl: './pagina-auditoria.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaAuditoria {
  private readonly consulta = inject(ConsultaAuditoria);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly acciones = ACCIONES_AUDITORIA;
  protected readonly esAccionCritica = esAccionCritica;
  protected readonly etiquetaAccion = etiquetaAccion;
  protected readonly ordenOriginal = (): number => 0;

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    desde: [null as Date | null],
    hasta: [null as Date | null],
    accion: [null as AccionAuditoria | null],
    entidad: [''],
  });
  protected readonly criterios = signal<CriteriosAuditoria>({
    filtro: {},
    solicitud: { pagina: 0, tamano: TAMANO_PAGINA_POR_DEFECTO },
  });
  protected readonly resultado = signal<Pagina<RegistroAuditoria> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly expandido = signal<number | null>(null);

  constructor() {
    toObservable(this.criterios)
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ filtro, solicitud }) =>
          this.consulta.consultar(filtro, solicitud).pipe(
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

  protected buscar(): void {
    const { desde, hasta, accion, entidad } = this.filtros.getRawValue();
    this.criterios.update(({ solicitud }) => ({
      filtro: {
        desde: desde ? aIsoLocal(inicioDelDia(desde)) : undefined,
        hasta: hasta ? aIsoLocal(finDelDia(hasta)) : undefined,
        accion: accion ?? undefined,
        entidad,
      },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected limpiar(): void {
    this.filtros.reset();
    this.buscar();
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  protected alternarDetalle(registro: RegistroAuditoria): void {
    this.expandido.update((actual) => (actual === registro.id ? null : registro.id));
  }

  protected tieneValores(registro: RegistroAuditoria): boolean {
    return registro.valoresAnteriores !== null || registro.valoresNuevos !== null;
  }
}
