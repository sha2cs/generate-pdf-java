package com.sha2cs.generatepdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Generación de documentos PDF sencillos: título, imagen, campos y párrafos.
 *
 * <p>No depende de ninguna librería externa. Página A4, fuente Helvetica,
 * soporte de tildes y eñes, salto de página automático.</p>
 *
 * <pre>
 * try {
 *     File pdf = new DocumentoPDF("Ficha de usuario")
 *             .imagen(usuario.getRutaFoto())
 *             .campo("Nombre", usuario.getNombre())
 *             .campo("Correo", usuario.getCorreo())
 *             .guardar("usuario-" + usuario.getId() + ".pdf");
 * } catch (IOException e) {
 *     JOptionPane.showMessageDialog(this, "No se pudo generar el PDF: " + e.getMessage());
 * }
 * </pre>
 */
public class DocumentoPDF {

    private static final float ANCHO_PAGINA = 595.28f;
    private static final float ALTO_PAGINA = 841.89f;
    private static final float MARGEN = 56.69f;
    private static final float ANCHO_UTIL = ANCHO_PAGINA - 2 * MARGEN;

    private static final float TAM_TITULO = 18f;
    private static final float TAM_TEXTO = 11f;
    private static final float INTERLINEA_TITULO = 22f;
    private static final float INTERLINEA_TEXTO = 15f;

