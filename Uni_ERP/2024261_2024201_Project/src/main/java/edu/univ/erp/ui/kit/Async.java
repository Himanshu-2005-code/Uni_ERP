package edu.univ.erp.ui.kit;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Runs database work off the event thread, then hands the result back to Swing. */
public final class Async {

    private Async() {
    }

    public static <T> void run(Component owner, Supplier<T> work, Consumer<T> done) {
        run(owner, work, done, null);
    }

    public static <T> void run(Component owner, Supplier<T> work, Consumer<T> done, Runnable always) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                try {
                    T value = get();
                    if (done != null) done.accept(value);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    Toasts.error(owner, message(cause));
                    if (!(cause instanceof edu.univ.erp.service.ServiceException)) cause.printStackTrace();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (always != null) always.run();
                }
            }
        }.execute();
    }

    /** Runs an action from a button, showing the button's spinner while it works. */
    public static <T> void button(Btn b, Supplier<T> work, Consumer<T> done) {
        b.setLoading(true);
        run(b, work, done, () -> b.setLoading(false));
    }

    public static String message(Throwable t) {
        String m = t.getMessage();
        return m == null || m.isBlank() ? "Something went wrong (" + t.getClass().getSimpleName() + ")." : m;
    }
}
