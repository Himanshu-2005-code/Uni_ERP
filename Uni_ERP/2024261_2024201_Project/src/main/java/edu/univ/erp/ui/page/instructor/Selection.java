package edu.univ.erp.ui.page.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.kit.Select;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Remembers which section the instructor is working on across the teaching pages. */
final class Selection {

    static Integer sectionId;

    private Selection() {
    }

    /** Current-term sections first, then newest terms. */
    static List<Section> order(List<Section> sections) {
        String term = SettingsService.get().term();
        List<Section> out = new ArrayList<>(sections);
        out.sort(Comparator.comparing((Section s) -> !s.term().equals(term))
                .thenComparing(Section::year, Comparator.reverseOrder())
                .thenComparing(Section::semester)
                .thenComparing(Section::courseCode));
        return out;
    }

    static Select<Section> picker(List<Section> sections) {
        Select<Section> sel = new Select<Section>().placeholder("Choose a section").leading("layers").menuWidth(380)
                .labels(s -> s.courseCode() + " · " + s.courseTitle() + "  —  " + s.dayTime() + " · " + s.term());
        List<Section> ordered = order(sections);
        sel.options(ordered);
        Section pick = ordered.stream().filter(s -> sectionId != null && s.id() == sectionId).findFirst()
                .orElse(ordered.isEmpty() ? null : ordered.get(0));
        sel.setSelectedQuietly(pick);
        if (pick != null) sectionId = pick.id();
        sel.onChange(s -> sectionId = s == null ? null : s.id());
        return sel;
    }
}
