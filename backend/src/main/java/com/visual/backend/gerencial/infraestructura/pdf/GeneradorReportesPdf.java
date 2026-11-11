package com.visual.backend.gerencial.infraestructura.pdf;

import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA_HORA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.NORMAL;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SECUNDARIO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SUBTITULO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celda;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celdaTotal;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.datos;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.espacio;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.importe;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.tabla;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import com.visual.backend.compartido.infraestructura.configuracion.PropiedadesEmpresa;
import com.visual.backend.compartido.infraestructura.pdf.ElementosPdf;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.FilaEvento;
import com.visual.backend.gerencial.dominio.modelo.FilaInventario;
import com.visual.backend.gerencial.dominio.modelo.FilaLote;
import com.visual.backend.gerencial.dominio.modelo.FilaMantenimiento;
import com.visual.backend.gerencial.dominio.modelo.IndicadoresPeriodo;
import com.visual.backend.gerencial.dominio.modelo.KpisGerenciales;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.RentabilidadRecurrente;
import com.visual.backend.gerencial.dominio.modelo.ResultadoCliente;
import com.visual.backend.gerencial.dominio.modelo.ResumenAlertas;
import com.visual.backend.gerencial.dominio.modelo.Severidad;
import com.visual.backend.gerencial.dominio.modelo.TasaUtilizacion;
import com.visual.backend.gerencial.dominio.modelo.TipoAlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.TipoReporte;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionCategoria;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionPersonal;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionRecursos;
import com.visual.backend.gerencial.dominio.puerto.GeneradorReportes;

/** Reportes operativos en PDF con la identidad de la empresa. */
@Component
public class GeneradorReportesPdf implements GeneradorReportes {

    private static final Rectangle HORIZONTAL = PageSize.LETTER.rotate();
    private static final Rectangle VERTICAL = PageSize.LETTER;
    private static final String SIN_DATO = "—";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DECIMALES_PORCENTAJE = 1;
    private static final int DECIMALES_DIVISION = 6;

    private static final float[] ANCHOS_INDICADORES = {40f, 20f, 20f, 20f};
    private static final float[] ANCHOS_CLIENTES = {34f, 12f, 15f, 15f, 15f, 9f};
    private static final float[] ANCHOS_UTILIZACION = {40f, 12f, 16f, 16f, 16f};
    private static final float[] ANCHOS_ALERTAS = {14f, 14f, 30f, 42f};
    private static final float[] ANCHOS_EVENTOS = {11f, 11f, 20f, 25f, 17f, 8f, 8f};
    private static final float[] ANCHOS_INVENTARIO = {25f, 9f, 11f, 9f, 13f, 9f, 9f, 15f};
    private static final float[] ANCHOS_LOTES = {40f, 25f, 20f, 15f};
    private static final float[] ANCHOS_MANTENIMIENTOS = {11f, 22f, 11f, 12f, 12f, 20f, 12f};
    private static final float[] ANCHOS_PERSONAL = {34f, 14f, 14f, 12f, 12f, 14f};

    private static final Map<String, String> ESTADOS_EVENTO = Map.of(
            "PROGRAMADO", "Programado", "EJECUTADO", "Ejecutado", "CANCELADO", "Cancelado");
    private static final Map<String, String> CONDICIONES = Map.of(
            "OPERATIVO", "Operativo", "OPERATIVO_CON_OBSERVACIONES", "Operativo con observaciones",
            "NO_REPARABLE", "No reparable");
    private static final String EN_PROCESO = "EN_PROCESO";
    private static final Map<Severidad, String> SEVERIDADES = Map.of(
            Severidad.CRITICA, "Crítica", Severidad.ADVERTENCIA, "Advertencia", Severidad.INFORMATIVA, "Informativa");
    private static final Map<TipoAlertaActivo, String> TIPOS_ALERTA = Map.of(
            TipoAlertaActivo.MANTENIMIENTO_VENCIDO, "Mantenimiento vencido",
            TipoAlertaActivo.MANTENIMIENTO_PROXIMO, "Mantenimiento próximo",
            TipoAlertaActivo.MANTENIMIENTO_PROLONGADO, "Mantenimiento prolongado",
            TipoAlertaActivo.MODULOS_DEFECTUOSOS, "Módulos LED defectuosos",
            TipoAlertaActivo.FALLAS_RECURRENTES, "Fallas recurrentes",
            TipoAlertaActivo.SIN_USO, "Sin uso");

