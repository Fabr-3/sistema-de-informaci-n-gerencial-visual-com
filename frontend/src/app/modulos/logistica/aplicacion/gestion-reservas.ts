import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ActivoReservable,
  DisponibilidadModelo,
  EquipoSubalquilado,
  PedidoModelo,
  Reserva,
} from '../dominio/modelos/reservas';
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

  disponibilidadPorModelo(idEvento: number): Observable<DisponibilidadModelo[]> {
    return this.repositorio.disponibilidadPorModelo(idEvento);
  }

  /** El sistema elige las unidades; si un modelo no alcanza la API responde 409 con los faltantes. */
  reservarAutomaticamente(idEvento: number, pedidos: readonly PedidoModelo[]): Observable<Reserva[]> {
    return this.repositorio.reservarAutomaticamente(
      idEvento,
      pedidos.filter((pedido) => pedido.cantidad > 0),
    );
  }

  subalquileres(idEvento: number): Observable<EquipoSubalquilado[]> {
    return this.repositorio.subalquileres(idEvento);
  }

  liberar(idEvento: number, idReserva: number, motivo: string): Observable<Reserva> {
    return this.repositorio.liberar(idEvento, idReserva, motivo.trim());
  }
}
