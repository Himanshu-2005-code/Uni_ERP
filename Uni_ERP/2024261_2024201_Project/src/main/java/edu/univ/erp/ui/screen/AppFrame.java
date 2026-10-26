package edu.univ.erp.ui.screen;

import edu.univ.erp.db.Database;
import edu.univ.erp.db.SchemaManager;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.kit.Brand;
import edu.univ.erp.ui.kit.Stage;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Prefs;

import javax.swing.JFrame;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The single application window; swaps between splash, connection, setup, sign-in and the workspace. */
public class AppFrame extends JFrame {

    private final Stage screens = new Stage();

    public AppFrame() {
        super(Brand.NAME);
        setIconImages(Brand.icons());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 740));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1440, (int) (screen.width * 0.92)), Math.min(900, (int) (screen.height * 0.92)));
        setLocationRelativeTo(null);
        setContentPane(screens);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                AuthService.logout();
                Database.stop();
                System.exit(0);
            }
        });
    }

    /** Connects, prepares the schema and routes to the right first screen. */
    public void start() {
        show(new SplashScreen("Connecting to the database…"), Prefs.Transition.FADE);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                long t0 = System.currentTimeMillis();
                SchemaManager.ensureSchema();
                Database.start();
                boolean empty = SchemaManager.isEmpty();
                long wait = 900 - (System.currentTimeMillis() - t0); // let the splash breathe
                if (wait > 0) Thread.sleep(wait);
                return empty;
            }

            @Override
            protected void done() {
                try {
                    if (get()) showSetup();
                    else showLogin(null);
                } catch (Exception e) {
                    Throwable c = e.getCause() == null ? e : e.getCause();
                    Database.stop();
                    showConnect(c.getMessage());
                }
            }
        }.execute();
    }

    void show(javax.swing.JComponent c, Prefs.Transition t) {
        screens.show(c, 1, t);
    }

    public void showLogin(String notice) {
        show(new LoginScreen(this, notice), Prefs.Transition.FADE);
    }

    public void showSetup() {
        show(new SetupScreen(this), Prefs.Transition.FADE);
    }

    public void showConnect(String error) {
        show(new ConnectScreen(this, error), Prefs.Transition.FADE);
    }

    public void showShell(User user) {
        setTitle(Brand.NAME + " — " + user.displayName());
        show(new Shell(this, user), Prefs.Transition.FADE);
    }

    public void signOut() {
        AuthService.logout();
        setTitle(Brand.NAME);
        show(new LoginScreen(this, null), Prefs.Transition.FADE);
    }
}
