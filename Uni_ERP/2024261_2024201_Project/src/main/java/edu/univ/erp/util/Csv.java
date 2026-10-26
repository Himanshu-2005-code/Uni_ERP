package edu.univ.erp.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal RFC 4180 CSV writer (quotes fields containing commas, quotes or newlines). */
public final class Csv {

    private Csv() {
    }

    public static void write(Path file, List<String> header, List<List<Object>> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿'); // BOM so Excel opens UTF-8 correctly
            line(w, header);
            for (List<Object> r : rows) {
                line(w, r);
            }
        }
    }

    private static void line(Writer w, List<?> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) w.write(',');
            w.write(escape(cells.get(i)));
        }
        w.write("\r\n");
    }

    static String escape(Object o) {
        String s = o == null ? "" : String.valueOf(o);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