    private final PropiedadesEmpresa empresa;

    public GeneradorReportesPdf(PropiedadesEmpresa empresa) {
        this.empresa = empresa;
    }

    @Override
    public byte[] resumenGerencial(KpisGerenciales kpis, List<AlertaActivo> alertas, LocalDateTime generado) {
        IndicadoresPeriodo actual = kpis.actual();
        return documento(VERTICAL, TipoReporte.RESUMEN_GERENCIAL, periodo(actual.desde(), actual.hasta()), generado,
                documento -> {
                    seccion(documento, "Indicadores financieros", "Frente al periodo anterior ("
                            + rango(kpis.anterior().desde(), kpis.anterior().hasta()) + ")");
                    documento.add(indicadores(actual, kpis.anterior()));
                    rentabilidad(documento, kpis.rentabilidadRecurrente());
                    seccion(documento, "Tasa de utilización de recursos",
                            "Días con uso o asignación sobre los días disponibles del periodo");
                    documento.add(utilizacion(kpis.utilizacion()));
                    ResumenAlertas resumen = kpis.alertas();
                    seccion(documento, "Alertas de estado de activos", "Críticas: " + resumen.criticas()
                            + " · Advertencias: " + resumen.advertencias() + " · Informativas: "
                            + resumen.informativas());
                    List<AlertaActivo> urgentes = alertas.stream()
                            .filter(alerta -> alerta.severidad() != Severidad.INFORMATIVA).toList();
                    if (!urgentes.isEmpty()) {
                        documento.add(alertas(urgentes));
                    }
                });
    }

    @Override
    public byte[] eventos(Periodo periodo, List<FilaEvento> eventos, LocalDateTime generado) {
        return documento(HORIZONTAL, TipoReporte.EVENTOS, periodo(periodo.desde(), periodo.hasta()), generado,
                documento -> {
                    Map<String, Long> porEstado = eventos.stream().collect(Collectors.groupingBy(FilaEvento::estado,
                            LinkedHashMap::new, Collectors.counting()));
                    documento.add(new Paragraph(cantidad(eventos.size(), "evento", "eventos")
                            + (porEstado.isEmpty() ? "" : " (" + porEstado.entrySet().stream()
                                    .map(estado -> ESTADOS_EVENTO.get(estado.getKey()) + ": " + estado.getValue())
                                    .collect(Collectors.joining(" · ")) + ")"), NORMAL));
                    espacio(documento);
                    PdfPTable tabla = tabla(ANCHOS_EVENTOS, "Inicio", "Fin", "Evento", "Proyecto y cliente", "Lugar",
                            "Personal", "Jornadas");
                    for (FilaEvento evento : eventos) {
                        tabla.addCell(texto(evento.inicio().format(FECHA_HORA)));
                        tabla.addCell(texto(evento.fin().format(FECHA_HORA)));
                        tabla.addCell(texto(evento.evento() + "\n" + ESTADOS_EVENTO.get(evento.estado())));
                        tabla.addCell(texto(evento.proyecto() + "\n" + evento.cliente()));
                        tabla.addCell(texto(evento.lugar()));
                        tabla.addCell(cifra(String.valueOf(evento.personal())));
                        tabla.addCell(cifra(String.valueOf(evento.jornadas())));
                    }
                    agregarOVacio(documento, tabla, eventos, "No hay eventos en el periodo.");
                });
    }

