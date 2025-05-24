package Face;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.net.URL; // Importado para el constructor de JPanel

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class ModeSelectionPanel extends JPanel {
    private SwingGUI parentFrame;
    private ImageIcon backgroundGif;

    // Colores inspirados en el estilo "Among Us" (puedes ajustarlos)
    private static final Color AMONG_US_BUTTON_BG_CYAN = new Color(66, 213, 224);
    private static final Color AMONG_US_BUTTON_FG_DARK = new Color(47, 55, 79); // Un gris oscuro/azul marino
    private static final Color AMONG_US_BUTTON_BORDER_DARK = new Color(30, 30, 60);
    private static final Color AMONG_US_EXIT_BUTTON_BG = new Color(220, 50, 50); // Rojo para salir
    private static final Color AMONG_US_TITLE_FG = Color.WHITE;


    // Panel interno para dibujar el GIF y superponer componentes
    private class BackgroundPanel extends JPanel {
        public BackgroundPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundGif != null) {
                // Dibuja el GIF cubriendo todo el panel.
                // El ImageIcon se encargará de la animación si es un GIF animado.
                g.drawImage(backgroundGif.getImage(), 0, 0, getWidth(), getHeight(), this);
            } else {
                // Fallback si el GIF no carga
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                g.drawString("Error: No se pudo cargar Fondo-modo.gif", 20, 20);
            }
        }
    }


    public ModeSelectionPanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        loadBackgroundImage();
        initComponents();
    }

    private void loadBackgroundImage() {
        try {
            String gifPath = "/backgrounds/Fondo-modo.gif";
            URL gifUrl = getClass().getResource(gifPath);
            if (gifUrl != null) {
                backgroundGif = new ImageIcon(gifUrl);
                // Forzar la carga de la imagen para obtener dimensiones y asegurar que no haya errores de carga diferida
                backgroundGif.getImage().getWidth(null); 
                System.out.println("[ModeSelectionPanel] GIF de fondo cargado desde: " + gifPath);
            } else {
                System.err.println("[ModeSelectionPanel] Error CRÍTICO: No se encontró el recurso GIF de fondo en: " + gifPath);
                backgroundGif = null;
            }
        } catch (Exception e) {
            System.err.println("[ModeSelectionPanel] Excepción al cargar el GIF de fondo: " + e.getMessage());
            e.printStackTrace();
            backgroundGif = null;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundGif != null && backgroundGif.getImage() != null) {
            // Dibuja el GIF cubriendo todo el panel.
            // El ImageIcon se encargará de la animación si es un GIF animado.
            g.drawImage(backgroundGif.getImage(), 0, 0, getWidth(), getHeight(), this);
        } else {
            // Fallback si el GIF no carga
            g.setColor(Color.BLACK); // Fondo negro si no hay GIF
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            Font errorFont = getFont();
             if (parentFrame != null && parentFrame.getPixelArtFont() != null) {
                errorFont = parentFrame.getPixelArtFont().deriveFont(12f);
            } else if (errorFont == null) {
                errorFont = new Font("Monospaced", Font.BOLD, 12);
            }
            g.setFont(errorFont);
            g.drawString("Error: No se pudo cargar Fondo-modo.gif", 20, getHeight() - 20);
        }
    }


    private void initComponents() {
        // Usar GridBagLayout para el panel principal para centrar el contenedor de botones
        setLayout(new GridBagLayout());
        GridBagConstraints gbcMain = new GridBagConstraints();
        
        // Panel que contendrá los botones verticalmente, similar al menú de Among Us
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false); // Hacerlo transparente para ver el fondo GIF del panel principal

        // Título (opcional, pero puede quedar bien encima de los botones)
        JLabel titleLabel = new JLabel("SELECCIONA EL MODO");
        titleLabel.setForeground(AMONG_US_TITLE_FG);
        Font titleFont = (parentFrame.getPixelArtFont() != null) ? 
                         parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 28f) : 
                         new Font("SansSerif", Font.BOLD, 28); // Fuente más grande y legible
        titleLabel.setFont(titleFont);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0)); // Espacio debajo del título

        buttonContainer.add(titleLabel);

        // Estilo común para los botones
        Dimension buttonSize = new Dimension(280, 65); // Tamaño de los botones de Among Us
        Font buttonFont = (parentFrame.getPixelArtFont() != null) ? 
                          parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 18f) : // Un poco más grande
                          new Font("SansSerif", Font.BOLD, 18);
        Border buttonBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AMONG_US_BUTTON_BORDER_DARK, 3), // Borde oscuro más grueso
            BorderFactory.createEmptyBorder(10, 20, 10, 20) // Padding interno
        );

        // Botón Modo Normal
        JButton normalModeButton = createStyledButton("MODO NORMAL", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_CYAN, AMONG_US_BUTTON_FG_DARK);
        normalModeButton.addActionListener(e -> {
            System.out.println("Modo Normal seleccionado");
            parentFrame.startGame("Normal");
        });
        buttonContainer.add(normalModeButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 15))); // Espaciador

        // Botón Modo Supervivencia
        JButton survivalModeButton = createStyledButton("MODO SUPERVIVENCIA", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_CYAN, AMONG_US_BUTTON_FG_DARK);
        survivalModeButton.addActionListener(e -> {
            System.out.println("Modo Supervivencia seleccionado");
            parentFrame.startGame("Supervivencia");
        });
        buttonContainer.add(survivalModeButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 15)));

        // Botón Modo Prueba
        JButton testModeButton = createStyledButton("MODO PRUEBA", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_CYAN, AMONG_US_BUTTON_FG_DARK);
        testModeButton.addActionListener(e -> {
            System.out.println("Modo Prueba seleccionado");
            parentFrame.startGame("Prueba");
        });
        buttonContainer.add(testModeButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 30))); // Espacio mayor antes de Salir

        // Botón Salir
        JButton exitButton = createStyledButton("SALIR DEL JUEGO", buttonSize, buttonFont, buttonBorder, AMONG_US_EXIT_BUTTON_BG, Color.WHITE);
        exitButton.addActionListener(e -> System.exit(0));
        buttonContainer.add(exitButton);

        // Añadir el contenedor de botones al panel principal (ModeSelectionPanel)
        // Esto centrará el buttonContainer en el ModeSelectionPanel
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1.0; // Permite que se expanda horizontalmente si es necesario
        gbcMain.weighty = 1.0; // Permite que se expanda verticalmente si es necesario
        gbcMain.anchor = GridBagConstraints.CENTER; // Centrar el contenedor
        add(buttonContainer, gbcMain);
    }

    private JButton createStyledButton(String text, Dimension size, Font font, Border border, Color bgColor, Color fgColor) {
        JButton button = new JButton(text.toUpperCase()); // Texto en mayúsculas
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size); // Fijar tamaño
        button.setFont(font);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setBorder(border);
        button.setFocusPainted(false); // Quitar el borde de foco por defecto
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Centrar el botón en el BoxLayout
        return button;
    }
}
