package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter; // Importado para antialiasing
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel; // Se sigue usando para el panel de error
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import PokeBody.domain.Trainer;

public class ScenarioSelectionPanel extends JPanel {
    private SwingGUI parentFrame;
    private Trainer player1Trainer;
    private Trainer player2Trainer;

    private JPanel scenarioGridPanel;
    private JButton backButton;

    private BufferedImage panelBackgroundImage; // Para el fondo principal del panel

    private static final String SCENARIOS_RESOURCE_PATH = "/PokeEscenarios/"; // Mantener la barra inicial para getResource
    // SCENARIO_THUMBNAIL_SIZE ya no se usará para fijar el tamaño, pero puede servir de referencia mínima.
    // private static final Dimension SCENARIO_THUMBNAIL_SIZE = new Dimension(200, 150);
    private static final int GRID_COLS = 2;

    private static final Color PIXEL_BLUE_DARK_DEFAULT_BG = Color.decode("#001F5C");
    private static final Color PIXEL_BLUE_LIGHT = Color.decode("#00A8E8"); // Usado para el fondo del grid si no es transparente
    private static final Color PIXEL_YELLOW = Color.decode("#FFDE00");
    private static final Color PIXEL_WHITE = Color.WHITE;
    private static final Color PIXEL_BLACK = Color.decode("#000000");
    private static final Border PIXEL_BORDER_RAISED = BorderFactory.createRaisedBevelBorder();
    private static final Border PIXEL_BORDER_LINE_BLACK = BorderFactory.createLineBorder(PIXEL_BLACK, 2);

    // Clase interna para dibujar imágenes de escenario escalables
    private class ScalableScenarioLabel extends JLabel {
        private BufferedImage scenarioImage;
        private boolean imageLoaded = false;

        public ScalableScenarioLabel() {
            super();
            this.setOpaque(false); // El label en sí es transparente
        }

        public void setScenarioImage(BufferedImage image) {
            this.scenarioImage = image;
            this.imageLoaded = (image != null);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Llama al paintComponent del JLabel (importante si tiene texto o bordes)
            
            if (imageLoaded && scenarioImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Mejor calidad para escalar
                
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = scenarioImage.getWidth();
                int imgHeight = scenarioImage.getHeight();

                if (panelWidth <= 0 || panelHeight <= 0 || imgWidth <= 0 || imgHeight <= 0) {
                    g2d.dispose();
                    return;
                }

                double imgAspect = (double) imgWidth / imgHeight;
                double panelAspect = (double) panelWidth / panelHeight;

                int drawWidth = panelWidth;
                int drawHeight = panelHeight;

                if (imgAspect > panelAspect) { // La imagen es más ancha que el panel
                    drawHeight = (int) (panelWidth / imgAspect);
                } else { // La imagen es más alta o tiene la misma proporción que el panel
                    drawWidth = (int) (panelHeight * imgAspect);
                }
                
                // Centrar la imagen
                int x = (panelWidth - drawWidth) / 2;
                int y = (panelHeight - drawHeight) / 2;

                g2d.drawImage(scenarioImage, x, y, drawWidth, drawHeight, this);
                g2d.dispose();
            } else {
                // Opcional: Dibujar un placeholder si la imagen no se carga
                g.setColor(Color.GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.drawString("Escenario no disponible", 10, getHeight() / 2);
            }
        }
    }


    public ScenarioSelectionPanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        loadMainBackgroundImage();
        setLayout(new GridBagLayout());
        // setBackground(PIXEL_BLUE_DARK_DEFAULT_BG); // El fondo se pinta en paintComponent
        initComponents();
    }
    
