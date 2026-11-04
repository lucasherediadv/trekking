package com.example.desktop.service;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.desktop.Servicios;
import com.example.desktop.database.ConexionDB;
import com.example.core.salida.Salida;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Exports the current operational snapshot persisted for a Salida. */
public class FichaOperativaPDFService {
    private static final String REGULAR_FONT = "/com/example/fonts/OpenSans-Regular.ttf";
    private static final String BOLD_FONT = "/com/example/fonts/OpenSans-Bold.ttf";
    private static final float MARGIN = 42;
    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;
    private static final float CONTENT_BOTTOM = 56;
    private static final float BAND_H = 70;
    private static final float CELL_PAD = 6;
    private static final float OBS_LINE = 22;
    private static final int OBS_MIN_LINES = 5;
    private static final int BAND = 0x32321f, TABLE_HEAD = 0x454425, ACCENT = 0x6e6928, BORDER = 0xcfcab7,
            ZEBRA = 0xf2f1ec, MUTED = 0x625f50, INK = 0x32321f, EMPTY_ROW = 0xe6e4dc, WHITE = 0xffffff;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String nombreArchivo(Salida salida) {
        String nombreRuta = salida.rutaNombre().trim().replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (nombreRuta.isBlank()) nombreRuta = "Ruta";
        return nombreRuta + "-" + salida.fecha() + "-salida-" + salida.id() + ".pdf";
    }

    public void exportar(int salidaId, Path destino) throws SQLException, IOException {
        Ficha ficha = cargarFicha(salidaId);
        Path absoluto = destino.toAbsolutePath();
        Path directorio = absoluto.getParent();
        if (directorio == null) throw new IOException("La ruta de destino no tiene una carpeta.");
        Path temporal = Files.createTempFile(directorio, "ficha-", ".tmp");
        try {
            generarPDF(ficha, temporal);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private Ficha cargarFicha(int id) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            Salida salida;
            try {
                salida = Servicios.salidas().obtener(id);
            } catch (EntidadNoEncontradaException e) {
                throw new SQLException("La Salida seleccionada ya no existe.", e);
            }
            return new Ficha(salida, cargarCheckpoints(c, id), cargarEquipamiento(c, id), cargarParticipantes(c, id));
        }
    }