    @Override
    public byte[] estadoActivos(List<FilaInventario> inventario, List<FilaLote> lotes, List<AlertaActivo> alertas,
                                LocalDateTime generado) {
        return documento(HORIZONTAL, TipoReporte.ESTADO_ACTIVOS, Map.of("Situación al",
                generado.format(FECHA)), generado, documento -> {
                    seccion(documento, "Equipos por unidad", "El valor es el costo de adquisición de los equipos "
                            + "que siguen en el inventario");
                    PdfPTable tabla = tabla(ANCHOS_INVENTARIO, "Categoría", "Total", "Disponibles", "En uso",
                            "En mantenimiento", "De baja", "Vendidos", "Valor");
                    for (FilaInventario fila : inventario) {
                        tabla.addCell(texto(fila.categoria()));
                        for (int cantidad : List.of(fila.total(), fila.disponibles(), fila.enUso(),
                                fila.enMantenimiento(), fila.deBaja(), fila.vendidos())) {
                            tabla.addCell(cifra(String.valueOf(cantidad)));
                        }
                        tabla.addCell(cifra(importe(fila.valor())));
                    }
                    tabla.addCell(celdaTotal("TOTAL", Element.ALIGN_LEFT, 1));
                    for (Function<FilaInventario, Integer> columna : List.<Function<FilaInventario, Integer>>of(
                            FilaInventario::total, FilaInventario::disponibles, FilaInventario::enUso,
                            FilaInventario::enMantenimiento, FilaInventario::deBaja, FilaInventario::vendidos)) {
                        tabla.addCell(celdaTotal(String.valueOf(inventario.stream().mapToInt(columna::apply).sum()),
                                Element.ALIGN_RIGHT, 1));
                    }
                    tabla.addCell(celdaTotal(importe(inventario.stream().map(FilaInventario::valor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)), Element.ALIGN_RIGHT, 1));
                    documento.add(tabla);
                    seccion(documento, "Inventario por lotes", "Existencias con saldo por bodega");
                    PdfPTable tablaLotes = tabla(ANCHOS_LOTES, "Modelo", "Categoría", "Bodega", "Cantidad");
                    for (FilaLote lote : lotes) {
                        tablaLotes.addCell(texto(lote.modelo()));
                        tablaLotes.addCell(texto(lote.categoria()));
                        tablaLotes.addCell(texto(lote.bodega()));
                        tablaLotes.addCell(cifra(String.valueOf(lote.cantidad())));
                    }
                    agregarOVacio(documento, tablaLotes, lotes, "No hay existencias por lote.");
                    seccion(documento, "Alertas de estado", cantidad(alertas.size(), "alerta vigente",
                            "alertas vigentes"));
                    agregarOVacio(documento, alertas(alertas), alertas, "No hay alertas vigentes.");
                });
    }

    @Override
    public byte[] mantenimientos(Periodo periodo, List<FilaMantenimiento> mantenimientos, LocalDateTime generado) {
        return documento(HORIZONTAL, TipoReporte.MANTENIMIENTOS, periodo(periodo.desde(), periodo.hasta()), generado,
                documento -> {
                    long enProceso = mantenimientos.stream().filter(m -> EN_PROCESO.equals(m.estado())).count();
                    documento.add(new Paragraph(cantidad(mantenimientos.size(), "mantenimiento", "mantenimientos")
                            + " en el periodo; en proceso: " + enProceso + ".", NORMAL));
                    espacio(documento);
                    PdfPTable tabla = tabla(ANCHOS_MANTENIMIENTOS, "Activo", "Modelo", "Tipo", "Inicio", "Fin",
                            "Condición final", "Costo");
                    for (FilaMantenimiento fila : mantenimientos) {
                        tabla.addCell(texto(fila.codigo()));
                        tabla.addCell(texto(fila.modelo()));
                        tabla.addCell(texto(fila.tipo()));
                        tabla.addCell(texto(fila.inicio().format(FECHA_HORA)));
                        tabla.addCell(texto(fila.fin() == null ? "En proceso" : fila.fin().format(FECHA_HORA)));
                        tabla.addCell(texto(fila.condicion() == null ? SIN_DATO : CONDICIONES.get(fila.condicion())));
                        tabla.addCell(cifra(fila.costo() == null ? SIN_DATO : importe(fila.costo())));
                    }
                    tabla.addCell(celdaTotal("COSTO TOTAL", Element.ALIGN_RIGHT, ANCHOS_MANTENIMIENTOS.length - 1));
                    tabla.addCell(celdaTotal(importe(mantenimientos.stream().map(FilaMantenimiento::costo)
                            .filter(costo -> costo != null).reduce(BigDecimal.ZERO, BigDecimal::add)),
                            Element.ALIGN_RIGHT, 1));
                    agregarOVacio(documento, tabla, mantenimientos, "No hubo mantenimientos en el periodo.");
                });
    }

