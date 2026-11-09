package com.visual.backend.comercial.infraestructura.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
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
import com.visual.backend.comercial.infraestructura.configuracion.PropiedadesEmpresa;

/**
 * Documentos PDF de cotizaciones y contratos con la identidad de la empresa
 * (rojo corporativo). Las clausulas del contrato provienen de la plantilla
 * plantillas/contrato-servicios.txt, cuyos marcadores {nombre} se completan
 * con los datos del proyecto.
 */
@Component
public class GeneradorDocumentosPdf implements GeneradorDocumentos {

    private static final String PLANTILLA_CONTRATO = "plantillas/contrato-servicios.txt";
    private static final Color ROJO = new Color(0xB7, 0x1C, 0x1C);
    private static final Color GRIS_TEXTO = new Color(0x5F, 0x63, 0x68);
    private static final Color GRIS_FONDO = new Color(0xF5, 0xF5, 0xF5);
    private static final Locale BOLIVIA = Locale.forLanguageTag("es-BO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", BOLIVIA);
    private static final String MONEDA = "Bs ";
    private static final float MARGEN = 50f;
    private static final float[] ANCHOS_DETALLE = {40f, 12f, 8f, 13f, 12f, 15f};
    private static final float[] ANCHOS_CRONOGRAMA = {34f, 33f, 33f};

    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, ROJO);
    private static final Font MARCA = new Font(Font.HELVETICA, 18, Font.BOLD, ROJO);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, Color.BLACK);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 9.5f, Font.BOLD, Color.BLACK);
    private static final Font SECUNDARIO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, GRIS_TEXTO);
    private static final Font CABECERA_TABLA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);

    private final PropiedadesEmpresa empresa;
    private final String plantillaContrato;

    public GeneradorDocumentosPdf(PropiedadesEmpresa empresa) {
        this.empresa = empresa;
        this.plantillaContrato = leerPlantilla();
    }

    @Override
    public byte[] cotizacion(Cotizacion cotizacion, Cliente cliente) {
        return generar(documento -> {
            encabezado(documento, "COTIZACIÓN " + cotizacion.getNumero());
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
            encabezado(documento, "CONTRATO DE PRESTACIÓN DE SERVICIOS AUDIOVISUALES N.º " + contrato.getNumero());
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

    private void encabezado(Document documento, String titulo) {
        documento.add(new Paragraph(empresa.razonSocial(), MARCA));
        String contacto = String.join("  ·  ", Stream.of(empresa.direccion(), empresa.ciudad(),
                empresa.telefono(), empresa.email(), empresa.nit() == null || empresa.nit().isBlank() ? null
                        : "NIT " + empresa.nit())
                .filter(valor -> valor != null && !valor.isBlank()).toList());
        documento.add(new Paragraph(contacto, SECUNDARIO));
        Paragraph linea = new Paragraph(new Chunk(new LineSeparator(1f, 100f, ROJO,
                Element.ALIGN_CENTER, -4f)));
        documento.add(linea);
        espacio(documento);
        Paragraph encabezado = new Paragraph(titulo, TITULO);
        encabezado.setSpacingAfter(10f);
        documento.add(encabezado);
    }

    /** Tabla de pares etiqueta/valor, una columna por par, en el orden recibido. */
    private static PdfPTable datos(Map<String, String> pares) {
        PdfPTable tabla = new PdfPTable(pares.size());
        tabla.setWidthPercentage(100f);
        tabla.setSpacingAfter(8f);
        pares.forEach((etiqueta, valor) -> {
            PdfPCell celda = new PdfPCell();
            celda.setBorder(PdfPCell.NO_BORDER);
            celda.addElement(new Paragraph(etiqueta, SECUNDARIO));
            celda.addElement(new Paragraph(valor, NEGRITA));
            tabla.addCell(celda);
        });
        return tabla;
    }

    private PdfPTable detalle(Cotizacion cotizacion) {
        PdfPTable tabla = new PdfPTable(ANCHOS_DETALLE);
        tabla.setWidthPercentage(100f);
        tabla.setHeaderRows(1);
        for (String titulo : List.of("Servicio", "Unidad", "Cant.", "Precio unit.", "Descuento", "Subtotal")) {
            tabla.addCell(cabecera(titulo));
        }
        for (LineaCotizacion linea : cotizacion.getLineas()) {
            tabla.addCell(celda(linea.servicio(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(linea.unidadMedida(), Element.ALIGN_LEFT, NORMAL));
            tabla.addCell(celda(String.valueOf(linea.cantidad()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.precioUnitario()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.descuento()), Element.ALIGN_RIGHT, NORMAL));
            tabla.addCell(celda(numero(linea.subtotal()), Element.ALIGN_RIGHT, NORMAL));
        }
        PdfPCell etiquetaTotal = celda("TOTAL", Element.ALIGN_RIGHT, NEGRITA);
        etiquetaTotal.setColspan(ANCHOS_DETALLE.length - 1);
        etiquetaTotal.setBackgroundColor(GRIS_FONDO);
        tabla.addCell(etiquetaTotal);
        PdfPCell total = celda(importe(cotizacion.importeTotal()), Element.ALIGN_RIGHT, NEGRITA);
        total.setBackgroundColor(GRIS_FONDO);
        tabla.addCell(total);
        return tabla;
    }

    private PdfPTable cronograma(List<Evento> eventos) {
        PdfPTable tabla = new PdfPTable(ANCHOS_CRONOGRAMA);
        tabla.setWidthPercentage(100f);
        tabla.setHeaderRows(1);
        for (String titulo : List.of("Evento", "Lugar", "Horario")) {
            tabla.addCell(cabecera(titulo));
        }
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

    private static PdfPCell cabecera(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CABECERA_TABLA));
        celda.setBackgroundColor(ROJO);
        celda.setPadding(5f);
        celda.setBorderColor(ROJO);
        return celda;
    }

    private static PdfPCell celda(String texto, int alineacion, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5f);
        celda.setBorderColor(new Color(0xE0, 0xE0, 0xE0));
        return celda;
    }

    private static void espacio(Document documento) {
        documento.add(new Paragraph(" ", SECUNDARIO));
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

    private static String numero(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BOLIVIA));
        return formato.format(valor);
    }

    private static String importe(BigDecimal valor) {
        return MONEDA + numero(valor);
    }

    private static byte[] generar(ContenidoPdf contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, MARGEN, MARGEN, MARGEN, MARGEN);
        PdfWriter.getInstance(documento, salida);
        documento.open();
        try {
            contenido.escribir(documento);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static String leerPlantilla() {
        try (InputStream entrada = new ClassPathResource(PLANTILLA_CONTRATO).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la plantilla " + PLANTILLA_CONTRATO, e);
        }
    }

    @FunctionalInterface
    private interface ContenidoPdf {
        void escribir(Document documento);
    }
}
