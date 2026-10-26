package edu.univ.erp.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small dependency-free PDF writer: A4 pages, the two standard Helvetica fonts,
 * filled rectangles, lines and text. Enough for transcripts and reports.
 * Coordinates are in points from the top-left corner of the page.
 */
public final class MiniPdf {

    public static final float WIDTH = 595.28f;
    public static final float HEIGHT = 841.89f;

    public enum Font { REGULAR, BOLD }

    /** Helvetica advance widths (1/1000 em) for ASCII 32..126. */
    private static final int[] HELVETICA = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};

    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder page;

    public MiniPdf() {
        newPage();
    }

    public void newPage() {
        page = new StringBuilder();
        pages.add(page);
    }

    public int pageCount() {
        return pages.size();
    }

    /** Selects a page (0-based) for further drawing, e.g. to add footers afterwards. */
    public void page(int index) {
        page = pages.get(index);
    }

    private static String n(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private void color(Color c, boolean fill) {
        page.append(n(c.getRed() / 255f)).append(' ').append(n(c.getGreen() / 255f)).append(' ')
                .append(n(c.getBlue() / 255f)).append(fill ? " rg\n" : " RG\n");
    }

    public void rect(float x, float y, float w, float h, Color fill) {
        color(fill, true);
        page.append(n(x)).append(' ').append(n(HEIGHT - y - h)).append(' ').append(n(w)).append(' ')
                .append(n(h)).append(" re f\n");
    }

    public void line(float x1, float y1, float x2, float y2, float width, Color c) {
        color(c, false);
        page.append(n(width)).append(" w ").append(n(x1)).append(' ').append(n(HEIGHT - y1)).append(" m ")
                .append(n(x2)).append(' ').append(n(HEIGHT - y2)).append(" l S\n");
    }

    /** Draws text with its baseline at y. */
    public void text(float x, float y, String s, Font font, float size, Color c) {
        color(c, true);
        page.append("BT /").append(font == Font.BOLD ? "F2" : "F1").append(' ').append(n(size)).append(" Tf ")
                .append(n(x)).append(' ').append(n(HEIGHT - y)).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    public void textRight(float right, float y, String s, Font font, float size, Color c) {
        text(right - width(s, font, size), y, s, font, size, c);
    }

    public void textCenter(float center, float y, String s, Font font, float size, Color c) {
        text(center - width(s, font, size) / 2, y, s, font, size, c);
    }

    public static float width(String s, Font font, float size) {
        float w = 0;
        for (char ch : s.toCharArray()) {
            w += ch >= 32 && ch <= 126 ? HELVETICA[ch - 32] : 556;
        }
        return w * size / 1000f * (font == Font.BOLD ? 1.06f : 1f);
    }

    /** Shortens text with an ellipsis so it fits in maxWidth. */
    public static String fit(String s, Font font, float size, float maxWidth) {
        if (s == null) return "";
        if (width(s, font, size) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && width(t + "...", font, size) > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            int b = winAnsi(ch);
            if (b == '(' || b == ')' || b == '\\') sb.append('\\').append((char) b);
            else if (b < 32 || b > 126) sb.append(String.format("\\%03o", b));
            else sb.append((char) b);
        }
        return sb.toString();
    }

    private static int winAnsi(char ch) {
        switch (ch) {
            case '–': return 0x96;
            case '—': return 0x97;
            case '•': return 0x95;
            case '‘': return 0x91;
            case '’': return 0x92;
            case '“': return 0x93;
            case '”': return 0x94;
            case '€': return 0x80;
            default:
                if (ch < 32) return ' ';
                if (ch < 256 && (ch < 0x80 || ch > 0x9F)) return ch;
                return '?';
        }
    }

    public void save(Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(toBytes());
        }
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int pageCount = pages.size();
        // Object numbers: 1 catalog, 2 pages, 3 F1, 4 F2, then (page, content) pairs
        write(out, "%PDF-1.4\n%âãÏÓ\n");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) kids.append(5 + i * 2).append(" 0 R ");

        obj(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < pageCount; i++) {
            obj(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(WIDTH) + " " + n(HEIGHT) + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + i * 2) + " 0 R >>");
            byte[] content = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.write(content, 0, content.length);
            write(out, "\nendstream\nendobj\n");
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) x.append(String.format("%010d 00000 n \n", off));
        x.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        write(out, x.toString());
        return out.toByteArray();
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }
}