    private List<List<String>> cargarCheckpoints(Connection c, int id) throws SQLException {
        List<List<String>> filas = new ArrayList<>();
        String sql = "SELECT nombre,hora,latitud,longitud,altitud,descripcion FROM salida_checkpoints WHERE salida_id=? ORDER BY orden";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                int n = 1;
                while (rs.next()) filas.add(List.of(String.valueOf(n++), celda(rs.getString("nombre")),
                        celda(rs.getString("hora")),
                        String.format(Locale.ROOT, "%.5f, %.5f", rs.getDouble("latitud"), rs.getDouble("longitud")),
                        String.format(Locale.ROOT, "%.0f m", rs.getDouble("altitud")), celda(rs.getString("descripcion"))));
            }
        }
        return filas;
    }

    private List<List<String>> cargarEquipamiento(Connection c, int id) throws SQLException {
        List<List<String>> filas = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT nombre_copia,categoria_copia,cantidad FROM salida_equipamiento WHERE salida_id=? ORDER BY nombre_copia COLLATE NOCASE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) filas.add(List.of(celda(rs.getString("nombre_copia")),
                        celda(rs.getString("categoria_copia")), String.valueOf(rs.getInt("cantidad"))));
            }
        }
        return filas;
    }

    private List<List<String>> cargarParticipantes(Connection c, int id) throws SQLException {
        List<List<String>> filas = new ArrayList<>();
        String sql = "SELECT dni_copia,nombre_copia,apellido_copia,telefono_copia,emergencia_nombre_copia,emergencia_telefono_copia,emergencia_relacion_copia FROM salida_participantes WHERE salida_id=? ORDER BY apellido_copia COLLATE NOCASE,nombre_copia COLLATE NOCASE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String contacto = rs.getString("emergencia_nombre_copia");
                    String relacion = rs.getString("emergencia_relacion_copia");
                    List<String> emergencia = new ArrayList<>();
                    if (!vacio(contacto) || !vacio(relacion)) {
                        emergencia.add((vacio(contacto) ? "" : contacto.trim()) + (vacio(relacion) ? "" : " (" + relacion.trim() + ")"));
                    }
                    if (!vacio(rs.getString("emergencia_telefono_copia"))) emergencia.add(rs.getString("emergencia_telefono_copia").trim());
                    List<String> nombre = new ArrayList<>();
                    if (!vacio(rs.getString("apellido_copia"))) nombre.add(rs.getString("apellido_copia").trim());
                    if (!vacio(rs.getString("nombre_copia"))) nombre.add(rs.getString("nombre_copia").trim());
                    filas.add(List.of(celda(rs.getString("dni_copia")), nombre.isEmpty() ? "—" : String.join(", ", nombre),
                            celda(rs.getString("telefono_copia")), emergencia.isEmpty() ? "—" : String.join("\n", emergencia), ""));
                }
            }
        }
        return filas;
    }

    private void generarPDF(Ficha ficha, Path destino) throws IOException {
        try (PDDocument documento = new PDDocument();
             InputStream regularStream = getClass().getResourceAsStream(REGULAR_FONT);
             InputStream boldStream = getClass().getResourceAsStream(BOLD_FONT)) {
            if (regularStream == null || boldStream == null) throw new IOException("No se encontraron las fuentes incluidas para el PDF.");
            PDType0Font regular = PDType0Font.load(documento, regularStream);
            PDType0Font bold = PDType0Font.load(documento, boldStream);
            Render r = new Render(documento, regular, bold);
            Salida s = ficha.salida();
            r.newPage();
            r.cabecera(s.fecha().format(FECHA));
            r.tarjetas(
                    new Card("Salida", List.of(new Field("Identificador", String.valueOf(s.id()), false),
                            new Field("Fecha", s.fecha().format(FECHA), false), new Field("Estado", s.estado().etiqueta(), true))),
                    new Card("Ruta", List.of(new Field("Nombre", valor(s.rutaNombre()), false),
                            new Field("Altitud máxima", String.format(Locale.ROOT, "%.0f m", s.rutaAltitudMaxima()), false),
                            new Field("Tipo de terreno", valor(s.rutaTipoTerreno()), false),
                            new Field("Dificultad técnica", valor(s.rutaDificultadTecnica()), false),
                            new Field("Dificultad física", valor(s.rutaDificultadFisica()), false))));
            r.tabla("Checkpoints", List.of(new Col("Orden", .07f, false, 8.5f, false), new Col("Nombre", .22f, false, 8.5f, false),
                    new Col("Horario", .10f, false, 8.5f, false), new Col("Coordenadas", .20f, true, 7.5f, false),
                    new Col("Altitud", .10f, true, 8.5f, false), new Col("Descripción", .31f, false, 8.5f, false)),
                    ficha.checkpoints(), "Sin checkpoints registrados");
            r.tabla("Equipamiento", List.of(new Col("Elemento", .5f, false, 8.5f, false), new Col("Categoría", .3f, false, 8.5f, false),
                    new Col("Cantidad", .2f, true, 8.5f, false)), ficha.equipamiento(), "Sin equipamiento asignado");
            r.tabla("Participantes", List.of(new Col("DNI", .13f, false, 8.5f, false), new Col("Nombre y apellido", .27f, false, 8.5f, false),
                    new Col("Teléfono", .16f, false, 8.5f, false), new Col("Contacto de emergencia", .32f, false, 8.5f, false),
                    new Col("Presente", .12f, false, 8.5f, true)), ficha.participantes(), "Sin participantes");
            r.observaciones();
            r.finish(valor(s.rutaNombre()), s.fecha().format(FECHA), LocalDateTime.now().format(FECHA_HORA));
            documento.save(destino.toFile());
        }
    }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private static String valor(String valor) { return vacio(valor) ? "No especificado" : valor.trim(); }
    private static String celda(String valor) { return vacio(valor) ? "—" : valor.trim(); }

    private record Ficha(Salida salida, List<List<String>> checkpoints, List<List<String>> equipamiento,
                         List<List<String>> participantes) {}
    private record Col(String titulo, float peso, boolean derecha, float size, boolean casilla) {}
    private record Field(String etiqueta, String valor, boolean estado) {}
    private record Card(String titulo, List<Field> campos) {}

    private static final class Render {
        private final PDDocument doc;
        private final PDType0Font regular;
        private final PDType0Font bold;
        private PDPageContentStream stream;
        private PDPage page;
        private float y;

        Render(PDDocument doc, PDType0Font regular, PDType0Font bold) { this.doc = doc; this.regular = regular; this.bold = bold; }

        void newPage() throws IOException {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;
        }

        void cabecera(String fecha) throws IOException {
            fill(0, PAGE_H - BAND_H, PAGE_W, BAND_H, BAND);
            float icon = 30, scale = icon / 24f, ox = MARGIN, oy = PAGE_H - BAND_H / 2 + icon / 2;
            stream.saveGraphicsState();
            stroke(WHITE);
            stream.setLineWidth(2 * scale);
            stream.setLineCapStyle(1);
            stream.setLineJoinStyle(1);
            stream.moveTo(ox + 8 * scale, oy - 3 * scale);
            stream.lineTo(ox + 12 * scale, oy - 11 * scale);
            stream.lineTo(ox + 17 * scale, oy - 6 * scale);
            stream.lineTo(ox + 22 * scale, oy - 21 * scale);
            stream.lineTo(ox + 2 * scale, oy - 21 * scale);
            stream.closePath();
            stream.stroke();
            stream.moveTo(ox + 4.14f * scale, oy - 15.08f * scale);
            stream.curveTo(ox + 6.76f * scale, oy - 13.51f * scale, ox + 9.38f * scale, oy - 13.65f * scale, ox + 12 * scale, oy - 15.5f * scale);
            stream.curveTo(ox + 14.74f * scale, oy - 17.44f * scale, ox + 17.49f * scale, oy - 17.5f * scale, ox + 20.23f * scale, oy - 15.69f * scale);
            stream.stroke();
            stream.restoreGraphicsState();
            text("TrekRisk", bold, 20, MARGIN + icon + 10, PAGE_H - BAND_H / 2 - 7, WHITE);
            textRight("Ficha operativa", bold, 15, PAGE_W - MARGIN, PAGE_H - BAND_H / 2 + 2, WHITE);
            textRight(fecha, regular, 10, PAGE_W - MARGIN, PAGE_H - BAND_H / 2 - 14, WHITE);
            y = PAGE_H - BAND_H - 18;
        }

        void tarjetas(Card izquierda, Card derecha) throws IOException {
            float gap = 12, w = (CONTENT_W - gap) / 2, labelW = 86, valueW = w - 16 - labelW;
            float h = Math.max(altoTarjeta(izquierda, valueW), altoTarjeta(derecha, valueW));
            ensure(h);
            dibujarTarjeta(izquierda, MARGIN, w, h, labelW, valueW);
            dibujarTarjeta(derecha, MARGIN + w + gap, w, h, labelW, valueW);
            y -= h + 6;
        }

        private float altoTarjeta(Card card, float valueW) throws IOException {
            float h = 16 + 18;
            for (Field f : card.campos()) h += f.estado() ? 18 : wrap(f.valor(), bold, 9, valueW).size() * 12 + 4;
            return h;
        }

        private void dibujarTarjeta(Card card, float x, float w, float h, float labelW, float valueW) throws IOException {
            fill(x, y - h, w, h, WHITE);
            stream.setLineWidth(0.75f);
            stroke(BORDER);
            stream.addRect(x, y - h, w, h);
            stream.stroke();
            text(card.titulo(), bold, 10.5f, x + 8, y - 8 - 10, ACCENT);
            float cy = y - 34;
            for (Field f : card.campos()) {
                text(f.etiqueta(), regular, 8.5f, x + 8, cy - 9, MUTED);
                if (f.estado()) {
                    String t = f.valor().substring(0, 1).toUpperCase(Locale.ROOT) + f.valor().substring(1);
                    float tw = bold.getStringWidth(t) * 8 / 1000 + 12;
                    fill(x + 8 + labelW, cy - 13, tw, 15, colorEstado(f.valor()));
                    text(t, bold, 8, x + 8 + labelW + 6, cy - 9, WHITE);
                    cy -= 18;
                } else {
                    List<String> lines = wrap(f.valor(), bold, 9, valueW);
                    for (int i = 0; i < lines.size(); i++) text(lines.get(i), bold, 9, x + 8 + labelW, cy - 9 - i * 12, INK);
                    cy -= lines.size() * 12 + 4;
                }
            }
        }

        private static int colorEstado(String estado) {
            return switch (estado.trim().toLowerCase(Locale.ROOT)) {
                case "planificada" -> 0x1a6fb0;
                case "realizada" -> 0x5f7a2e;
                case "cancelada" -> 0xd93025;
                default -> 0xe37400;
            };
        }

        void seccion(String titulo, float minimoDebajo) throws IOException {
            ensure(36 + minimoDebajo);
            y -= 12;
            text(titulo, bold, 12, MARGIN, y - 10, ACCENT);
            y -= 16;
            stroke(BORDER);
            stream.setLineWidth(0.75f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(MARGIN + CONTENT_W, y);
            stream.stroke();
            y -= 6;
        }

        void tabla(String titulo, List<Col> cols, List<List<String>> filas, String vacio) throws IOException {
            float[] anchos = new float[cols.size()];
            for (int i = 0; i < anchos.length; i++) anchos[i] = cols.get(i).peso() * CONTENT_W;
            float headH = 8.5f * 1.35f + 2 * CELL_PAD;
            float primera = filas.isEmpty() ? 8.5f * 1.35f + 2 * CELL_PAD : altoFila(cols, anchos, filas.get(0));
            seccion(titulo, headH + primera);
            if (filas.isEmpty()) {
                float h = 8.5f * 1.35f + 2 * CELL_PAD;
                fill(MARGIN, y - h, CONTENT_W, h, EMPTY_ROW);
                text(vacio, regular, 8.5f, MARGIN + CELL_PAD, y - CELL_PAD - 8.5f, MUTED);
                y -= h;
                return;
            }
            dibujarEncabezado(cols, anchos);
            for (int n = 0; n < filas.size(); n++) {
                List<String> fila = filas.get(n);
                float h = altoFila(cols, anchos, fila);
                if (y - h < CONTENT_BOTTOM) {
                    newPage();
                    dibujarEncabezado(cols, anchos);
                }
                if (n % 2 == 1) fill(MARGIN, y - h, CONTENT_W, h, ZEBRA);
                float x = MARGIN;
                for (int i = 0; i < cols.size(); i++) {
                    Col col = cols.get(i);
                    if (col.casilla()) {
                        stroke(INK);
                        stream.setLineWidth(0.75f);
                        stream.addRect(x + anchos[i] / 2 - 5, y - CELL_PAD - 10, 10, 10);
                        stream.stroke();
                    } else {
                        List<String> lines = wrap(fila.get(i), regular, col.size(), anchos[i] - 2 * CELL_PAD);
                        for (int k = 0; k < lines.size(); k++) {
                            float by = y - CELL_PAD - col.size() - k * col.size() * 1.35f;
                            if (col.derecha()) textRight(lines.get(k), regular, col.size(), x + anchos[i] - CELL_PAD, by, INK);
                            else text(lines.get(k), regular, col.size(), x + CELL_PAD, by, INK);
                        }
                    }
                    x += anchos[i];
                }
                stroke(BORDER);
                stream.setLineWidth(0.5f);
                stream.moveTo(MARGIN, y - h);
                stream.lineTo(MARGIN + CONTENT_W, y - h);
                stream.stroke();
                y -= h;
            }
        }

        private float altoFila(List<Col> cols, float[] anchos, List<String> fila) throws IOException {
            float alto = 10;
            for (int i = 0; i < cols.size(); i++) {
                if (cols.get(i).casilla()) continue;
                float size = cols.get(i).size();
                alto = Math.max(alto, wrap(fila.get(i), regular, size, anchos[i] - 2 * CELL_PAD).size() * size * 1.35f);
            }
            return alto + 2 * CELL_PAD;
        }

        private void dibujarEncabezado(List<Col> cols, float[] anchos) throws IOException {
            float h = 8.5f * 1.35f + 2 * CELL_PAD;
            fill(MARGIN, y - h, CONTENT_W, h, TABLE_HEAD);
            float x = MARGIN;
            for (int i = 0; i < cols.size(); i++) {
                Col col = cols.get(i);
                float by = y - CELL_PAD - 8.5f;
                if (col.derecha()) textRight(col.titulo(), bold, 8.5f, x + anchos[i] - CELL_PAD, by, WHITE);
                else text(col.titulo(), bold, 8.5f, x + CELL_PAD, by, WHITE);
                x += anchos[i];
            }
            y -= h;
        }

        void observaciones() throws IOException {
            seccion("Observaciones", OBS_MIN_LINES * OBS_LINE + 16);
            int lineas = Math.max(OBS_MIN_LINES, (int) ((y - CONTENT_BOTTOM - 16) / OBS_LINE));
            stroke(BORDER);
            stream.setLineWidth(0.75f);
            for (int i = 0; i < lineas; i++) {
                y -= OBS_LINE;
                stream.moveTo(MARGIN, y);
                stream.lineTo(MARGIN + CONTENT_W, y);
                stream.stroke();
            }
            y -= 12;
            text("Los adjuntos médicos no se incorporan ni interpretan en esta ficha.", regular, 8, MARGIN, y, MUTED);
        }

        void finish(String ruta, String fechaSalida, String generada) throws IOException {
            stream.close();
            stream = null;
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                try (PDPageContentStream pie = new PDPageContentStream(doc, doc.getPage(i), AppendMode.APPEND, true, true)) {
                    stream = pie;
                    stroke(BORDER);
                    stream.setLineWidth(0.75f);
                    stream.moveTo(MARGIN, 40);
                    stream.lineTo(MARGIN + CONTENT_W, 40);
                    stream.stroke();
                    String numero = "Página " + (i + 1) + " de " + total;
                    float espacio = CONTENT_W - bold.getStringWidth(numero) * 8.5f / 1000 - 12;
                    text(recortar(ruta + " · Salida del " + fechaSalida, regular, 8, espacio), regular, 8, MARGIN, 28, MUTED);
                    textRight(numero, bold, 8.5f, MARGIN + CONTENT_W, 28, MUTED);
                    text("Generada el " + generada, regular, 8, MARGIN, 17, MUTED);
                }
            }
            stream = null;
        }

        private String recortar(String texto, PDType0Font font, float size, float max) throws IOException {
            String t = limpiar(texto);
            if (font.getStringWidth(t) * size / 1000 <= max) return t;
            while (t.length() > 1 && font.getStringWidth(t + "…") * size / 1000 > max) t = t.substring(0, t.length() - 1);
            return t + "…";
        }

        private void ensure(float height) throws IOException {
            if (y - height < CONTENT_BOTTOM) newPage();
        }

        private static float[] rgb(int c) { return new float[] {((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f}; }
        private void fill(float x, float yy, float w, float h, int color) throws IOException {
            float[] c = rgb(color);
            stream.setNonStrokingColor(c[0], c[1], c[2]);
            stream.addRect(x, yy, w, h);
            stream.fill();
        }
        private void stroke(int color) throws IOException { float[] c = rgb(color); stream.setStrokingColor(c[0], c[1], c[2]); }
        private void text(String t, PDType0Font font, float size, float x, float yy, int color) throws IOException {
            float[] c = rgb(color);
            stream.setNonStrokingColor(c[0], c[1], c[2]);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, yy);
            stream.showText(limpiar(t));
            stream.endText();
        }
        private void textRight(String t, PDType0Font font, float size, float right, float yy, int color) throws IOException {
            text(t, font, size, right - font.getStringWidth(limpiar(t)) * size / 1000, yy, color);
        }
        private static String limpiar(String t) { return t.replaceAll("[\\p{Cntrl}]", " "); }

        private List<String> wrap(String text, PDType0Font font, float size, float max) throws IOException {
            List<String> result = new ArrayList<>();
            for (String paragraph : text.split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.trim().split("\\s+")) {
                    List<String> pieces = partir(word, font, size, max);
                    for (int i = 0; i < pieces.size(); i++) {
                        String piece = pieces.get(i);
                        if (i > 0) {
                            result.add(line.toString());
                            line.setLength(0);
                        }
                        String candidate = line.isEmpty() ? piece : line + " " + piece;
                        if (!line.isEmpty() && font.getStringWidth(limpiar(candidate)) * size / 1000 > max) {
                            result.add(line.toString());
                            candidate = piece;
                        }
                        line.setLength(0);
                        line.append(candidate);
                    }
                }
                result.add(line.toString());
            }
            return result;
        }

        private List<String> partir(String word, PDType0Font font, float size, float max) throws IOException {
            List<String> parts = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            for (char ch : word.toCharArray()) {
                if (cur.length() > 0 && font.getStringWidth(cur.toString() + ch) * size / 1000 > max) {
                    parts.add(cur.toString());
                    cur.setLength(0);
                }
                cur.append(ch);
            }
            parts.add(cur.toString());
            return parts;
        }
    }
}
