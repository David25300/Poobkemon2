package Face;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle; // Necesario para Scrollable
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Scrollable; // Implementar Scrollable
import javax.swing.SwingConstants; // Necesario para Scrollable

public class ImageBackgroundPanel extends JPanel implements Scrollable { // Implementar Scrollable
    private Image backgroundImage;

    public ImageBackgroundPanel(String imagePath) {
        this(); // Llama al constructor por defecto
        try {
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                backgroundImage = ImageIO.read(imageUrl);
            } else {
                System.err.println("No se pudo encontrar la imagen de fondo en: " + imagePath);
                backgroundImage = null;
            }
        } catch (IOException e) {
            System.err.println("Error al cargar la imagen de fondo: " + imagePath);
            e.printStackTrace();
            backgroundImage = null;
        }
    }

    public ImageBackgroundPanel(Image image) {
        this(); // Llama al constructor por defecto
        this.backgroundImage = image;
    }

    public ImageBackgroundPanel() {
        super();
        // Por defecto, el panel es opaco. Si la imagen no se carga, se verá el color de fondo del JPanel.
        setOpaque(true); 
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        if (backgroundImage != null) {
            // Dibuja la imagen para que cubra todo el panel.
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // Implementación de la interfaz Scrollable
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        // Si la imagen de fondo tiene un tamaño intrínseco y no quieres que se estire,
        // podrías basar el tamaño preferido en ella.
        // Para este caso, queremos que el contenido determine el tamaño preferido.
        return getPreferredSize(); 
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Incremento de scroll razonable, por ejemplo, una fracción del viewport
        if (orientation == SwingConstants.VERTICAL) {
            return Math.max(1, visibleRect.height / 10);
        } else { // HORIZONTAL
            return Math.max(1, visibleRect.width / 10);
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Scroll por una página del viewport
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height;
        } else { // HORIZONTAL
            return visibleRect.width;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // Queremos que el ImageBackgroundPanel se ajuste al ancho del viewport
        // si el JScrollPane así lo requiere.
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // CAMBIO: Devolver true para que el panel intente ocupar la altura del viewport.
        // Esto permitirá que el GridLayout interno distribuya mejor el espacio vertical
        // a las miniaturas de Pokémon cuando la ventana se maximiza.
        // Si el contenido real (la cuadrícula de Pokémon) es más alto que el viewport,
        // el JScrollPane debería seguir permitiendo el scroll gracias a que
        // ScrollableGridLayoutJPanel.getScrollableTracksViewportHeight() es false.
        return true; 
    }
}
