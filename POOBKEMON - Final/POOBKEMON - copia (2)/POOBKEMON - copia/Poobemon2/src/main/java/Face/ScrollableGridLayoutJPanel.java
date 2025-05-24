package Face;

import java.awt.Component; // <-- IMPORTACIÓN AÑADIDA
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

public class ScrollableGridLayoutJPanel extends JPanel implements Scrollable {

    public ScrollableGridLayoutJPanel(int rows, int cols, int hgap, int vgap) {
        super(new GridLayout(rows, cols, hgap, vgap));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            if (getComponentCount() > 0) {
                Component comp = getComponent(0);
                if (comp != null && comp.getHeight() > 0) {
                    return comp.getHeight(); // Scroll por el alto de una celda
                }
            }
            return Math.max(1, visibleRect.height / 10); // Fallback
        } else { // HORIZONTAL
            if (getComponentCount() > 0) {
                Component comp = getComponent(0);
                if (comp != null && comp.getWidth() > 0) {
                    return comp.getWidth(); // Scroll por el ancho de una celda
                }
            }
            return Math.max(1, visibleRect.width / 10); // Fallback
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height;
        } else { // HORIZONTAL
            return visibleRect.width;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // Forzar al panel a ser tan ancho como el viewport
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // No forzar al panel a ser tan alto como el viewport; permitir scroll vertical
        return false;
    }
}
