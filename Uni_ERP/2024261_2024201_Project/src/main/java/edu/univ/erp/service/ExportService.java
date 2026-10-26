package edu.univ.erp.service;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.domain.StudentProfile;
import edu.univ.erp.util.Csv;
import edu.univ.erp.util.GradeScale;
import edu.univ.erp.util.MiniPdf;
import edu.univ.erp.util.MiniPdf.Font;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Student transcripts and grade reports as PDF or CSV. */
public final class ExportService {

    private static final Color INK = new Color(0x1B, 0x1F, 0x3B);
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    private static final Color ACCENT = new Color(0x4F, 0x46, 0xE5);
    private static final Color ACCENT_SOFT = new Color(0xEE, 0xF0, 0xFF);
    private static final Color RULE = new Color(0xE5, 0xE7, 0xEB);

    private ExportService() {
    }

    public static void transcriptPdf(Path file) throws IOException {
        StudentProfile p = StudentService.profile();
        List<Enrollment> all = StudentService.enrollments();
        StudentService.GpaSummary gpa = StudentService.gpa(all);
        String institution = SettingsService.get().institution();

        Map<String, List<Enrollment>> byTerm = new LinkedHashMap<>();
        all.stream()
                .filter(e -> !"DROPPED".equals(e.status()))
                .sorted((a, b) -> a.section().year() != b.section().year()
                        ? Integer.compare(a.section().year(), b.section().year())
                        : StudentService.termOrder(a.section().semester()) - StudentService.termOrder(b.section().semester()))
                .forEach(e -> byTerm.computeIfAbsent(e.section().term(), k -> new ArrayList<>()).add(e));

        MiniPdf pdf = new MiniPdf();
        float left = 48;
        float right = MiniPdf.WIDTH - 48;
        float y = header(pdf, institution);

        // Student block
        pdf.rect(left, y, right - left, 74, ACCENT_SOFT);
        pdf.text(left + 16, y + 26, p.displayName(), Font.BOLD, 16, INK);
        pdf.text(left + 16, y + 46, "Roll No. " + p.rollNo() + "   •   " + nz(p.program()) + "   •   Batch "
                + p.year(), Font.REGULAR, 10, MUTED);
        pdf.text(left + 16, y + 62, "Username: " + p.username(), Font.REGULAR, 9, MUTED);
        pdf.textRight(right - 16, y + 30, String.format("%.2f", gpa.cgpa()), Font.BOLD, 24, ACCENT);
        pdf.textRight(right - 16, y + 48, "CGPA (10-point scale)", Font.REGULAR, 8.5f, MUTED);
        pdf.textRight(right - 16, y + 62, gpa.creditsEarned() + " credits earned", Font.REGULAR, 8.5f, MUTED);
        y += 100;

        float[] cols = {left, left + 72, right - 150, right - 95, right - 45};
        for (var term : byTerm.entrySet()) {
            if (y > MiniPdf.HEIGHT - 140) {
                pdf.newPage();
                y = header(pdf, institution);
            }
            pdf.text(left, y, term.getKey(), Font.BOLD, 12, INK);
            Double sgpa = gpa.termGpa().get(term.getKey());
            if (sgpa != null) pdf.textRight(right, y, "Term GPA " + String.format("%.2f", sgpa), Font.BOLD, 10, ACCENT);
            y += 10;
            pdf.line(left, y, right, y, 0.8f, RULE);
            y += 14;
            String[] head = {"Code", "Course", "Credits", "Grade", "Points"};
            for (int i = 0; i < head.length; i++) {
                if (i >= 2) pdf.textRight(i == 4 ? right : cols[i + 1] - 12, y, head[i], Font.BOLD, 8.5f, MUTED);
                else pdf.text(cols[i], y, head[i], Font.BOLD, 8.5f, MUTED);
            }
            y += 16;
            for (Enrollment e : term.getValue()) {
                if (y > MiniPdf.HEIGHT - 80) {
                    pdf.newPage();
                    y = header(pdf, institution);
                }
                String grade = e.hasFinal() ? e.finalGrade() : "IP";
                String points = e.hasFinal() ? String.format("%.0f", GradeScale.points(e.finalGrade())) : "–";
                pdf.text(cols[0], y, e.section().courseCode(), Font.BOLD, 9.5f, INK);
                pdf.text(cols[1], y, MiniPdf.fit(e.section().courseTitle(), Font.REGULAR, 9.5f, cols[2] - cols[1] - 16),
                        Font.REGULAR, 9.5f, INK);
                pdf.textRight(cols[3] - 12, y, String.valueOf(e.section().credits()), Font.REGULAR, 9.5f, INK);
                pdf.textRight(cols[4] - 12, y, grade, Font.BOLD, 9.5f, e.hasFinal() ? INK : MUTED);
                pdf.textRight(right, y, points, Font.REGULAR, 9.5f, INK);
                y += 8;
                pdf.line(left, y, right, y, 0.4f, RULE);
                y += 13;
            }
            y += 14;
        }
        if (byTerm.isEmpty()) {
            pdf.text(left, y, "No courses on record yet.", Font.REGULAR, 11, MUTED);
        }

        // Grade key and footers on every page
        String generated = "Generated " + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                + "  •  IP = in progress  •  A+/A 10, A- 9, B 8, B- 7, C 6, C- 5, D 4, F 0";
        for (int i = 0; i < pdf.pageCount(); i++) {
            pdf.page(i);
            pdf.line(left, MiniPdf.HEIGHT - 44, right, MiniPdf.HEIGHT - 44, 0.5f, RULE);
            pdf.text(left, MiniPdf.HEIGHT - 30, generated, Font.REGULAR, 7.5f, MUTED);
            pdf.textRight(right, MiniPdf.HEIGHT - 30, "Page " + (i + 1) + " of " + pdf.pageCount(), Font.REGULAR, 7.5f, MUTED);
        }
        pdf.save(file);
    }

