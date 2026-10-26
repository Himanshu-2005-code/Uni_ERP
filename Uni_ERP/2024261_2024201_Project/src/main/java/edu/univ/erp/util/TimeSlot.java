package edu.univ.erp.util;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A weekly meeting pattern such as "Mon Wed 10:00-11:30".
 * Days may be separated by spaces, commas or slashes, and written short or in full.
 */
public final class TimeSlot {

    private final Set<DayOfWeek> days;
    private final int startMinute;
    private final int endMinute;

    private TimeSlot(Set<DayOfWeek> days, int startMinute, int endMinute) {
        this.days = days;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    /** Parses a slot, or returns null when the text isn't a valid pattern. */
    public static TimeSlot parse(String text) {
        if (text == null) return null;
        String t = text.trim();
        int dash = t.lastIndexOf('-');
        int space = t.lastIndexOf(' ', dash);
        if (dash < 0 || space < 0) return null;
        String dayPart = t.substring(0, space).trim();
        String timePart = t.substring(space + 1).trim();
        String[] times = timePart.split("-");
        if (times.length != 2) return null;
        Integer start = minutes(times[0]);
        Integer end = minutes(times[1]);
        if (start == null || end == null || end <= start) return null;
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : dayPart.split("[\\s,/]+")) {
            if (token.isBlank()) continue;
            DayOfWeek d = day(token);
            if (d == null) return null;
            days.add(d);
        }
        if (days.isEmpty()) return null;
        return new TimeSlot(days, start, end);
    }

    public static boolean isValid(String text) {
        return parse(text) != null;
    }

    private static Integer minutes(String hhmm) {
        String[] p = hhmm.trim().split(":");
        try {
            int h = Integer.parseInt(p[0]);
            int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DayOfWeek day(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        for (DayOfWeek d : DayOfWeek.values()) {
            String full = d.name().toLowerCase(Locale.ROOT);
            if (full.equals(t) || full.startsWith(t) && t.length() >= 2) return d;
        }
        return null;
    }

    public boolean overlaps(TimeSlot other) {
        if (other == null) return false;
        for (DayOfWeek d : days) {
            if (other.days.contains(d)) {
                return startMinute < other.endMinute && other.startMinute < endMinute;
            }
        }
        return false;
    }

    public static boolean clash(String a, String b) {
        TimeSlot x = parse(a);
        TimeSlot y = parse(b);
        return x != null && x.overlaps(y);
    }

    public Set<DayOfWeek> days() {
        return days;
    }

    public int startMinute() {
        return startMinute;
    }

    public int endMinute() {
        return endMinute;
    }

    public static String format(int minute) {
        return String.format("%02d:%02d", minute / 60, minute % 60);
    }

    @Override
    public String toString() {
        return days.stream().map(d -> d.name().substring(0, 1) + d.name().substring(1, 3).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" ")) + " " + format(startMinute) + "-" + format(endMinute);
    }
}
