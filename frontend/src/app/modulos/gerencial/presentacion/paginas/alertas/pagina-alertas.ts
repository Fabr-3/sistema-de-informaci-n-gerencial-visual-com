import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Rutas, rutaAbsoluta } from '../../../../../nucleo/configuracion/rutas';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { GestionGerencial } from '../../../aplicacion/gestion-gerencial';
import {
  AlertaActivo,
  SEVERIDADES,
  Severidad,
  TIPOS_ALERTA,
  TipoAlertaActivo,
  contarPorSeveridad,
  filtrarAlertas,
  iconoSeveridad,
  tipoVisualSeveridad,
} from '../../../dominio/modelos/alertas';

/** Alertas vigentes del estado de los activos, filtrables por severidad y tipo. */
@Component({
  selector: 'app-pagina-alertas',
  imports: [
    EtiquetaPipe,
    FormsModule,
    KeyValuePipe,
    RouterLink,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './pagina-alertas.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaAlertas {
  private readonly gestion = inject(GestionGerencial);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly severidades = SEVERIDADES;
  protected readonly tipos = TIPOS_ALERTA;
  protected readonly columnas = ['severidad', 'activo', 'tipo', 'detalle'];
  protected readonly tipoVisualSeveridad = tipoVisualSeveridad;
  protected readonly iconoSeveridad = iconoSeveridad;
  protected readonly ordenOriginal = (): number => 0;

  protected readonly severidad = signal<Severidad | null>(null);
  protected readonly tipo = signal<TipoAlertaActivo | null>(null);

  /** undefined mientras carga; null si fallo. */
  protected readonly alertas = toSignal<AlertaActivo[] | null>(
    this.gestion.alertasActivos().pipe(
      catchError((error: unknown) => {
        this.notificaciones.error(error);
        return of(null);
      }),
    ),
  );

  protected readonly filtradas = computed(() =>
    filtrarAlertas(this.alertas() ?? [], this.severidad(), this.tipo()),
  );
  protected readonly conteo = computed(() => contarPorSeveridad(this.alertas() ?? []));

  protected rutaActivo(alerta: AlertaActivo): string {
    return rutaAbsoluta(Rutas.ACTIVOS, String(alerta.idActivo));
  }
}
