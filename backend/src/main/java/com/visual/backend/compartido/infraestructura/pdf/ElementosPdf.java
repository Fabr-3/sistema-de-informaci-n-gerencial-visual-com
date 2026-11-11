package com.visual.backend.compartido.infraestructura.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
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
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;

import com.visual.backend.compartido.infraestructura.configuracion.PropiedadesEmpresa;

/**
 * Identidad visual comun de los documentos PDF (rojo corporativo): fuentes,
 * encabezado con los datos de la empresa, tablas y formatos de Bolivia.
 */
public final class ElementosPdf {

    public static final Color ROJO = new Color(0xB7, 0x1C, 0x1C);
    public static final Color GRIS_TEXTO = new Color(0x5F, 0x63, 0x68);
    public static final Color GRIS_FONDO = new Color(0xF5, 0xF5, 0xF5);
    private static final Color GRIS_BORDE = new Color(0xE0, 0xE0, 0xE0);
    public static final Locale BOLIVIA = Locale.forLanguageTag("es-BO");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MONEDA = "Bs ";
    private static final float MARGEN = 50f;
    private static final float PADDING_CELDA = 5f;

    public static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, ROJO);
    public static final Font MARCA = new Font(Font.HELVETICA, 18, Font.BOLD, ROJO);
    public static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    public static final Font NORMAL = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, Color.BLACK);
    public static final Font NEGRITA = new Font(Font.HELVETICA, 9.5f, Font.BOLD, Color.BLACK);
    public static final Font SECUNDARIO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, GRIS_TEXTO);
    public static final Font CABECERA_TABLA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);

    private ElementosPdf() {
    }

    /** Razon social, datos de contacto, linea roja y titulo del documento. */
    public static void encabezado(Document documento, PropiedadesEmpresa empresa, String titulo) {
        documento.add(new Paragraph(empresa.razonSocial(), MARCA));
        String contacto = String.join("  ·  ", Stream.of(empresa.direccion(), empresa.ciudad(),
                empresa.telefono(), empresa.email(), empresa.nit() == null || empresa.nit().isBlank() ? null
                        : "NIT " + empresa.nit())
                .filter(valor -> valor != null && !valor.isBlank()).toList());
        documento.add(new Paragraph(contacto, SECUNDARIO));
        documento.add(new Paragraph(new Chunk(new LineSeparator(1f, 100f, ROJO, Element.ALIGN_CENTER, -4f))));
        espacio(documento);
        Paragraph encabezado = new Paragraph(titulo, TITULO);
        encabezado.setSpacingAfter(10f);
        documento.add(encabezado);
    }

    /** Tabla de pares etiqueta/valor, una columna por par, en el orden recibido. */
    public static PdfPTable datos(Map<String, String> pares) {
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

    /** Tabla de ancho completo con su fila de titulos, que se repite en cada pagina. */
    public static PdfPTable tabla(float[] anchos, String... titulos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100f);
        tabla.setHeaderRows(1);
        for (String titulo : titulos) {
            tabla.addCell(cabecera(titulo));
        }
        return tabla;
    }

    public static PdfPCell cabecera(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CABECERA_TABLA));
        celda.setBackgroundColor(ROJO);
        celda.setPadding(PADDING_CELDA);
        celda.setBorderColor(ROJO);
        return celda;
    }

    public static PdfPCell celda(String texto, int alineacion, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(PADDING_CELDA);
        celda.setBorderColor(GRIS_BORDE);
        return celda;
    }

    /** Celda resaltada para totales. */
    public static PdfPCell celdaTotal(String texto, int alineacion, int columnas) {
        PdfPCell celda = celda(texto, alineacion, NEGRITA);
        celda.setColspan(columnas);
        celda.setBackgroundColor(GRIS_FONDO);
        return celda;
    }

    public static void espacio(Document documento) {
        documento.add(new Paragraph(" ", SECUNDARIO));
    }

    public static String numero(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BOLIVIA));
        return formato.format(valor);
    }

    public static String importe(BigDecimal valor) {
        return MONEDA + numero(valor);
    }

    /** Documento tamano carta vertical. */
    public static byte[] generar(ContenidoPdf contenido) {
        return generar(PageSize.LETTER, null, contenido);
    }

    /** pie: texto opcional que se imprime al pie de cada pagina junto al numero de pagina. */
    public static byte[] generar(Rectangle tamano, String pie, ContenidoPdf contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(tamano, MARGEN, MARGEN, MARGEN, MARGEN);
        PdfWriter escritor = PdfWriter.getInstance(documento, salida);
        if (pie != null) {
            escritor.setPageEvent(new PiePagina(pie));
        }
        documento.open();
        try {
            contenido.escribir(documento);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    @FunctionalInterface
    public interface ContenidoPdf {
        void escribir(Document documento);
    }

    /** "texto · Página n" centrado en el margen inferior. */
    private static final class PiePagina extends PdfPageEventHelper {

        private static final float DISTANCIA_PIE = 25f;

        private final String texto;

        private PiePagina(String texto) {
            this.texto = texto;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            Phrase pie = new Phrase(texto + "  ·  Página " + escritor.getPageNumber(), SECUNDARIO);
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_CENTER, pie,
                    (documento.left() + documento.right()) / 2, documento.bottom() - DISTANCIA_PIE, 0);
        }
    }
}
