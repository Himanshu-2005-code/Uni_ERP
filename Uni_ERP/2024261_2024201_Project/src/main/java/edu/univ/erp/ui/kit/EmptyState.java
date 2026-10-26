package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** A friendly message for lists with nothing in them yet. */
public class EmptyState extends JPanel {

    public EmptyState(String icon, String title, String text) {
        super(new MigLayout("insets 30, wrap 1, alignx center, aligny center", "[center]", "[]14[]4[]"));
        setOpaque(false);
        add(new JLabel(new IconBubble(icon, 60, Theme.p().accent())));
        add(UI.h3(title));
        if (text != null) add(UI.html("<div style='text-align:center;width:231px'>" + text + "</div>", 12.5f, true));
    }
}