    @Override
    public byte[] personal(Periodo periodo, UtilizacionRecursos utilizacion, LocalDateTime generado) {
        return documento(VERTICAL, TipoReporte.PERSONAL, periodo(periodo.desde(), periodo.hasta()), generado,
                documento -> {
                    documento.add(new Paragraph("Días con asignaciones en eventos no cancelados sobre los "
                            + periodo.dias() + " días del periodo.", NORMAL));
                    espacio(documento);
                    PdfPTable tabla = tabla(ANCHOS_PERSONAL, "Técnico", "Días asignados", "Días del periodo",
                            "Tasa", "Eventos", "Horas");
                    for (UtilizacionPersonal tecnico : utilizacion.porTecnico()) {
                        tabla.addCell(texto(tecnico.nombre()));
                        tabla.addCell(cifra(String.valueOf(tecnico.tasa().diasOcupados())));
                        tabla.addCell(cifra(String.valueOf(tecnico.tasa().diasDisponibles())));
                        tabla.addCell(cifra(porcentaje(tecnico.tasa().porcentaje())));
                        tabla.addCell(cifra(String.valueOf(tecnico.eventos())));
                        tabla.addCell(cifra(decimal(tecnico.horas())));
                    }
                    TasaUtilizacion total = utilizacion.personal();
                    tabla.addCell(celdaTotal("TOTAL (" + total.recursos() + " técnicos)", Element.ALIGN_LEFT, 1));
                    tabla.addCell(celdaTotal(String.valueOf(total.diasOcupados()), Element.ALIGN_RIGHT, 1));
                    tabla.addCell(celdaTotal(String.valueOf(total.diasDisponibles()), Element.ALIGN_RIGHT, 1));
                    tabla.addCell(celdaTotal(porcentaje(total.porcentaje()), Element.ALIGN_RIGHT, 1));
                    tabla.addCell(celdaTotal("", Element.ALIGN_RIGHT, 2));
                    agregarOVacio(documento, tabla, utilizacion.porTecnico(), "No hay personal técnico activo.");
                });
    }

    private byte[] documento(Rectangle tamano, TipoReporte tipo, Map<String, String> datosPeriodo,
                             LocalDateTime generado, ElementosPdf.ContenidoPdf contenido) {
        String emision = "Generado el " + generado.format(FECHA_HORA);
        return ElementosPdf.generar(tamano, empresa.razonSocial() + "  ·  " + tipo.titulo() + "  ·  " + emision,
                documento -> {
                    ElementosPdf.encabezado(documento, empresa, tipo.titulo().toUpperCase(ElementosPdf.BOLIVIA));
                    Map<String, String> pares = new LinkedHashMap<>(datosPeriodo);
                    pares.put("Emisión", generado.format(FECHA_HORA));
                    documento.add(datos(pares));
                    contenido.escribir(documento);
                });
    }

