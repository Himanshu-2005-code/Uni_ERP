package edu.univ.erp.ui.anim;

import edu.univ.erp.ui.theme.Prefs;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * One shared ~60 fps clock driving every animation in the app, so many simultaneous
 * tweens cost a single timer. All callbacks run on the Swing event thread.
 */
public final class Animator {

    /** Handle for a running animation. */
    public static final class Anim {
        private final long start = System.nanoTime();
        private final long durationNs;
        private final Easing easing;
        private final DoubleConsumer frame;
        private final Runnable end;
        private boolean cancelled;
        private boolean done;

        private Anim(long durationMs, Easing easing, DoubleConsumer frame, Runnable end) {
            this.durationNs = Math.max(1, durationMs) * 1_000_000L;
            this.easing = easing;
            this.frame = frame;
            this.end = end;
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isRunning() {
            return !cancelled && !done;
        }

        private boolean tick(long now) {
            if (cancelled) return true;
            double t = Math.min(1.0, (now - start) / (double) durationNs);
            frame.accept(easing.apply(t));
            if (t >= 1.0) {
                done = true;
                if (end != null) end.run();
                return true;
            }
            return false;
        }
    }

    private static final List<Anim> ACTIVE = new ArrayList<>();
    private static final List<Runnable> LOOPS = new ArrayList<>();
    private static final Timer TIMER = new Timer(15, e -> tick());

    static {
        TIMER.setCoalesce(true);
    }

    private Animator() {
    }

    /** Runs frame(progress) for durationMs, then end. Honours the reduce-motion preference. */
    public static Anim animate(long durationMs, Easing easing, DoubleConsumer frame, Runnable end) {
        if (Prefs.reduceMotion()) durationMs = 1;
        Anim a = new Anim(durationMs, easing, frame, end);
        ACTIVE.add(a);
        if (!TIMER.isRunning()) TIMER.start();
        return a;
    }

    public static Anim animate(long durationMs, DoubleConsumer frame) {
        return animate(durationMs, Easing.OUT_CUBIC, frame, null);
    }

    /** Registers a callback run every frame until removed (for continuous effects like 3D rotation). */
    public static void addLoop(Runnable r) {
        if (!LOOPS.contains(r)) LOOPS.add(r);
        if (!TIMER.isRunning()) TIMER.start();
    }

    public static void removeLoop(Runnable r) {
        LOOPS.remove(r);
    }

    private static void tick() {
        long now = System.nanoTime();
        List<Anim> snapshot = new ArrayList<>(ACTIVE);
        for (Anim a : snapshot) {
            if (a.tick(now)) ACTIVE.remove(a);
        }
        for (Runnable r : new ArrayList<>(LOOPS)) r.run();
        if (ACTIVE.isEmpty() && LOOPS.isEmpty()) TIMER.stop();
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Removes finished handles from a caller-held list. */
    public static void prune(List<Anim> anims) {
        for (Iterator<Anim> it = anims.iterator(); it.hasNext(); ) {
            if (!it.next().isRunning()) it.remove();
        }
    }
}