    private static final float IMAGEN_ANCHO_MAX = 160f;
    private static final float IMAGEN_ALTO_MAX = 200f;

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    /** Anchos de Helvetica (caracteres 32 a 126), en milésimas del tamaño de fuente. */
    private static final int[] ANCHOS_NORMAL = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
    };

    /** Anchos de Helvetica-Bold (caracteres 32 a 126). */
    private static final int[] ANCHOS_NEGRITA = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584
    };

    private final String titulo;
    private final List<Object[]> elementos = new ArrayList<>();

    // Estado del armado de páginas
    private List<StringBuilder> paginas;
    private StringBuilder pagina;
    private float y;

    /**
     * Crea un documento con el título indicado. El título encabeza la primera página.
     *
     * @param titulo título del documento
     */
    public DocumentoPDF(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    /**
     * Agrega un campo con formato «Etiqueta: valor», con la etiqueta en negrita.
     *
     * @param etiqueta nombre del campo, por ejemplo {@code "Nombre"}
     * @param valor    contenido del campo; {@code null} se escribe vacío
     * @return el mismo documento, para encadenar llamadas
     */
    public DocumentoPDF campo(String etiqueta, Object valor) {
        elementos.add(new Object[]{"campo", texto(etiqueta), valor == null ? "" : String.valueOf(valor)});
        return this;
    }

    /**
     * Agrega un párrafo de texto. Las líneas largas se ajustan al ancho de la página.
     *
     * @param texto contenido del párrafo
     * @return el mismo documento, para encadenar llamadas
     */
    public DocumentoPDF parrafo(String texto) {
        elementos.add(new Object[]{"parrafo", texto(texto)});
        return this;
    }

    /**
     * Agrega una imagen PNG o JPG, escalada a un máximo de 160 × 200 puntos con su proporción original.
     * El archivo se lee al llamar a {@link #guardar(String)}.
     *
     * @param ruta ruta del archivo de imagen
     * @return el mismo documento, para encadenar llamadas
     */
    public DocumentoPDF imagen(String ruta) {
        elementos.add(new Object[]{"imagen", ruta == null ? null : new File(ruta)});
        return this;
    }

    /**
     * Agrega una imagen PNG o JPG a partir de un {@link File}.
     *
     * @param archivo archivo de imagen
     * @return el mismo documento, para encadenar llamadas
     */
    public DocumentoPDF imagen(File archivo) {
        elementos.add(new Object[]{"imagen", archivo});
        return this;
    }

    /**
     * Genera el PDF y lo escribe en la ruta indicada.
     *
     * @param ruta ruta del archivo PDF, por ejemplo {@code "usuario-15.pdf"}
     * @return el archivo generado
     * @throws IOException si una imagen no existe o no es válida, o si el archivo no se puede escribir
     */
    public File guardar(String ruta) throws IOException {
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new IOException("La ruta del PDF está vacía");
        }
        return guardar(new File(ruta));
    }

    /**
     * Genera el PDF y lo escribe en el archivo indicado.
     *
     * @param archivo archivo PDF de destino
     * @return el archivo generado
     * @throws IOException si una imagen no existe o no es válida, o si el archivo no se puede escribir
     */
    public File guardar(File archivo) throws IOException {
        List<byte[]> imagenes = new ArrayList<>();
        armarPaginas(imagenes);
        try (OutputStream salida = new FileOutputStream(archivo)) {
            salida.write(construirPdf(imagenes));
        }
        return archivo;
    }

    // ------------------------------------------------------------------ armado

    private void armarPaginas(List<byte[]> imagenes) throws IOException {
        paginas = new ArrayList<>();
        nuevaPagina();

        for (String linea : ajustar(titulo, true, TAM_TITULO, ANCHO_UTIL)) {
            asegurarEspacio(INTERLINEA_TITULO);
            y -= INTERLINEA_TITULO;
            escribir(linea, true, TAM_TITULO, MARGEN);
        }
        y -= 14;

        for (Object[] e : elementos) {
            String tipo = (String) e[0];
            if ("campo".equals(tipo)) {
                armarCampo((String) e[1], (String) e[2]);
            } else if ("parrafo".equals(tipo)) {
                for (String linea : ajustar((String) e[1], false, TAM_TEXTO, ANCHO_UTIL)) {
                    asegurarEspacio(INTERLINEA_TEXTO);
                    y -= INTERLINEA_TEXTO;
                    escribir(linea, false, TAM_TEXTO, MARGEN);
                }
                y -= 6;
            } else {
                armarImagen((File) e[1], imagenes);
            }
        }
    }

    private void armarCampo(String etiqueta, String valor) {
        String rotulo = etiqueta + ": ";
        float anchoRotulo = ancho(rotulo, true, TAM_TEXTO);
        List<String> lineas = ajustar(valor, false, TAM_TEXTO, ANCHO_UTIL - anchoRotulo);
        for (int i = 0; i < lineas.size(); i++) {
            asegurarEspacio(INTERLINEA_TEXTO);
            y -= INTERLINEA_TEXTO;
            if (i == 0) {
                escribir(rotulo, true, TAM_TEXTO, MARGEN);
            }
            escribir(lineas.get(i), false, TAM_TEXTO, MARGEN + anchoRotulo);
        }
        y -= 3;
    }

    private void armarImagen(File archivo, List<byte[]> imagenes) throws IOException {
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("No se encontró la imagen: " + (archivo == null ? "(sin ruta)" : archivo.getPath()));
        }
        BufferedImage original = ImageIO.read(archivo);
        if (original == null) {
            throw new IOException("El archivo no es una imagen PNG o JPG: " + archivo.getPath());
        }

        // Fondo blanco para PNG con transparencia; el PDF guarda la imagen como JPG.
        BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(original, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream jpg = new ByteArrayOutputStream();
        if (!ImageIO.write(rgb, "jpg", jpg)) {
            throw new IOException("No se pudo convertir la imagen: " + archivo.getPath());
        }

        int numero = imagenes.size() + 1;
        imagenes.add(encabezadoImagen(rgb.getWidth(), rgb.getHeight(), jpg.toByteArray()));

        float escala = Math.min(1f, Math.min(IMAGEN_ANCHO_MAX / rgb.getWidth(), IMAGEN_ALTO_MAX / rgb.getHeight()));
        float ancho = rgb.getWidth() * escala;
        float alto = rgb.getHeight() * escala;

        asegurarEspacio(alto);
        y -= alto;
        pagina.append("q ").append(n(ancho)).append(" 0 0 ").append(n(alto)).append(' ')
              .append(n(MARGEN)).append(' ').append(n(y)).append(" cm /Im").append(numero).append(" Do Q\n");
        y -= 14;
    }

    private void nuevaPagina() {
        pagina = new StringBuilder();
        paginas.add(pagina);
        y = ALTO_PAGINA - MARGEN;
    }

    private void asegurarEspacio(float alto) {
        if (y - alto < MARGEN && pagina.length() > 0) {
            nuevaPagina();
        }
    }

    private void escribir(String texto, boolean negrita, float tam, float x) {
        if (texto.isEmpty()) {
            return;
        }
        pagina.append("BT /").append(negrita ? "F2 " : "F1 ").append(n(tam)).append(" Tf ")
              .append(n(x)).append(' ').append(n(y)).append(" Td (").append(literal(texto)).append(") Tj ET\n");
    }

    // ------------------------------------------------------------------ texto

    private static String texto(String s) {
        return s == null ? "" : s;
    }

    private static List<String> ajustar(String texto, boolean negrita, float tam, float anchoMax) {
        List<String> lineas = new ArrayList<>();
        for (String renglon : texto.split("\\R", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : renglon.trim().split(" +")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (actual.length() > 0 && ancho(prueba, negrita, tam) > anchoMax) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(prueba);
                }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static float ancho(String texto, boolean negrita, float tam) {
        int[] tabla = negrita ? ANCHOS_NEGRITA : ANCHOS_NORMAL;
        float total = 0;
        for (char c : texto.toCharArray()) {
            int codigo = c;
            if (codigo < 32 || codigo > 126) {
                String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                codigo = base.isEmpty() ? 'n' : base.charAt(0);
                if (codigo < 32 || codigo > 126) {
                    codigo = 'n';
                }
            }
            total += tabla[codigo - 32];
        }
        return total * tam / 1000f;
    }

    /** Cadena literal de PDF en codificación WinAnsi, con los bytes no ASCII en octal. */
    private static String literal(String texto) {
        StringBuilder sb = new StringBuilder();
        for (byte b : texto.getBytes(WIN_ANSI)) {
            int v = b & 0xFF;
            if (v == '(' || v == ')' || v == '\\') {
                sb.append('\\').append((char) v);
            } else if (v < 32 || v > 126) {
                sb.append('\\').append(String.format("%03o", v));
            } else {
                sb.append((char) v);
            }
        }
        return sb.toString();
    }

    /** Número con punto decimal, independiente del idioma del sistema. */
    private static String n(float valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    // ------------------------------------------------------------------ PDF

    private static byte[] encabezadoImagen(int ancho, int alto, byte[] jpg) {
        String dic = "<< /Type /XObject /Subtype /Image /Width " + ancho + " /Height " + alto
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpg.length + " >>\nstream\n";
        return unir(ascii(dic), jpg, ascii("\nendstream"));
    }

    private byte[] construirPdf(List<byte[]> imagenes) throws IOException {
        // Objetos: 1 catálogo, 2 páginas, 3 Helvetica, 4 Helvetica-Bold, 5 información,
        // luego las imágenes y, por cada página, la página y su contenido.
        List<byte[]> objetos = new ArrayList<>();
        int primeraImagen = 6;
        int primeraPagina = primeraImagen + imagenes.size();

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) {
            kids.append(primeraPagina + 2 * i).append(" 0 R ");
        }
        StringBuilder recursosImagen = new StringBuilder();
        for (int i = 0; i < imagenes.size(); i++) {
            recursosImagen.append("/Im").append(i + 1).append(' ').append(primeraImagen + i).append(" 0 R ");
        }

        objetos.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
        objetos.add(ascii("<< /Type /Pages /Kids [" + kids + "] /Count " + paginas.size() + " >>"));
        objetos.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objetos.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        objetos.add(ascii("<< /Title (" + literal(titulo) + ") /Producer (generate-pdf-java) >>"));
        objetos.addAll(imagenes);

        for (int i = 0; i < paginas.size(); i++) {
            int contenido = primeraPagina + 2 * i + 1;
            objetos.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(ANCHO_PAGINA) + " " + n(ALTO_PAGINA) + "]"
                    + " /Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << " + recursosImagen + ">> >>"
                    + " /Contents " + contenido + " 0 R >>"));
            byte[] flujo = ascii(paginas.get(i).toString());
            objetos.add(unir(ascii("<< /Length " + flujo.length + " >>\nstream\n"), flujo, ascii("\nendstream")));
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(ascii("%PDF-1.4\n%"));
        pdf.write(new byte[]{(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        int[] posiciones = new int[objetos.size()];
        for (int i = 0; i < objetos.size(); i++) {
            posiciones[i] = pdf.size();
            pdf.write(ascii((i + 1) + " 0 obj\n"));
            pdf.write(objetos.get(i));
            pdf.write(ascii("\nendobj\n"));
        }

        int inicioXref = pdf.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            xref.append(String.format("%010d 00000 n \n", posicion));
        }
        xref.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R /Info 5 0 R >>\n")
            .append("startxref\n").append(inicioXref).append("\n%%EOF\n");
        pdf.write(ascii(xref.toString()));
        return pdf.toByteArray();
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] unir(byte[]... partes) {
        ByteArrayOutputStream sb = new ByteArrayOutputStream();
        for (byte[] p : partes) {
            sb.write(p, 0, p.length);
        }
        return sb.toByteArray();
    }
}