    private PdfPTable indicadores(IndicadoresPeriodo actual, IndicadoresPeriodo anterior) {
        PdfPTable tabla = tabla(ANCHOS_INDICADORES, "Indicador", "Periodo", "Periodo anterior", "Variación");
        filaImporte(tabla, "Ingresos", actual.ingresos(), anterior.ingresos());
        filaImporte(tabla, "Costos directos", actual.costosDirectos(), anterior.costosDirectos());
        filaImporte(tabla, "Utilidad bruta", actual.utilidadBruta(), anterior.utilidadBruta());
        filaImporte(tabla, "Gastos generales", actual.gastosGenerales(), anterior.gastosGenerales());
        filaImporte(tabla, "Ganancia (utilidad neta)", actual.ganancia(), anterior.ganancia());
        filaPorcentaje(tabla, "Margen de utilidad bruta", actual.margenBruto(), anterior.margenBruto());
        filaPorcentaje(tabla, "Margen de beneficio neto", actual.margenNeto(), anterior.margenNeto());
        return tabla;
    }

    private void rentabilidad(Document documento, RentabilidadRecurrente rentabilidad) {
        seccion(documento, "Rentabilidad recurrente anual", rango(rentabilidad.desde(), rentabilidad.hasta())
                + " · clientes con " + rentabilidad.minimoProyectos() + " o más proyectos contratados");
        documento.add(new Paragraph(rentabilidad.clientesRecurrentes() + " de " + rentabilidad.clientesConProyectos()
                + " clientes con proyectos en el año son recurrentes. Margen " + importe(rentabilidad.margen())
                + " sobre ingresos de " + importe(rentabilidad.ingresos()) + " (" + porcentaje(rentabilidad.porcentaje())
                + "); aportan el " + porcentaje(rentabilidad.participacion()) + " del margen anual de proyectos ("
                + importe(rentabilidad.margenTotalProyectos()) + ").", NORMAL));
        if (rentabilidad.clientes().isEmpty()) {
            return;
        }
        espacio(documento);
        PdfPTable tabla = tabla(ANCHOS_CLIENTES, "Cliente recurrente", "Proyectos", "Ingresos", "Costos", "Margen",
                "%");
        for (ResultadoCliente cliente : rentabilidad.clientes()) {
            tabla.addCell(texto(cliente.cliente()));
            tabla.addCell(cifra(String.valueOf(cliente.proyectosContratados())));
            tabla.addCell(cifra(importe(cliente.ingresos())));
            tabla.addCell(cifra(importe(cliente.costos())));
            tabla.addCell(cifra(importe(cliente.margen())));
            tabla.addCell(cifra(porcentaje(cliente.porcentaje())));
        }
        documento.add(tabla);
    }

    private PdfPTable utilizacion(UtilizacionRecursos utilizacion) {
        PdfPTable tabla = tabla(ANCHOS_UTILIZACION, "Recurso", "Cantidad", "Días disponibles", "Días ocupados",
                "Tasa");
        for (UtilizacionCategoria categoria : utilizacion.porCategoria()) {
            filaUtilizacion(tabla, "Equipos · " + categoria.categoria(), categoria.tasa(), false);
        }
        filaUtilizacion(tabla, "Total de equipos", utilizacion.equipos(), true);
        filaUtilizacion(tabla, "Personal técnico", utilizacion.personal(), true);
        return tabla;
    }

    private static PdfPTable alertas(List<AlertaActivo> alertas) {
        PdfPTable tabla = tabla(ANCHOS_ALERTAS, "Severidad", "Activo", "Alerta", "Detalle");
        for (AlertaActivo alerta : alertas) {
            tabla.addCell(texto(SEVERIDADES.get(alerta.severidad())));
            tabla.addCell(texto(alerta.codigo()));
            tabla.addCell(texto(TIPOS_ALERTA.get(alerta.tipo()) + "\n" + alerta.modelo()));
            tabla.addCell(texto(alerta.detalle()));
        }
        return tabla;
    }

