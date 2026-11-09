import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DatosConfirmacion {
  readonly titulo: string;
  readonly mensaje: string;
  readonly textoConfirmar: string;
  /** Resalta la accion en rojo cuando es destructiva (eliminar, desactivar). */
  readonly destructiva?: boolean;
}

/** Dialogo generico de confirmacion. Se cierra con true al confirmar. */
@Component({
  selector: 'app-dialogo-confirmacion',
  imports: [MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ datos.mensaje }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button [mat-dialog-close]="true" [class.boton-destructivo]="datos.destructiva">
        {{ datos.textoConfirmar }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .boton-destructivo {
      --mat-button-filled-container-color: var(--vc-rojo-oscuro);
    }
  `,
})
export class DialogoConfirmacion {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}
