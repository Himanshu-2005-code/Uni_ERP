package edu.univ.erp.ui.theme;

import java.util.prefs.Preferences;

/** Per-user UI preferences stored by the JVM (registry on Windows, plist on macOS, files on Linux). */
public final class Prefs {

    public enum Transition { SLIDE, FADE, CUBE }

    private static final Preferences P = Preferences.userRoot().node("campus-erp");

    private Prefs() {
    }

    public static boolean dark() { return P.getBoolean("dark", false); }
    public static void dark(boolean v) { P.putBoolean("dark", v); }

    public static boolean reduceMotion() { return P.getBoolean("reduceMotion", false); }
    public static void reduceMotion(boolean v) { P.putBoolean("reduceMotion", v); }

    public static Transition transition() {
        try {
            return Transition.valueOf(P.get("transition", Transition.SLIDE.name()));
        } catch (IllegalArgumentException e) {
            return Transition.SLIDE;
        }
    }

    public static void transition(Transition t) { P.put("transition", t.name()); }

    public static String lastUsername() { return P.get("lastUsername", ""); }
    public static void lastUsername(String u) { P.put("lastUsername", u == null ? "" : u); }

    public static boolean sidebarCollapsed() { return P.getBoolean("sidebarCollapsed", false); }
    public static void sidebarCollapsed(boolean v) { P.putBoolean("sidebarCollapsed", v); }

    public static boolean show3d() { return P.getBoolean("show3d", true); }
    public static void show3d(boolean v) { P.putBoolean("show3d", v); }
}
