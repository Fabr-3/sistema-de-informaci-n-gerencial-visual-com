import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import {
  Pagina,
  SolicitudPagina,
  TAMANOS_PAGINA,
  TAMANO_PAGINA_POR_DEFECTO,
} from '../../../../../compartido/modelos/pagina';
import { ServicioNotificaciones } from '../../../../../nucleo/notificaciones/servicio-notificaciones';
import { ServicioSesion } from '../../../../accesos/aplicacion/servicio-sesion';
import { Permisos } from '../../../../accesos/dominio/modelos/permisos';
import { GestionPersonal } from '../../../aplicacion/gestion-personal';
import { CatalogosLogistica, FiltroPersonal, PersonalTecnico } from '../../../dominio/modelos/personal';
import { DatosDialogoPersonal, DialogoPersonal } from './dialogo-personal';

const RETARDO_BUSQUEDA_MS = 300;
const COLUMNAS = ['nombre', 'especialidades', 'telefono', 'cuenta', 'estado', 'acciones'];

interface CriteriosPersonal {
  readonly filtro: FiltroPersonal;
  readonly solicitud: SolicitudPagina;
}

/** Personal tecnico que se asigna a montajes, eventos y desmontajes. */
@Component({
  selector: 'app-pagina-personal',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pagina-personal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaPersonal {
  private readonly gestion = inject(GestionPersonal);
  private readonly notificaciones = inject(ServicioNotificaciones);
  private readonly dialogos = inject(MatDialog);

  protected readonly columnas = COLUMNAS;
  protected readonly tamanosPagina = TAMANOS_PAGINA;
  protected readonly puedeGestionar = inject(ServicioSesion).tienePermiso(Permisos.LOGISTICA_GESTIONAR);
  protected readonly catalogos = toSignal(
    this.gestion.catalogos().pipe(
      catchError((error: unknown) => {
        this.notificaciones.error(error);
        return of(null);
      }),
    ),
    { initialValue: null },
  );
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly resultado = signal<Pagina<PersonalTecnico> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly criterios = signal<CriteriosPersonal>({
    filtro: { activo: true },
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

  protected especialidades(personal: PersonalTecnico): string {
    return personal.especialidades.map((especialidad) => especialidad.nombre).join(', ') || '—';
  }

  protected filtrar(cambios: Partial<FiltroPersonal>): void {
    this.criterios.update(({ filtro, solicitud }) => ({
      filtro: { ...filtro, ...cambios },
      solicitud: { ...solicitud, pagina: 0 },
    }));
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.criterios.update(({ filtro }) => ({ filtro, solicitud: { pagina: evento.pageIndex, tamano: evento.pageSize } }));
  }

  /** Abre el formulario con los catalogos actualizados (las cuentas libres cambian al vincular). */
  protected editar(personal?: PersonalTecnico): void {
    this.gestion
      .catalogos()
      .pipe(
        switchMap((catalogos: CatalogosLogistica) =>
          this.dialogos
            .open<DialogoPersonal, DatosDialogoPersonal, PersonalTecnico>(DialogoPersonal, {
              data: { catalogos, personal },
              width: '640px',
            })
            .afterClosed(),
        ),
        filter(Boolean),
      )
      .subscribe({
        next: () => {
          this.notificaciones.exito(personal ? 'Personal actualizado' : 'Personal registrado');
          this.criterios.update((actuales) => ({ ...actuales }));
        },
        error: (error: unknown) => this.notificaciones.error(error),
      });
  }
}
