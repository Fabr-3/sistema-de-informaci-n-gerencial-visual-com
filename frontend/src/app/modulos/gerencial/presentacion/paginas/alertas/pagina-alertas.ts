import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, filter, of, switchMap, tap } from 'rxjs';

import {
  DatosEntradaTexto,
  DialogoEntradaTexto,
} from '../../../../../compartido/componentes/dialogo-entrada-texto/dialogo-entrada-texto';
import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionGerencial } from '../../../aplicacion/gestion-gerencial';
import {
  Alerta,
  ESTADOS_ALERTA,
  EstadoAlerta,
  SEVERIDADES,
  Severidad,
  TIPOS_ALERTA,
  TipoAlerta,
  contarPorSeveridad,
  filtrarAlertas,
  iconoSeveridad,
  tipoVisualSeveridad,
} from '../../../dominio/modelos/alertas';

const LONGITUD_COMENTARIO = 1000;
const COLUMNAS = ['severidad', 'activo', 'tipo', 'detalle', 'atencion', 'acciones'];

/**
 * Alertas de activos, reservas y consumibles. Se sincronizan con la situacion
 * actual al consultarlas; el gerente marca como atendidas las pendientes.
 */
@Component({
  selector: 'app-pagina-alertas',
  imports: [
    DatePipe,
    EtiquetaPipe,
    FormsModule,
    KeyValuePipe,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-alertas.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaAlertas {
  private readonly gestion = inject(GestionGerencial);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);

  protected readonly severidades = SEVERIDADES;
  protected readonly tipos = TIPOS_ALERTA;
  protected readonly estados = ESTADOS_ALERTA;
  protected readonly columnas = COLUMNAS;
  protected readonly tipoVisualSeveridad = tipoVisualSeveridad;
  protected readonly iconoSeveridad = iconoSeveridad;
  protected readonly ordenOriginal = (): number => 0;

  protected readonly estado = signal<EstadoAlerta>('PENDIENTE');
  protected readonly severidad = signal<Severidad | null>(null);
  protected readonly tipo = signal<TipoAlerta | null>(null);
  private readonly version = signal(0);
  protected readonly cargando = signal(true);
  /** null si fallo la consulta. */
  protected readonly alertas = signal<Alerta[] | null>([]);

  protected readonly filtradas = computed(() => filtrarAlertas(this.alertas() ?? [], this.severidad(), this.tipo()));
  protected readonly conteo = computed(() => contarPorSeveridad(this.alertas() ?? []));

  constructor() {
    toObservable(computed(() => ({ estado: this.estado(), version: this.version() })))
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(({ estado }) =>
          this.gestion.alertas(estado).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((alertas) => {
        this.alertas.set(alertas);
        this.cargando.set(false);
      });
  }

  protected rutaActivo(alerta: Alerta): string {
    return rutaAbsoluta(Rutas.ACTIVOS, String(alerta.idActivo));
  }

  protected atender(alerta: Alerta): void {
    this.dialogos
      .open<DialogoEntradaTexto, DatosEntradaTexto, string>(DialogoEntradaTexto, {
        data: {
          titulo: 'Marcar como atendida',
          etiqueta: 'Qué se hizo',
          indicacion: `${TIPOS_ALERTA[alerta.tipo]}: ${alerta.detalle}. No volverá a aparecer como pendiente mientras la situación persista.`,
          textoConfirmar: 'Marcar como atendida',
          longitudMaxima: LONGITUD_COMENTARIO,
          multilinea: true,
        },
        width: '520px',
      })
      .afterClosed()
      .pipe(
        filter((comentario): comentario is string => !!comentario),
        switchMap((comentario) => this.gestion.atenderAlerta(alerta.id, comentario)),
      )
      .subscribe({
        next: () => {
          this.notificaciones.exito('Alerta marcada como atendida');
          this.version.update((valor) => valor + 1);
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }
}
