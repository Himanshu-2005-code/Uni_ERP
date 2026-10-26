package edu.univ.erp;

import edu.univ.erp.ui.screen.AppFrame;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.SwingUtilities;

/** Campus ERP entry point. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", System.getProperty("sun.java2d.opengl", "false"));
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            AppFrame frame = new AppFrame();
            frame.setVisible(true);
            frame.start();
        });
    }
}
