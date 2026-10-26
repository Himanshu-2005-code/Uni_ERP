package edu.univ.erp.ui.screen;

import edu.univ.erp.db.DemoSeeder;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.ServiceException;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** First run on an empty database: load the demo campus or create the first administrator. */
class SetupScreen extends SceneScreen {

    SetupScreen(AppFrame frame) {
        super(540);
        card.add(Glass.label("Welcome to Campus ERP", 26f, true, Glass.TEXT));
        card.add(Glass.label("<html><div style='width:338px'>Your database is ready. How would you like to start?</div></html>",
                13.5f, false, Glass.MUTED), "gapbottom 20");
        Segmented mode = new Segmented("Explore a demo campus", "Start fresh");
        card.add(mode, "w pref!, gapbottom 20");
        JPanel body = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[]"));
        body.setOpaque(false);
        card.add(body, "growx, wmin 0");

        // Demo
        JPanel demo = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", ""));
        demo.setOpaque(false);
        String[][] facts = {{"users", "60 students, 8 instructors and 2 administrators"},
                {"book", "17 courses with sections across three terms"},
                {"award", "Marks, published grades and GPAs so every chart has data"},
                {"key", "Every demo account uses the password " + DemoSeeder.DEMO_PASSWORD}};
        for (String[] f : facts) {
            JPanel r = new JPanel(new MigLayout("insets 0, gap 12", "[][grow]", "[center]"));
            r.setOpaque(false);
            r.add(new JLabel(new javax.swing.Icon() {
                @Override
                public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = Theme.hq(g);
                    Icons.paint(g2, f[0], x, y, 18, Theme.hex("#A5B4FC"), 1.9f);
                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return 18;
                }

                @Override
                public int getIconHeight() {
                    return 18;
                }
            }));
            r.add(Glass.label(f[1], 13.5f, false, Glass.TEXT));
            demo.add(r, "gapbottom 10");
        }
        JLabel demoErr = Glass.label(" ", 12.5f, false, Theme.hex("#FCA5A5"));
        demo.add(demoErr);
        Btn load = Btn.primary("Load demo campus", "sparkle");
        demo.add(load, "h 48!, gaptop 6");
        load.addActionListener(e -> {
            load.setLoading(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DemoSeeder.seed();
                    return null;
                }

                @Override
                protected void done() {
                    load.setLoading(false);
                    try {
                        get();
                        frame.showLogin("Demo campus loaded. Try the demo account buttons below the form.");
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() == null ? ex : ex.getCause();
                        demoErr.setText(c.getMessage());
                    }
                }
            }.execute();
        });
        body.add(demo, "growx, wmin 0");

        // Fresh install
        JPanel fresh = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", ""));
        fresh.setOpaque(false);
        JTextField name = Glass.field("Full name", "user");
        JTextField user = Glass.field("Username (e.g. admin)", "user");
        JPasswordField pw = Glass.password("Password (8+ characters, letters and numbers)");
        JPasswordField pw2 = Glass.password("Confirm password");
        fresh.add(name, "h 46!, gapbottom 10");
        fresh.add(user, "h 46!, gapbottom 10");
        fresh.add(pw, "h 46!, gapbottom 10");
        fresh.add(pw2, "h 46!, gapbottom 6");
        JLabel err = Glass.label(" ", 12.5f, false, Theme.hex("#FCA5A5"));
        fresh.add(err);
        Btn create = Btn.primary("Create administrator", "shield");
        fresh.add(create, "h 48!, gaptop 6");
        create.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pw.getPassword());
            try {
                if (!u.matches("[A-Za-z0-9._-]{3,50}")) throw new ServiceException("Username must be 3–50 letters, numbers, dot, dash or underscore.");
                AuthService.checkPolicy(p);
                if (!p.equals(new String(pw2.getPassword()))) throw new ServiceException("The passwords don't match.");
            } catch (ServiceException ex) {
                err.setText(ex.getMessage());
                return;
            }
            create.setLoading(true);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DemoSeeder.createAdmin(u, p, name.getText().trim().isEmpty() ? null : name.getText().trim());
                    return null;
                }

                @Override
                protected void done() {
                    create.setLoading(false);
                    try {
                        get();
                        edu.univ.erp.ui.theme.Prefs.lastUsername(u);
                        frame.showLogin("Administrator created. Sign in to set up your campus.");
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() == null ? ex : ex.getCause();
                        err.setText(c.getMessage());
                    }
                }
            }.execute();
        });
        mode.onChange(i -> {
            body.removeAll();
            body.add(i == 0 ? demo : fresh, "growx, wmin 0");
            body.revalidate();
            card.revalidate();
            card.repaint();
        });
    }
}