    private static float header(MiniPdf pdf, String institution) {
        pdf.rect(0, 0, MiniPdf.WIDTH, 86, INK);
        pdf.rect(0, 86, MiniPdf.WIDTH, 4, ACCENT);
        pdf.text(48, 42, institution, Font.BOLD, 20, Color.WHITE);
        pdf.text(48, 62, "Academic Transcript", Font.REGULAR, 11, new Color(0xC7, 0xCB, 0xF5));
        return 116;
    }

    public static void transcriptCsv(Path file) throws IOException {
        StudentProfile p = StudentService.profile();
        List<List<Object>> rows = new ArrayList<>();
        for (Enrollment e : StudentService.enrollments()) {
            if ("DROPPED".equals(e.status())) continue;
            rows.add(List.of(p.rollNo(), p.displayName(), e.section().term(), e.section().courseCode(),
                    e.section().courseTitle(), e.section().credits(), e.hasFinal() ? e.finalGrade() : "In progress",
                    e.finalPercentage() == null ? "" : String.format("%.1f", e.finalPercentage()),
                    e.hasFinal() ? String.format("%.0f", GradeScale.points(e.finalGrade())) : ""));
        }
        Csv.write(file, List.of("Roll No", "Name", "Term", "Code", "Course", "Credits", "Grade", "Final %", "Points"),
                rows);
    }

    /** Every assessment score for the student's courses. */
    public static void gradeReportCsv(Path file) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        for (StudentService.CourseResult r : StudentService.results()) {
            for (GradeComponent comp : r.components()) {
                GradeEntry g = r.scores().get(comp.name());
                rows.add(List.of(r.enrollment().section().term(), r.enrollment().section().courseCode(), comp.name(),
                        g == null ? "" : InstructorService.fmt(g.score()), InstructorService.fmt(comp.maxScore()),
                        g == null ? "" : String.format("%.1f", g.percent()), InstructorService.fmt(comp.weight())));
            }
        }
        Csv.write(file, List.of("Term", "Course", "Assessment", "Score", "Out of", "Percent", "Weight %"), rows);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
