package com.visual.backend.comercial.infraestructura.pdf;

import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.BOLIVIA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.FECHA_HORA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.NEGRITA;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.NORMAL;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SECUNDARIO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.SUBTITULO;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celda;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.celdaTotal;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.datos;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.espacio;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.generar;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.importe;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.numero;
import static com.visual.backend.compartido.infraestructura.pdf.ElementosPdf.tabla;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.LineaCotizacion;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.puerto.GeneradorDocumentos;
import com.visual.backend.compartido.infraestructura.configuracion.PropiedadesEmpresa;
import com.visual.backend.compartido.infraestructura.pdf.ElementosPdf;

/**
 * Documentos PDF de cotizaciones y contratos con la identidad de la empresa
 * (rojo corporativo). Las clausulas del contrato provienen de la plantilla
 * plantillas/contrato-servicios.txt, cuyos marcadores {nombre} se completan
 * con los datos del proyecto.
 */
@Component
public class GeneradorDocumentosPdf implements GeneradorDocumentos {

    private static final String PLANTILLA_CONTRATO = "plantillas/contrato-servicios.txt";
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", BOLIVIA);
    private static final float[] ANCHOS_DETALLE = {40f, 12f, 8f, 13f, 12f, 15f};
    private static final float[] ANCHOS_CRONOGRAMA = {34f, 33f, 33f};


    private final PropiedadesEmpresa empresa;
    private final String plantillaContrato;

    public GeneradorDocumentosPdf(PropiedadesEmpresa empresa) {
        this.empresa = empresa;
        this.plantillaContrato = leerPlantilla();
    }

    @Override
    public byte[] cotizacion(Cotizacion cotizacion, Cliente cliente) {
        return generar(documento -> {
            ElementosPdf.encabezado(documento, empresa, "COTIZACIÓN " + cotizacion.getNumero());
            Map<String, String> datosCliente = new LinkedHashMap<>();
            datosCliente.put("Cliente", cliente.getRazonSocial());
            datosCliente.put("Documento", documentoPrincipal(cliente));
            datosCliente.put("Emisión", cotizacion.getFechaEmision().format(FECHA));
            datosCliente.put("Válida hasta", cotizacion.getFechaValidez().format(FECHA));
            documento.add(datos(datosCliente));
            documento.add(new Paragraph(cotizacion.getTitulo(), SUBTITULO));
            if (cotizacion.getFechaEventoInicio() != null) {
                documento.add(new Paragraph("Evento: " + periodo(cotizacion), NORMAL));
            }
            espacio(documento);
            documento.add(detalle(cotizacion));
            if (cotizacion.getCondiciones() != null) {
                espacio(documento);
                documento.add(new Paragraph("Condiciones comerciales", SUBTITULO));
                documento.add(new Paragraph(cotizacion.getCondiciones(), NORMAL));
            }
            espacio(documento);
            documento.add(new Paragraph("Precios expresados en bolivianos (Bs). La facturación se emite por separado.",
                    SECUNDARIO));
        });
    }

    @Override
    public byte[] contrato(Contrato contrato, Proyecto proyecto, Cotizacion cotizacion, Cliente cliente,
                           List<Evento> eventos) {
        Map<String, String> valores = Map.of(
                "{empresa}", empresa.razonSocial(),
                "{numero_cotizacion}", cotizacion.getNumero(),
                "{proyecto}", proyecto.getNombre(),
                "{periodo}", periodoProyecto(proyecto),
                "{monto}", importe(cotizacion.importeTotal()),
                "{ciudad}", empresa.ciudad(),
                "{fecha_firma}", contrato.getFechaFirma().format(FECHA_LARGA));
        return generar(documento -> {
            ElementosPdf.encabezado(documento, empresa, "CONTRATO DE PRESTACIÓN DE SERVICIOS AUDIOVISUALES N.º " + contrato.getNumero());
            documento.add(parrafoJustificado("Conste por el presente contrato que, de una parte, " + empresa.razonSocial()
                    + ", con domicilio en " + textoOpcional(empresa.direccion(), "la ciudad de " + empresa.ciudad())
                    + ", en adelante la EMPRESA; y de otra parte, " + cliente.getRazonSocial() + ", con "
                    + documentoPrincipal(cliente) + textoOpcional(cliente.getDireccion() == null ? null
                    : ", con domicilio en " + cliente.getDireccion(), "") + ", en adelante el CLIENTE, acuerdan "
                    + "las siguientes cláusulas:", NORMAL));
            espacio(documento);
            for (String clausula : plantillaContrato.split("\\R")) {
                if (!clausula.isBlank()) {
                    documento.add(clausula(reemplazar(clausula, valores)));
                }
            }
            if (contrato.getTerminos() != null) {
                documento.add(new Paragraph("TÉRMINOS PARTICULARES", SUBTITULO));
                documento.add(parrafoJustificado(contrato.getTerminos(), NORMAL));
                espacio(documento);
            }
            documento.add(new Paragraph("ANEXO I · Detalle de servicios", SUBTITULO));
            espacio(documento);
            documento.add(detalle(cotizacion));
            List<Evento> programados = eventos.stream().filter(e -> e.getEstado() != EstadoEvento.CANCELADO).toList();
            if (!programados.isEmpty()) {
                espacio(documento);
                documento.add(new Paragraph("ANEXO II · Cronograma de eventos", SUBTITULO));
                espacio(documento);
                documento.add(cronograma(programados));
            }
            firmas(documento, cliente);
        });
    }

