import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ActivoReservable, Reserva } from '../dominio/modelos/reservas';
import { RepositorioReservas } from '../dominio/repositorios/repositorio-reservas';

/** Casos de uso de la reserva de equipos por evento. */
@Injectable({ providedIn: 'root' })
export class GestionReservas {
  private readonly repositorio = inject(RepositorioReservas);

  listar(idEvento: number): Observable<Reserva[]> {
    return this.repositorio.listar(idEvento);
  }

  disponibles(idEvento: number, texto: string): Observable<ActivoReservable[]> {
    const limpio = texto.trim();
    return this.repositorio.disponibles(idEvento, { texto: limpio ? limpio : undefined });
  }

  /** Se reservan todos los equipos o ninguno: si alguno esta ocupado la API responde 409 con el detalle. */
  reservar(idEvento: number, idsActivos: readonly number[]): Observable<Reserva[]> {
    return this.repositorio.reservar(idEvento, idsActivos);
  }

  liberar(idEvento: number, idReserva: number, motivo: string): Observable<Reserva> {
    return this.repositorio.liberar(idEvento, idReserva, motivo.trim());
  }
}
