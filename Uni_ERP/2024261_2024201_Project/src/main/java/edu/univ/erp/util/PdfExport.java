package edu.univ.erp.util;

import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Grade;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfExport {

    public static void exportTranscript(Student student, List<Grade> grades, String filename)
            throws DocumentException, IOException {

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("UNIVERSITY TRANSCRIPT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(Chunk.NEWLINE);

        // Student info
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        document.add(new Paragraph("Student: " + student.getUsername(), infoFont));
        document.add(new Paragraph("Roll No: " + student.getRollNo(), infoFont));
        document.add(new Paragraph("Program: " + student.getProgram(), infoFont));
        document.add(new Paragraph("Year: " + student.getYear(), infoFont));

        document.add(Chunk.NEWLINE);

        // Grades table
        if (!grades.isEmpty()) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            // Table headers
            table.addCell(createHeaderCell("Component"));
            table.addCell(createHeaderCell("Score"));
            table.addCell(createHeaderCell("Max Score"));
            table.addCell(createHeaderCell("Percentage"));
            table.addCell(createHeaderCell("Weight"));

            // Table data
            for (Grade grade : grades) {
                table.addCell(createNormalCell(grade.getComponent()));
                table.addCell(createNormalCell(String.valueOf(grade.getScore())));
                table.addCell(createNormalCell(String.valueOf(grade.getMaxScore())));
                table.addCell(createNormalCell(String.format("%.1f%%", grade.getPercentage())));
                table.addCell(createNormalCell(String.valueOf(grade.getWeight())));
            }

            document.add(table);
        } else {
            document.add(new Paragraph("No grades available.", infoFont));
        }

        document.close();
    }

    private static PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createNormalCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}