    private PdfPTable detalle(Cotizacion cotizacion) {
        PdfPTable tabla = tabla(ANCHOS_DETALLE, "Servicio", "Unidad", "Cant.", "Precio unit.", "Descuento",
                "Subtotal");
        for (LineaCotizacion linea : cotizacion.getLineas()) {
            tabla.addCell(celda(linea.servicio(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(linea.unidadMedida(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(String.valueOf(linea.cantidad()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.precioUnitario()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.descuento()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.subtotal()), Element.ALIGN_RIGHT, NORMAL));
        }
        tabla.addCell(celdaTotal("TOTAL", Element.ALIGN_RIGHT, ANCHOS_DETALLE.length - 1));
        tabla.addCell(celdaTotal(importe(cotizacion.importeTotal()), Element.ALIGN_RIGHT, 1));
        return tabla;
    }

    private PdfPTable cronograma(List<Evento> eventos) {
        PdfPTable tabla = tabla(ANCHOS_CRONOGRAMA, "Evento", "Lugar", "Horario");
        for (Evento evento : eventos) {
            tabla.addCell(celda(evento.getNombre(), Element.ALIGN_LEFT, NORMAL));
            String lugar = evento.getLugar().nombre()
                    + (evento.getLugar().ciudad() == null ? "" : " (" + evento.getLugar().ciudad() + ")");
            tabla.addCell(celda(lugar, Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(evento.getFechaInicio().format(FECHA_HORA) + " a "
                    + evento.getFechaFin().format(FECHA_HORA), Element.ALIGN_LEFT, NORMAL));
        }
        return tabla;
    }

    private void firmas(Document documento, Cliente cliente) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(90f);
        tabla.setSpacingBefore(60f);
        for (String parte : List.of(empresa.razonSocial() + "\nLA EMPRESA", cliente.getRazonSocial() + "\nEL CLIENTE")) {
            PdfPCell celda = new PdfPCell(new Phrase("______________________________\n" + parte, NORMAL));
            celda.setBorder(PdfPCell.NO_BORDER);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }
        documento.add(tabla);
    }

    /** La primera palabra de la clausula (PRIMERA, SEGUNDA...) y su titulo van en negrita. */
    private static Paragraph clausula(String texto) {
        int finTitulo = texto.indexOf(')');
        Paragraph parrafo = new Paragraph();
        parrafo.setAlignment(Element.ALIGN_JUSTIFIED);
        parrafo.setSpacingAfter(6f);
        if (finTitulo > 0) {
            parrafo.add(new Chunk(texto.substring(0, finTitulo + 1), NEGRITA));
            parrafo.add(new Chunk(texto.substring(finTitulo + 1), NORMAL));
        } else {
            parrafo.add(new Chunk(texto, NORMAL));
        }
        return parrafo;
    }

    private static Paragraph parrafoJustificado(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_JUSTIFIED);
        return parrafo;
    }

    private static String reemplazar(String texto, Map<String, String> valores) {
        String resultado = texto;
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            resultado = resultado.replace(valor.getKey(), valor.getValue());
        }
        return resultado;
    }

    /** NIT si existe; si no, el primer documento registrado. */
    private static String documentoPrincipal(Cliente cliente) {
        DocumentoCliente documento = cliente.getDocumentos().stream()
                .filter(d -> "NIT".equalsIgnoreCase(d.tipo().nombre())).findFirst()
                .orElse(cliente.getDocumentos().getFirst());
        return documento.tipo().nombre() + " " + documento.numero();
    }

    private static String periodo(Cotizacion cotizacion) {
        return rango(cotizacion.getFechaEventoInicio(), cotizacion.getFechaEventoFin());
    }

    private static String periodoProyecto(Proyecto proyecto) {
        return proyecto.getFechaInicio() == null ? "en las fechas del cronograma de eventos"
                : rango(proyecto.getFechaInicio(), proyecto.getFechaFin());
    }

    /** "el 16/07/2026" para un solo dia; "del 18/09/2026 al 27/09/2026" para un periodo. */
    private static String rango(LocalDate inicio, LocalDate fin) {
        if (fin == null || fin.equals(inicio)) {
            return "el " + inicio.format(FECHA);
        }
        return "del " + inicio.format(FECHA) + " al " + fin.format(FECHA);
    }

    private static String textoOpcional(String valor, String alternativo) {
        return valor == null || valor.isBlank() ? alternativo : valor;
    }

    private static String leerPlantilla() {
        try (InputStream entrada = new ClassPathResource(PLANTILLA_CONTRATO).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la plantilla " + PLANTILLA_CONTRATO, e);
        }
    }
}