    private void loadMainBackgroundImage() {
        try {
            panelBackgroundImage = null; 
            String backgroundPath = "/backgrounds/Fondo-peleas.png";
            URL bgUrl = getClass().getResource(backgroundPath);
            
            if (bgUrl == null) {
                String pathForClassLoader = backgroundPath.startsWith("/") ? backgroundPath.substring(1) : backgroundPath;
                bgUrl = getClass().getClassLoader().getResource(pathForClassLoader);
            }

            if (bgUrl != null) {
                System.out.println("[ScenarioSelectionPanel] URL del fondo principal encontrada: " + bgUrl.toExternalForm());
                try (InputStream bgInputStream = bgUrl.openStream()) {
                    panelBackgroundImage = ImageIO.read(bgInputStream);
                    if (panelBackgroundImage != null) {
                        System.out.println("[ScenarioSelectionPanel] Fondo principal cargado CORRECTAMENTE desde: " + backgroundPath);
                    } else {
                        System.err.println("[ScenarioSelectionPanel] Error: ImageIO.read devolvió null para el fondo principal (" + backgroundPath + ").");
                    }
                } catch (IOException ioe) {
                    System.err.println("[ScenarioSelectionPanel] Error de IO al leer el stream del fondo principal (" + backgroundPath + "): " + ioe.getMessage());
                    ioe.printStackTrace();
                }
            } else {
                System.err.println("[ScenarioSelectionPanel] Error CRÍTICO: No se encontró el recurso de fondo principal en: " + backgroundPath);
            }
        } catch (Exception e) {
            System.err.println("[ScenarioSelectionPanel] Excepción inesperada durante la carga de la imagen de fondo principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (panelBackgroundImage != null) {
            g.drawImage(panelBackgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(PIXEL_BLUE_DARK_DEFAULT_BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            Font errorFont = getFont();
             if (parentFrame != null && parentFrame.getPixelArtFont() != null) {
                errorFont = parentFrame.getPixelArtFont().deriveFont(12f);
            } else if (errorFont == null) {
                errorFont = new Font("Monospaced", Font.BOLD, 12);
            }
            g.setFont(errorFont);
            g.drawString("Error: Fondo de ScenarioSelectionPanel ('/backgrounds/Fondo-peleas.png') no cargado.", 20, 20);
        }
    }

    public void setSelectedTrainers(Trainer p1, Trainer p2) {
        this.player1Trainer = p1;
        this.player2Trainer = p2;
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        Font pixelFont = parentFrame.getPixelArtFont();
        Font titleFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 18f) : new Font("Monospaced", Font.BOLD, 18);
        Font buttonFont = (pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 12f) : new Font("Monospaced", Font.PLAIN, 12);

        JLabel titleLabel = new JLabel("SELECCIONA EL ESCENARIO DE COMBATE");
        titleLabel.setForeground(PIXEL_YELLOW);
        titleLabel.setFont(titleFont);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false); // Hacer transparente para ver el fondo del panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GRID_COLS; // El título puede ocupar el ancho de la cuadrícula
        gbc.anchor = GridBagConstraints.PAGE_START; // Anclar arriba
        gbc.weighty = 0.05; // Darle un poco de peso para el espacio superior
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(titleLabel, gbc);

        scenarioGridPanel = new JPanel(new GridLayout(0, GRID_COLS, 15, 15)); 
        scenarioGridPanel.setOpaque(false); // Hacer transparente para ver el fondo del panel principal
        // scenarioGridPanel.setBackground(PIXEL_BLUE_LIGHT); // Opcional: si quieres un color de fondo para el grid
        // scenarioGridPanel.setBorder(PIXEL_BORDER_LINE_BLACK); // Opcional: borde para el grid
        loadAndDisplayScenarios(); 

        JScrollPane scrollPane = new JScrollPane(scenarioGridPanel);
        scrollPane.setOpaque(false); // ScrollPane transparente
        scrollPane.getViewport().setOpaque(false); // Viewport transparente
        // scrollPane.setPreferredSize(new Dimension(GRID_COLS * (200 + 20) + 40, 400)); // Ajustar tamaño si es necesario
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Sin borde para el scrollpane

        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.9; // Darle más peso al scrollpane para que ocupe espacio
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER; // Centrar el scrollpane
        gbc.insets = new Insets(10, 40, 10, 40); // Más padding horizontal para el scrollpane
        add(scrollPane, gbc);

        backButton = new JButton("Volver a Selección de Equipo");
        backButton.setFont(buttonFont);
        backButton.setBackground(PIXEL_WHITE);
        backButton.setBorder(PIXEL_BORDER_RAISED);
        backButton.addActionListener(e -> parentFrame.cardLayout.show(parentFrame.mainPanel, "TEAM_SELECT"));

        gbc.gridy = 2;
        gbc.weighty = 0.05; // Peso para el botón inferior
        gbc.fill = GridBagConstraints.NONE; // No rellenar, usar tamaño preferido
        gbc.anchor = GridBagConstraints.PAGE_END; // Anclar abajo
        gbc.insets = new Insets(10, 10, 10, 10); // Resetear insets
        add(backButton, gbc);
    }

    private void loadAndDisplayScenarios() {
        scenarioGridPanel.removeAll(); 
        String[] scenarioNames = {"Hierba", "Bosque", "Cueva", "Gimnasio_1"}; // Nombres de archivo sin extensión

        System.out.println("[ScenarioSelectionPanel] Intentando cargar escenarios desde: " + SCENARIOS_RESOURCE_PATH);

        for (String scenarioName : scenarioNames) {
            String resourcePath = SCENARIOS_RESOURCE_PATH + scenarioName + ".png";
            System.out.println("[ScenarioSelectionPanel] Buscando recurso: " + resourcePath);
            
            BufferedImage img = null;
            try {
                URL imageUrl = getClass().getResource(resourcePath);
                if (imageUrl == null) { // Intento con ClassLoader si getResource falla
                    String pathForClassLoader = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                    imageUrl = getClass().getClassLoader().getResource(pathForClassLoader);
                }

                if (imageUrl != null) {
                    img = ImageIO.read(imageUrl);
                    if (img == null) {
                         System.err.println("[ScenarioSelectionPanel] Error: ImageIO.read devolvió null para: " + resourcePath);
                    }
                } else {
                    System.err.println("[ScenarioSelectionPanel] Error Crítico: Recurso de escenario NO ENCONTRADO: " + resourcePath);
                }
            } catch (IOException e) {
                System.err.println("[ScenarioSelectionPanel] Error de IO al leer la imagen del escenario: " + resourcePath + " - " + e.getMessage());
            }

            JPanel scenarioPanel = createScenarioPreviewPanel(scenarioName, img);
            scenarioGridPanel.add(scenarioPanel);
            if (img != null) {
                 System.out.println("[ScenarioSelectionPanel] Escenario '" + scenarioName + "' añadido al panel.");
            } else {
                 System.out.println("[ScenarioSelectionPanel] Escenario '" + scenarioName + "' añadido al panel (imagen no cargada).");
            }
        }
        scenarioGridPanel.revalidate();
        scenarioGridPanel.repaint();
    }

    private JPanel createScenarioPreviewPanel(String scenarioName, BufferedImage scenarioImage) {
        JPanel panel = new JPanel(new BorderLayout(0, 5)); // Sin gap horizontal, 5px vertical
        panel.setOpaque(false); // Hacer el panel de previsualización transparente
        // panel.setBorder(PIXEL_BORDER_LINE_BLACK); // Borde opcional

        ScalableScenarioLabel imageLabel = new ScalableScenarioLabel();
        if (scenarioImage != null) {
            imageLabel.setScenarioImage(scenarioImage);
        }
        // Darle un tamaño mínimo preferido para que el GridLayout sepa cómo distribuirlos inicialmente
        imageLabel.setPreferredSize(new Dimension(180, 135)); // Ajusta según sea necesario

        panel.add(imageLabel, BorderLayout.CENTER);

        JLabel nameLabel = new JLabel(scenarioName, SwingConstants.CENTER);
        Font pixelFont = parentFrame.getPixelArtFont();
        nameLabel.setFont((pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11));
        nameLabel.setForeground(PIXEL_WHITE); // Texto blanco para mejor contraste con fondos oscuros
        nameLabel.setOpaque(false);
        panel.add(nameLabel, BorderLayout.SOUTH);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Scenario selected: " + scenarioName);
                if (player1Trainer != null && player2Trainer != null) {
                    parentFrame.showCombatModeSelection(player1Trainer, player2Trainer, scenarioName);
                } else {
                     System.err.println("Error: Trainers not set when scenario was selected.");
                     JOptionPane.showMessageDialog(parentFrame,
                         "Error interno: No se pudieron obtener los equipos.",
                         "Error de Configuración", JOptionPane.ERROR_MESSAGE);
                     parentFrame.cardLayout.show(parentFrame.mainPanel, "TEAM_SELECT"); 
                }
            }
             @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBorder(BorderFactory.createLineBorder(PIXEL_YELLOW, 3)); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBorder(null); // Quitar borde o volver al original
            }
        });
        return panel;
    }

    // createErrorPanel ya no es necesario si ScalableScenarioLabel maneja el placeholder
}
