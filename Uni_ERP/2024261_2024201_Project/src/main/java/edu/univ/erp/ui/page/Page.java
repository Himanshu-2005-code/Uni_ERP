package edu.univ.erp.ui.page;

import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Skeleton;
import edu.univ.erp.ui.kit.UI;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base for every screen inside the shell. Subclasses build their content in {@link #reload()},
 * typically by calling {@link #load} which shows a shimmering skeleton the first time.
 */
public abstract class Page extends JPanel {

    protected final Shell shell;
    private boolean loadedOnce;
    private final JPanel body = new JPanel(new BorderLayout());
    private JScrollPane scroll;

    protected Page(Shell shell) {
        super(new BorderLayout());
        this.shell = shell;
        setOpaque(false);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    public abstract String title();

    public String subtitle() {
        return null;
    }

    /** Called every time the page is navigated to. */
    public void onShow() {
        reload();
    }

    public abstract void reload();

    /** Loads data off the UI thread; shows a skeleton until the first load finishes. */
    protected <T> void load(Supplier<T> work, Consumer<T> render) {
        if (!loadedOnce) {
            JPanel sk = new JPanel(new MigLayout("insets 0 28 28 28, fill", "[grow, fill]", "[grow, fill]"));
            sk.setOpaque(false);
            sk.add(new Skeleton());
            setBody(sk, false);
        }
        Async.run(this, work, data -> {
            loadedOnce = true;
            render.accept(data);
            shell.stage().refreshSnapshot();
        });
    }

    /** Replaces the page content, keeping the scroll position when re-rendering. */
    protected void setBody(JComponent content, boolean scrollable) {
        int y = scroll == null ? 0 : scroll.getVerticalScrollBar().getValue();
        body.removeAll();
        if (scrollable) {
            scroll = UI.scroll(content);
            body.add(scroll, BorderLayout.CENTER);
            int keep = y;
            javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(keep));
        } else {
            scroll = null;
            body.add(content, BorderLayout.CENTER);
        }
        body.revalidate();
        body.repaint();
    }

    /** Standard page container: padded, single column. */
    protected static JPanel column() {
        JPanel p = new JPanel(new MigLayout("insets 4 28 32 28, fillx, wrap 1, gapy 18", "[grow, fill]", ""));
        p.setOpaque(false);
        return p;
    }
}
