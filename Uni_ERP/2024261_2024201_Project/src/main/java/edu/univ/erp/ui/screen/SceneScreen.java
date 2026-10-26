package edu.univ.erp.ui.screen;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.three.LoginScene;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.Component;

/** A centred glass card over the 3D scene. */
abstract class SceneScreen extends JLayeredPane {

    protected final Glass.Card card;

    SceneScreen(int width) {
        add(new LoginScene(), Integer.valueOf(0));
        JPanel overlay = new JPanel(new MigLayout("insets 30, fill", "[center]", "[center]"));
        overlay.setOpaque(false);
        card = new Glass.Card(new MigLayout("insets 36 38 30 38, fillx, wrap 1", "[grow, fill]", ""));
        overlay.add(card, "w " + width + "!");
        add(overlay, Integer.valueOf(1));
        card.alpha = 0;
        card.offsetY = 26;
        Animator.animate(650, Easing.OUT_BACK, t -> {
            card.alpha = (float) Math.min(1, t * 1.5);
            card.offsetY = 26 * (1 - t);
            card.repaint();
        }, null);
    }

    @Override
    public void doLayout() {
        for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
    }
}
