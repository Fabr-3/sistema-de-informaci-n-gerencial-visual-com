# Mejoras de las pruebas de usuarios

Resumen de lo que se pidió en las pruebas con el gerente y el personal técnico, y dónde quedó implementado.

| # | Pedido | Implementación |
|---|--------|----------------|
| 1 | El gerente ve su tablero e indicadores al entrar. | Al iniciar sesión, el gerente va directo a **Información gerencial → Tablero gerencial**. |
| 2 | Las alertas se muestran al entrar. | Aviso temporal al iniciar sesión con las alertas pendientes (y las críticas) o, para el técnico, sus asignaciones próximas; una vez por sesión, con acceso directo. |
| 3 | Flujo secuencial del gerente. | Cliente → cotización → aceptación → proyecto (con su avance) → contrato → eventos → plan logístico (jornadas, personal, equipos) → cronograma, con enlaces entre las pantallas. |
| 4 | Crear un cliente desde la cotización. | Botón «Registrar un cliente nuevo» en el formulario de la cotización (diálogo flotante). |
| 5 | Campos del cliente validados. | Nombres de personas solo con letras; razón social, documentos y contactos con su formato. |
| 6 | Complemento del carnet de identidad. | Campo opcional en clientes, personal técnico y proveedores (`V17`). |
| 7 | Reserva automática de equipos. | **Reserva automática** en el plan del evento: cantidades por modelo y el sistema elige las unidades libres. |
| 8 | Fechas y horas con selector gráfico. | Calendario desplegable y lista de horas (24 h) en jornadas, asignaciones, eventos y uso de equipos. |
| 9 | Aviso al superar las existencias. | La reserva automática muestra los faltantes y ofrece subalquilarlos (subcontratación con egreso del proyecto y del evento, visible como «Subcontratado» en *Equipos*) o cancelar el evento (`V20`). |
| 10 | Anular o activar proveedores en Subcontratación. | Pestaña *Proveedores* en Subcontrataciones (y en Finanzas) con anular/activar; un proveedor anulado no se elige en operaciones nuevas. |
| 11 | PDF de movimientos del proyecto. | Botón «Descargar PDF» en el detalle de **Resultados por proyecto**. |
| 12 | Proveedores con los datos del cliente. | Tipo (persona natural, empresa privada, institución pública), nombre o razón social, dirección y documentos con las mismas reglas (`V18`). |
| 13 | Cotizaciones más intuitivas. | Filas navegables con indicador; al hacer clic se abre la cotización. |
| 14 | Pantallazo de asignaciones del técnico. | La página de inicio del técnico muestra sus asignaciones pendientes. |
| 15 | Gastos fuera de los consumibles con recibo. | **Gastos del evento**: descripción, monto, evento y comprobante (foto o PDF); egreso del proyecto y del evento en Finanzas (`V22`). |
| 16 | Uso de varios activos a la vez. | «Registrar uso de varios» en Activos: mismo periodo para varios equipos (todos o ninguno). |
| 17 | Logo «visual.com» en Poppins ExtraLight. | Marca en la barra lateral y en el inicio de sesión. |
| 18 | Quitar el lema de la barra superior. | Retirado para todos los usuarios. |
| 19 | Descripción de los modelos para el técnico. | Descripción del modelo (`V21`) en Catálogos (ficha de consulta) y en la ficha de cada activo. |
| 20 | Calculadora LED. | **Herramientas → Calculadora LED**: m², resolución, módulos y costo por m² por día; no guarda datos. |
| 21 | Cronograma del técnico con sus eventos. | **Mi cronograma**: calendario solo con los eventos asignados, en su horario de trabajo. |
