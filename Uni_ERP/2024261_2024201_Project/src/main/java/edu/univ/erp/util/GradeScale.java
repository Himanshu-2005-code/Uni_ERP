package edu.univ.erp.util;

import java.util.List;

/** Letter grades on a 10-point scale. */
public final class GradeScale {

    public record Band(String letter, double minPercent, double points) {
    }

    public static final List<Band> BANDS = List.of(
            new Band("A+", 90, 10),
            new Band("A", 80, 10),
            new Band("A-", 70, 9),
            new Band("B", 60, 8),
            new Band("B-", 50, 7),
            new Band("C", 45, 6),
            new Band("C-", 40, 5),
            new Band("D", 35, 4),
            new Band("F", 0, 0));

    private GradeScale() {
    }

    public static String letter(double percent) {
        for (Band b : BANDS) {
            if (percent >= b.minPercent()) return b.letter();
        }
        return "F";
    }

    public static double points(String letter) {
        if (letter == null) return 0;
        for (Band b : BANDS) {
            if (b.letter().equalsIgnoreCase(letter.trim())) return b.points();
        }
        return 0;
    }

    public static boolean isPassing(String letter) {
        return points(letter) > 0;
    }
}
