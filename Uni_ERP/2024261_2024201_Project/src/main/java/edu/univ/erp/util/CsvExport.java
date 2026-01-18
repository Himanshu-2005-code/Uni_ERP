package edu.univ.erp.util;

import edu.univ.erp.domain.Grade;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExport {

    public static void exportGrades(List<Grade> grades, String filename) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            // Write header
            writer.writeNext(new String[]{"Component", "Score", "Max Score", "Percentage", "Weight"});

            // Write data
            for (Grade grade : grades) {
                writer.writeNext(new String[]{
                        grade.getComponent(),
                        String.valueOf(grade.getScore()),
                        String.valueOf(grade.getMaxScore()),
                        String.format("%.1f", grade.getPercentage()),
                        String.valueOf(grade.getWeight())
                });
            }
        }
    }
}