package com.visual.backend.finanzas.infraestructura.pdf;

import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA_HORA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.NORMAL;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SECUNDARIO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SUBTITULO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celda;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celdaTotal;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.importe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import com.visual.backend.compartido.infraestructura.configuracion.PropiedadesEmpresa;
import com.visual.backend.compartido.infraestructura.pdf.ElementosPdf;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Margen;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.Referencia;
import com.visual.backend.finanzas.dominio.modelo.ResultadoEvento;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.TotalPorTipo;
import com.visual.backend.finanzas.dominio.puerto.GeneradorPdfFinanzas;

/** PDF de los movimientos economicos de un proyecto, con la identidad visual comun. */
@Component
public class GeneradorPdfFinanzasOpenPdf implements GeneradorPdfFinanzas {

    private static final float[] ANCHOS_MOVIMIENTOS = {1.1f, 2.2f, 3.6f, 2.2f, 1.6f, 1.6f};
    private static final float[] ANCHOS_TIPOS = {4f, 1.6f, 1.6f};
    private static final float[] ANCHOS_EVENTOS = {3.4f, 1.6f, 1.6f, 1.6f, 1.6f, 1.1f};
    private static final String SIN_DATO = "—";
    private static final Map<String, String> ESTADOS_PROYECTO = Map.of("PLANIFICACION", "En planificación",
            "EN_EJECUCION", "En ejecución", "FINALIZADO", "Finalizado", "CANCELADO", "Cancelado");

    private final PropiedadesEmpresa empresa;

    public GeneradorPdfFinanzasOpenPdf(PropiedadesEmpresa empresa) {
        this.empresa = empresa;
    }

    @Override
    public byte[] movimientosProyecto(DetalleResultadoProyecto detalle, List<MovimientoFinanciero> movimientos,
                                      LocalDateTime generado) {
        ResultadoProyecto proyecto = detalle.proyecto();
        return ElementosPdf.generar(PageSize.LETTER.rotate(), "Movimientos de " + proyecto.proyecto(), documento -> {
            ElementosPdf.encabezado(documento, empresa, "Movimientos económicos del proyecto");
            documento.add(ElementosPdf.datos(datosProyecto(proyecto, generado)));
            documento.add(ElementosPdf.datos(datosMargen(proyecto)));
            seccion(documento, "Movimientos registrados");
            documento.add(movimientos.isEmpty() ? vacio("El proyecto no tiene movimientos registrados.")
                    : tablaMovimientos(movimientos));
            seccion(documento, "Resumen por tipo (incluye consumibles usados)");
            documento.add(tablaTipos(detalle.porTipo()));
            if (!detalle.eventos().isEmpty()) {
                seccion(documento, "Resultado por evento");
                documento.add(tablaEventos(detalle.eventos()));
            }
            documento.add(new Paragraph("Los movimientos anulados no se incluyen. Margen = ingresos − costos "
                    + "directos (egresos imputados y consumibles), sin sueldos.", SECUNDARIO));
        });
    }

