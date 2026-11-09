import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { catchError, of, switchMap } from 'rxjs';

import { EtiquetaPipe } from '../../../../../compartido/pipes/etiqueta.pipe';
import { Pagina } from '../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { InventarioLotes } from '../../../aplicacion/inventario-lotes';
import { Existencia, MovimientoLote, TIPOS_MOVIMIENTO } from '../../../dominio/modelos/lotes';

const TAMANO_HISTORIAL = 10;

/** Movimientos de una existencia, del mas reciente al mas antiguo. */
@Component({
  selector: 'app-dialogo-historial-lote',
  imports: [DatePipe, EtiquetaPipe, MatButtonModule, MatDialogModule, MatPaginatorModule, MatTableModule],
  template: `
    <h2 mat-dialog-title>{{ existencia.modelo }} · {{ existencia.bodega }}</h2>
    <mat-dialog-content>
      <p class="indicacion">Existencia actual: <strong>{{ existencia.cantidad }}</strong> unidades.</p>
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="movimientos()?.contenido ?? []">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let movimiento">{{ movimiento.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let movimiento">{{ movimiento.tipo | etiqueta: tipos }}</td>
          </ng-container>
          <ng-container matColumnDef="cantidad">
            <th mat-header-cell *matHeaderCellDef>Cantidad</th>
            <td mat-cell *matCellDef="let movimiento" [class.negativo]="movimiento.cantidad < 0">
              {{ movimiento.cantidad > 0 ? '+' : '' }}{{ movimiento.cantidad }}
            </td>
          </ng-container>
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef>Motivo</th>
            <td mat-cell *matCellDef="let movimiento">{{ movimiento.motivo ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef>Usuario</th>
            <td mat-cell *matCellDef="let movimiento" class="texto-secundario">{{ movimiento.nombreCreador ?? '—' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
        </table>
      </div>
      <mat-paginator
        [length]="movimientos()?.totalElementos ?? 0"
        [pageIndex]="pagina()"
        [pageSize]="tamano"
        [hidePageSize]="true"
        (page)="cambiarPagina($event)"
      />
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .negativo {
      color: var(--vc-rojo);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoHistorialLote {
  protected readonly existencia = inject<Existencia>(MAT_DIALOG_DATA);
  private readonly inventario = inject(InventarioLotes);
  private readonly notificaciones = inject(ServicioNotificaciones);

  protected readonly tipos = TIPOS_MOVIMIENTO;
  protected readonly columnas = ['fecha', 'tipo', 'cantidad', 'motivo', 'usuario'];
  protected readonly tamano = TAMANO_HISTORIAL;
  protected readonly pagina = signal(0);
  protected readonly movimientos = signal<Pagina<MovimientoLote> | null>(null);

  constructor() {
    toObservable(this.pagina)
      .pipe(
        switchMap((pagina) =>
          this.inventario.listarMovimientos(this.existencia.id, { pagina, tamano: this.tamano }).pipe(
            catchError((error: unknown) => {
              this.notificaciones.error(error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((movimientos) => this.movimientos.set(movimientos));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
  }
}
