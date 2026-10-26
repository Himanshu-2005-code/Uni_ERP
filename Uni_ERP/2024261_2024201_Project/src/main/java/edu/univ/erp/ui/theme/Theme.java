package edu.univ.erp.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Colours, fonts and the light/dark switch. Custom-painted components read colours
 * through {@link #p()} at paint time, so a theme switch repaints everything consistently.
 */
public final class Theme {

    public record Palette(Color bg, Color surface, Color surfaceAlt, Color border, Color text, Color muted,
                          Color subtle, Color accent, Color accentHover, Color accentSoft, Color accent2,
                          Color success, Color warning, Color danger, Color sidebarTop, Color sidebarBottom,
                          Color sidebarText, Color shadow, Color hover) {
    }

    public static final Palette LIGHT = new Palette(
            hex("#F3F5FB"), hex("#FFFFFF"), hex("#F8FAFD"), hex("#E5E8F1"), hex("#0F172A"), hex("#64748B"),
            hex("#94A3B8"), hex("#6366F1"), hex("#4F46E5"), hex("#EEF0FF"), hex("#06B6D4"),
            hex("#10B981"), hex("#F59E0B"), hex("#EF4444"), hex("#141A3A"), hex("#1E1B4B"),
            hex("#C7CBEA"), new Color(30, 41, 90), hex("#F1F3FA"));

    public static final Palette DARK = new Palette(
            hex("#0A0E1A"), hex("#121829"), hex("#161D31"), hex("#232C45"), hex("#E7EAF3"), hex("#95A0B8"),
            hex("#5B6682"), hex("#818CF8"), hex("#A5B4FC"), hex("#1E2246"), hex("#22D3EE"),
            hex("#34D399"), hex("#FBBF24"), hex("#F87171"), hex("#0B1024"), hex("#141A38"),
            hex("#AEB5D9"), new Color(0, 0, 0), hex("#1A2238"));

    /** Categorical colours for chart series. */
    public static final Color[] SERIES = {hex("#6366F1"), hex("#06B6D4"), hex("#10B981"), hex("#F59E0B"),
            hex("#F43F5E"), hex("#A855F7"), hex("#0EA5E9"), hex("#84CC16"), hex("#EC4899"), hex("#14B8A6")};

    private static Palette current = LIGHT;
    private static final java.util.List<Runnable> LISTENERS = new java.util.ArrayList<>();

    /** Called after every light/dark switch. */
    public static void onChange(Runnable r) {
        LISTENERS.add(r);
    }

    public static void removeListener(Runnable r) {
        LISTENERS.remove(r);
    }
    private static boolean fontsLoaded;
    private static String regular = Font.SANS_SERIF;
    private static String medium = Font.SANS_SERIF;
    private static String semibold = Font.SANS_SERIF;

    private Theme() {
    }

    public static Palette p() {
        return current;
    }

    public static boolean isDark() {
        return current == DARK;
    }

    public static Color series(int i) {
        return SERIES[Math.floorMod(i, SERIES.length)];
    }

    public static Color hex(String h) {
        return new Color(Integer.parseInt(h.substring(1), 16));
    }

    public static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.max(0, Math.min(1, a)) * 255));
    }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color brighter(Color c, double amount) {
        return mix(c, Color.WHITE, amount);
    }

    public static Color darker(Color c, double amount) {
        return mix(c, Color.BLACK, amount);
    }

    // ---- Fonts -----------------------------------------------------------------------------

    private static void loadFonts() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Map<String, String> families = new HashMap<>();
        for (String w : new String[]{"Regular", "Medium", "SemiBold", "Bold"}) {
            try (InputStream in = Theme.class.getResourceAsStream("/fonts/Inter-" + w + ".otf")) {
                if (in == null) continue;
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                ge.registerFont(f);
                families.put(w, f.getFamily());
            } catch (Exception ignored) {
                // fall back to the platform sans-serif
            }
        }
        if (families.containsKey("Regular")) regular = families.get("Regular");
        medium = families.getOrDefault("Medium", regular);
        semibold = families.getOrDefault("SemiBold", regular);
    }

    public static Font font(float size) {
        return new Font(regular, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font medium(float size) {
        return new Font(medium, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font semibold(float size) {
        return new Font(semibold, Font.PLAIN, 1).deriveFont(size);
    }

    public static Font bold(float size) {
        return new Font(regular, Font.BOLD, 1).deriveFont(size);
    }

    // ---- Look and feel ---------------------------------------------------------------------

    /** Installs FlatLaf with the app's palette. Call once before creating any windows. */
    public static void install() {
        loadFonts();
        System.setProperty("flatlaf.animation", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");
        UIManager.put("defaultFont", new FontUIResource(font(13f)));
        apply(Prefs.dark());
    }

    private static void apply(boolean dark) {
        current = dark ? DARK : LIGHT;
        Palette p = current;
        Map<String, String> d = new HashMap<>();
        d.put("@accentColor", css(p.accent()));
        d.put("@background", css(p.bg()));
        d.put("@foreground", css(p.text()));
        d.put("Component.arc", "12");
        d.put("Button.arc", "12");
        d.put("TextComponent.arc", "12");
        d.put("CheckBox.arc", "6");
        d.put("ProgressBar.arc", "999");
        d.put("Component.focusWidth", "2");
        d.put("Component.innerFocusWidth", "0");
        d.put("Component.focusColor", css(alpha(p.accent(), 0.35)));
        d.put("Component.borderColor", css(p.border()));
        d.put("Component.focusedBorderColor", css(p.accent()));
        d.put("TextComponent.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("TextField.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("PasswordField.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("TextArea.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("Spinner.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("ComboBox.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("TextComponent.selectionBackground", css(alpha(p.accent(), 0.35)));
        d.put("Button.background", css(dark ? p.surfaceAlt() : p.surface()));
        d.put("Button.borderColor", css(p.border()));
        d.put("Button.hoverBackground", css(p.hover()));
        d.put("Button.default.background", css(p.accent()));
        d.put("Button.default.foreground", "#FFFFFF");
        d.put("Button.default.hoverBackground", css(p.accentHover()));
        d.put("Button.default.borderWidth", "0");
        d.put("Button.default.focusedBackground", css(p.accent()));
        d.put("ScrollBar.thumbArc", "999");
        d.put("ScrollBar.thumbInsets", "2,2,2,2");
        d.put("ScrollBar.width", "11");
        d.put("ScrollBar.showButtons", "false");
        d.put("ScrollBar.track", css(new Color(0, 0, 0, 0)));
        d.put("ScrollBar.thumb", css(alpha(p.muted(), 0.35)));
        d.put("ScrollBar.hoverThumbColor", css(alpha(p.muted(), 0.55)));
        d.put("ScrollPane.background", css(p.surface()));
        d.put("ScrollPane.smoothScrolling", "true");
        d.put("Table.rowHeight", "42");
        d.put("Table.intercellSpacing", "0,0");
        d.put("Table.showHorizontalLines", "true");
        d.put("Table.showVerticalLines", "false");
        d.put("Table.gridColor", css(alpha(p.border(), 0.8)));
        d.put("Table.background", css(p.surface()));
        d.put("Table.selectionBackground", css(p.accentSoft()));
        d.put("Table.selectionForeground", css(p.text()));
        d.put("Table.selectionInactiveBackground", css(p.accentSoft()));
        d.put("Table.selectionInactiveForeground", css(p.text()));
        d.put("Table.cellMargins", "4,14,4,14");
        d.put("TableHeader.height", "40");
        d.put("TableHeader.background", css(p.surfaceAlt()));
        d.put("TableHeader.foreground", css(p.muted()));
        d.put("TableHeader.separatorColor", css(p.surfaceAlt()));
        d.put("TableHeader.bottomSeparatorColor", css(p.border()));
        d.put("TableHeader.cellMargins", "4,14,4,14");
        d.put("TableHeader.font", "-1");
        d.put("ToolTip.background", css(dark ? hex("#1F2740") : hex("#0F172A")));
        d.put("ToolTip.foreground", "#F8FAFC");
        d.put("ToolTip.border", "8,10,8,10," + css(dark ? hex("#1F2740") : hex("#0F172A")));
        d.put("PopupMenu.background", css(p.surface()));
        d.put("PopupMenu.borderColor", css(p.border()));
        d.put("PopupMenu.borderCornerRadius", "12");
        d.put("MenuItem.selectionBackground", css(p.accentSoft()));
        d.put("MenuItem.selectionForeground", css(p.text()));
        d.put("ComboBox.selectionBackground", css(p.accentSoft()));
        d.put("ComboBox.selectionForeground", css(p.text()));
        d.put("List.selectionBackground", css(p.accentSoft()));
        d.put("List.selectionForeground", css(p.text()));
        d.put("TitlePane.unifiedBackground", "true");
        d.put("TitlePane.background", css(p.bg()));
        d.put("Separator.foreground", css(p.border()));
        d.put("Slider.trackWidth", "4");
        d.put("Spinner.arrowButtonWidth", "0");
        d.put("TabbedPane.selectedBackground", css(p.surface()));
        d.put("TabbedPane.underlineColor", css(p.accent()));
        d.put("TabbedPane.tabHeight", "40");
        d.put("TabbedPane.showTabSeparators", "false");
        d.put("TabbedPane.hoverColor", css(p.hover()));
        d.put("ToggleButton.selectedBackground", css(p.accent()));
        d.put("ToggleButton.selectedForeground", "#FFFFFF");
        d.put("CheckBox.icon.checkmarkColor", "#FFFFFF");
        d.put("CheckBox.icon.selectedBackground", css(p.accent()));
        d.put("CheckBox.icon.selectedBorderColor", css(p.accent()));
        d.put("SplitPaneDivider.gripColor", css(p.border()));
        FlatLaf.setGlobalExtraDefaults(d);
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
    }

    /** Switches theme with a cross-fade of the whole window. */
    public static void toggle(Window w) {
        boolean dark = !isDark();
        Prefs.dark(dark);
        JRootPane root = SwingUtilities.getRootPane(w);
        BufferedImage before = null;
        if (root != null && root.getWidth() > 0 && root.getHeight() > 0) {
            before = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = before.createGraphics();
            root.paint(g);
            g.dispose();
        }
        apply(dark);
        FlatLaf.updateUI();
        for (Runnable r : new java.util.ArrayList<>(LISTENERS)) r.run();
        if (before == null || Prefs.reduceMotion()) return;
        BufferedImage img = before;
        FadeGlass glass = new FadeGlass(img);
        java.awt.Component oldGlass = root.getGlassPane();
        root.setGlassPane(glass);
        glass.setVisible(true);
        Animator.animate(420, Easing.IN_OUT_CUBIC, t -> {
            glass.alpha = (float) (1 - t);
            glass.repaint();
        }, () -> {
            glass.setVisible(false);
            root.setGlassPane(oldGlass);
        });
    }

    private static final class FadeGlass extends JComponent {
        private final BufferedImage img;
        private float alpha = 1f;

        FadeGlass(BufferedImage img) {
            this.img = img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(img, 0, 0, null);
            g2.dispose();
        }
    }

    private static String css(Color c) {
        if (c.getAlpha() == 255) return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    /** Standard quality hints for custom painting. */
    public static Graphics2D hq(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g2;
    }
}
