package edu.univ.erp;

import edu.univ.erp.util.GradeScale;
import edu.univ.erp.util.MiniPdf;
import edu.univ.erp.util.TimeSlot;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilTest {

    @Test
    void parsesAndComparesTimeSlots() {
        TimeSlot t = TimeSlot.parse("Mon Wed 10:00-11:30");
        assertEquals("Mon Wed 10:00-11:30", t.toString());
        assertEquals("Tue Thu 09:00-10:30", TimeSlot.parse("tuesday,thu 9:00-10:30").toString());
        assertTrue(TimeSlot.clash("Mon Wed 10:00-11:30", "Wed 11:00-12:00"));
        assertFalse(TimeSlot.clash("Mon Wed 10:00-11:30", "Mon 11:30-12:00"), "back-to-back is fine");
        assertFalse(TimeSlot.clash("Mon 10:00-11:00", "Tue 10:00-11:00"));
        assertNull(TimeSlot.parse("Mon 11:00-10:00"));
        assertNull(TimeSlot.parse("whenever"));
    }

    @Test
    void gradeScale() {
        assertEquals("A+", GradeScale.letter(90));
        assertEquals("A", GradeScale.letter(89.9));
        assertEquals("F", GradeScale.letter(34.9));
        assertEquals(8, GradeScale.points("B"));
        assertFalse(GradeScale.isPassing("F"));
    }

    @Test
    void pdfIsWellFormed() {
        MiniPdf pdf = new MiniPdf();
        pdf.text(40, 40, "Hello (world) — café", MiniPdf.Font.BOLD, 12, Color.BLACK);
        pdf.newPage();
        pdf.rect(10, 10, 100, 20, Color.RED);
        String s = new String(pdf.toBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(s.startsWith("%PDF-1.4"));
        assertTrue(s.contains("/Count 2"));
        assertTrue(s.contains("Hello \\(world\\) \\227 caf\\351"));
        assertTrue(s.trim().endsWith("%%EOF"));
    }
}