    private static Map<String, String> datosProyecto(ResultadoProyecto proyecto, LocalDateTime generado) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("Proyecto", proyecto.proyecto());
        datos.put("Cliente", proyecto.cliente());
        datos.put("Estado", ESTADOS_PROYECTO.getOrDefault(proyecto.estado(), proyecto.estado()));
        datos.put("Generado", FECHA_HORA.format(generado));
        return datos;
    }

    private static Map<String, String> datosMargen(ResultadoProyecto proyecto) {
        Margen margen = proyecto.margen();
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("Contratado", importe(proyecto.contratado()));
        datos.put("Ingresos", importe(margen.ingresos()));
        datos.put("Costos directos", importe(margen.costos()));
        datos.put("Margen", importe(margen.margen()) + porcentaje(margen));
        datos.put("Por cobrar", importe(proyecto.porCobrar()));
        return datos;
    }

    private static PdfPTable tablaMovimientos(List<MovimientoFinanciero> movimientos) {
        PdfPTable tabla = ElementosPdf.tabla(ANCHOS_MOVIMIENTOS, "Fecha", "Tipo", "Concepto", "Evento / proveedor",
                "Ingreso", "Egreso");
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (MovimientoFinanciero movimiento : movimientos) {
            boolean ingreso = movimiento.getTipo().naturaleza() == Naturaleza.INGRESO;
            tabla.addCell(celda(FECHA.format(movimiento.getFecha()), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(movimiento.getTipo().nombre(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(movimiento.getConcepto(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(imputacion(movimiento), Element.ALIGN_LEFT, SECUNDARIO));
            tabla.addCell(celda(ingreso ? importe(movimiento.getMonto()) : "", Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(ingreso ? "" : importe(movimiento.getMonto()), Element.ALIGN_RIGHT, NORMAL));
            if (ingreso) {
                ingresos = ingresos.add(movimiento.getMonto());
            } else {
                egresos = egresos.add(movimiento.getMonto());
            }
        }
        tabla.addCell(celdaTotal("Totales (" + movimientos.size() + " movimientos)", Element.ALIGN_LEFT, 4));
        tabla.addCell(celdaTotal(importe(ingresos), Element.ALIGN_RIGHT, 1));
        tabla.addCell(celdaTotal(importe(egresos), Element.ALIGN_RIGHT, 1));
        tabla.setSpacingAfter(8f);
        return tabla;
    }

    private static PdfPTable tablaTipos(List<TotalPorTipo> tipos) {
        PdfPTable tabla = ElementosPdf.tabla(ANCHOS_TIPOS, "Tipo", "Ingresos", "Egresos");
        for (TotalPorTipo tipo : tipos) {
            boolean ingreso = tipo.naturaleza() == Naturaleza.INGRESO;
            tabla.addCell(celda(tipo.tipo(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(ingreso ? importe(tipo.monto()) : "", Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(ingreso ? "" : importe(tipo.monto()), Element.ALIGN_RIGHT, NORMAL));
        }
        if (tipos.isEmpty()) {
            tabla.addCell(celdaTotal("Sin movimientos", Element.ALIGN_LEFT, ANCHOS_TIPOS.length));
        }
        tabla.setSpacingAfter(8f);
        return tabla;
    }

    private static PdfPTable tablaEventos(List<ResultadoEvento> eventos) {
        PdfPTable tabla = ElementosPdf.tabla(ANCHOS_EVENTOS, "Evento", "Fecha", "Ingresos", "Costos", "Margen", "%");
        for (ResultadoEvento evento : eventos) {
            Margen margen = evento.margen();
            tabla.addCell(celda(evento.evento(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(evento.fechaInicio() == null ? SIN_DATO : FECHA.format(evento.fechaInicio()),
                    Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(importe(margen.ingresos()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(importe(margen.costos()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(importe(margen.margen()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(margen.porcentaje() == null ? SIN_DATO : margen.porcentaje().toPlainString() + " %",
                    Element.ALIGN_RIGHT, NORMAL));
        }
        tabla.setSpacingAfter(8f);
        return tabla;
    }

    private static String imputacion(MovimientoFinanciero movimiento) {
        return String.join(" · ", Stream.of(movimiento.getEvento(), movimiento.getProveedor())
                .filter(Objects::nonNull).map(Referencia::nombre).toList());
    }

    private static String porcentaje(Margen margen) {
        return margen.porcentaje() == null ? "" : " (" + margen.porcentaje().toPlainString() + " %)";
    }

    private static Paragraph vacio(String texto) {
        Paragraph parrafo = new Paragraph(texto, SECUNDARIO);
        parrafo.setSpacingAfter(8f);
        return parrafo;
    }

    private static void seccion(Document documento, String titulo) {
        Paragraph parrafo = new Paragraph(titulo, SUBTITULO);
        parrafo.setSpacingBefore(6f);
        parrafo.setSpacingAfter(6f);
        documento.add(parrafo);
    }
}
