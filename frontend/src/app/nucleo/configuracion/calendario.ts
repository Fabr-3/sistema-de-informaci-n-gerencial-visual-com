import { Provider } from '@angular/core';
import { CalendarDateFormatter, CalendarNativeDateFormatter, DateAdapter, provideCalendar } from 'angular-calendar';
import { adapterFactory } from 'angular-calendar/date-adapters/date-fns';

/** Proveedores del calendario de eventos (cronograma general y cronograma propio del tecnico). */
export const PROVEEDORES_CALENDARIO: Provider[] = provideCalendar(
  { provide: DateAdapter, useFactory: adapterFactory },
  // Intl respeta el orden dia-mes del espanol en titulos y cabeceras
  { dateFormatter: { provide: CalendarDateFormatter, useClass: CalendarNativeDateFormatter } },
);