    private static void filaImporte(PdfPTable tabla, String nombre, BigDecimal actual, BigDecimal anterior) {
        tabla.addCell(texto(nombre));
        tabla.addCell(cifra(importe(actual)));
        tabla.addCell(cifra(importe(anterior)));
        tabla.addCell(cifra(anterior.signum() == 0 ? SIN_DATO : porcentaje(actual.subtract(anterior)
                .divide(anterior.abs(), DECIMALES_DIVISION, RoundingMode.HALF_UP).multiply(CIEN)
                .setScale(DECIMALES_PORCENTAJE, RoundingMode.HALF_UP), true)));
    }

    /** La variacion de un margen se expresa en puntos porcentuales. */
    private static void filaPorcentaje(PdfPTable tabla, String nombre, BigDecimal actual, BigDecimal anterior) {
        tabla.addCell(texto(nombre));
        tabla.addCell(cifra(porcentaje(actual)));
        tabla.addCell(cifra(porcentaje(anterior)));
        tabla.addCell(cifra(actual == null || anterior == null ? SIN_DATO
                : signo(actual.subtract(anterior)) + decimal(actual.subtract(anterior).abs()) + " pp"));
    }

    private static void filaUtilizacion(PdfPTable tabla, String nombre, TasaUtilizacion tasa, boolean total) {
        if (total) {
            tabla.addCell(celdaTotal(nombre, Element.ALIGN_LEFT, 1));
            for (String valor : List.of(String.valueOf(tasa.recursos()), String.valueOf(tasa.diasDisponibles()),
                    String.valueOf(tasa.diasOcupados()), porcentaje(tasa.porcentaje()))) {
                tabla.addCell(celdaTotal(valor, Element.ALIGN_RIGHT, 1));
            }
            return;
        }
        tabla.addCell(texto(nombre));
        tabla.addCell(cifra(String.valueOf(tasa.recursos())));
        tabla.addCell(cifra(String.valueOf(tasa.diasDisponibles())));
        tabla.addCell(cifra(String.valueOf(tasa.diasOcupados())));
        tabla.addCell(cifra(porcentaje(tasa.porcentaje())));
    }

    private static void seccion(Document documento, String titulo, String descripcion) {
        espacio(documento);
        documento.add(new Paragraph(titulo, SUBTITULO));
        Paragraph detalle = new Paragraph(descripcion, SECUNDARIO);
        detalle.setSpacingAfter(6f);
        documento.add(detalle);
    }

    private static void agregarOVacio(Document documento, PdfPTable tabla, List<?> filas, String mensajeVacio) {
        documento.add(filas.isEmpty() ? new Paragraph(mensajeVacio, SECUNDARIO) : tabla);
    }

    private static Map<String, String> periodo(LocalDate desde, LocalDate hasta) {
        return Map.of("Periodo", rango(desde, hasta));
    }

    private static String rango(LocalDate desde, LocalDate hasta) {
        return desde.format(FECHA) + " al " + hasta.format(FECHA);
    }

    private static String cantidad(long valor, String singular, String plural) {
        return valor + " " + (valor == 1 ? singular : plural);
    }

    private static String porcentaje(BigDecimal valor) {
        return porcentaje(valor, false);
    }

    private static String porcentaje(BigDecimal valor, boolean conSigno) {
        if (valor == null) {
            return SIN_DATO;
        }
        return (conSigno ? signo(valor) : valor.signum() < 0 ? "−" : "") + decimal(valor.abs()) + " %";
    }

    private static String signo(BigDecimal valor) {
        return valor.signum() > 0 ? "+" : valor.signum() < 0 ? "−" : "";
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(DECIMALES_PORCENTAJE, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static PdfPCell texto(String valor) {
        return celda(valor, Element.ALIGN_LEFT, NORMAL);
    }

    private static PdfPCell cifra(String valor) {
        return celda(valor, Element.ALIGN_RIGHT, NORMAL);
    }
}
