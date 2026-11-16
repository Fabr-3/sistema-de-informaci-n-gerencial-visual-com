import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { parametrosConsulta } from '../../../../compartido/utilidades/parametros-http';
import { URL_API } from '../../../../nucleo/configuracion/api';
import { ActivoReservable, FiltroActivosReservables, Reserva } from '../../dominio/modelos/reservas';
import { RepositorioReservas } from '../../dominio/repositorios/repositorio-reservas';

@Injectable()
export class RepositorioReservasHttp extends RepositorioReservas {
  private readonly http = inject(HttpClient);
  private readonly url = inject(URL_API) + '/logistica/eventos';

  listar(idEvento: number): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.url}/${idEvento}/reservas`);
  }

  disponibles(idEvento: number, filtro: FiltroActivosReservables): Observable<ActivoReservable[]> {
    return this.http.get<ActivoReservable[]>(`${this.url}/${idEvento}/activos-disponibles`, {
      params: parametrosConsulta({ ...filtro }),
    });
  }

  reservar(idEvento: number, idsActivos: readonly number[]): Observable<Reserva[]> {
    return this.http.post<Reserva[]>(`${this.url}/${idEvento}/reservas`, { idsActivos });
  }

  liberar(idEvento: number, idReserva: number, motivo: string): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.url}/${idEvento}/reservas/${idReserva}/liberacion`, { motivo });
  }
}
