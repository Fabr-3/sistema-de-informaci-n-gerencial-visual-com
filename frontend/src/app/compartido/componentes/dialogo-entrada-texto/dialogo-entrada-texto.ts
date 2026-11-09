import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DatosEntradaTexto {
  readonly titulo: string;
  readonly etiqueta: string;
  readonly textoConfirmar: string;
  readonly valorInicial?: string;
  readonly longitudMaxima: number;
  readonly indicacion?: string;
  /** Usa un area de texto (p. ej. motivos). */
  readonly multilinea?: boolean;
  readonly destructiva?: boolean;
}

/** Solicita un texto obligatorio. Se cierra con el texto recortado o sin valor si se cancela. */
@Component({
  selector: 'app-dialogo-entrada-texto',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="confirmar()" novalidate>
      <mat-dialog-content>
        @if (datos.indicacion) {
          <p class="indicacion">{{ datos.indicacion }}</p>
        }
        <mat-form-field appearance="outline" class="campo">
          <mat-label>{{ datos.etiqueta }}</mat-label>
          @if (datos.multilinea) {
            <textarea matInput [formControl]="texto" rows="3" [maxlength]="datos.longitudMaxima"></textarea>
          } @else {
            <input matInput [formControl]="texto" [maxlength]="datos.longitudMaxima" />
          }
          <mat-error>Este campo es obligatorio</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [class.boton-destructivo]="datos.destructiva">
          {{ datos.textoConfirmar }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campo {
      width: 100%;
    }

    .boton-destructivo {
      --mat-button-filled-container-color: var(--vc-rojo-oscuro);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoEntradaTexto {
  protected readonly datos = inject<DatosEntradaTexto>(MAT_DIALOG_DATA);
  private readonly dialogo = inject(MatDialogRef<DialogoEntradaTexto, string>);

  protected readonly texto = new FormControl(this.datos.valorInicial ?? '', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(this.datos.longitudMaxima), Validators.pattern(/\S/)],
  });
  /** El grupo enlaza el formulario para que ngSubmit funcione y evite el envio nativo del navegador. */
  protected readonly formulario = new FormGroup({ texto: this.texto });

  protected confirmar(): void {
    if (this.texto.invalid) {
      this.texto.markAsTouched();
      return;
    }
    this.dialogo.close(this.texto.value.trim());
  }
}
