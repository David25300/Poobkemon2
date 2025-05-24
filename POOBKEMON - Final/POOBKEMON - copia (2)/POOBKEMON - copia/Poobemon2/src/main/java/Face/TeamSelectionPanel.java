package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image; 
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream; 
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport; // Necesario para obtener el viewport
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.Type;
import PokeBody.domain.items.HyperPotion;
import PokeBody.domain.items.Potion;
import PokeBody.domain.items.Revive;
import PokeBody.domain.items.SuperPotion;

public class TeamSelectionPanel extends JPanel {
    private SwingGUI parentFrame;
    private List<Pokemon> allAvailablePokemons;
    private List<Pokemon> filteredPokemons;
    private List<Pokemon> currentPagePokemons;
    private List<Item> allAvailableItemsSystem;

    private ScrollableGridLayoutJPanel pokemonGridPanel;
    private JLabel pageInfoLabel;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JTextField searchField;
    private JButton finalizeSelectionButton;

    private JWindow previewWindow;
    private JLabel previewGifLabel;
    private JTextArea previewInfoArea;
    private static final Dimension PREVIEW_SIZE = new Dimension(250, 300);
    private static final Dimension GIF_SIZE = new Dimension(150, 150);
    private static final String POKEMON_ANIMATIONS_PATH = "/PokeAnimations/";

    private JPanel player1TeamDisplayPanel;
    private JPanel player2TeamDisplayPanel;
    private List<Pokemon> player1TeamSelection;
    private List<Pokemon> player2TeamSelection;
    private static final int MAX_TEAM_SIZE = 6;
    private ImageIcon emptySlotIcon;

    private JPanel player1SelectPanel, player2SelectPanel, cancelSelectPanel;
    private JButton selectPlayer1Button, selectPlayer2Button, cancelButton;
    private Pokemon selectedPokemonForTeam;

    private JPanel player1ItemCheckboxPanel; // Panel interno para checkboxes de J1
    private Map<Item, JCheckBox> player1ItemCheckboxes;
    private Map<String, Integer> player1SelectedItemCounts;

    private JPanel player2ItemCheckboxPanel; // Panel interno para checkboxes de J2
    private Map<Item, JCheckBox> player2ItemCheckboxes;
    private Map<String, Integer> player2SelectedItemCounts;

    private static final int POKEMONS_PER_PAGE = 20;
    private int currentPage = 0;
    private static final int GRID_COLS = 5;
    private static final Dimension MINIATURE_PREFERRED_SIZE = new Dimension(100, 100); 

    private static final Color PIXEL_BLUE_DARK_DEFAULT_BG = Color.decode("#001F5C"); 
    private static final Color PIXEL_BLUE_LIGHT = Color.decode("#00A8E8");
    private static final Color PIXEL_YELLOW = Color.decode("#FFDE00");
    private static final Color PIXEL_WHITE = Color.WHITE;
    private static final Color PIXEL_BLACK = Color.decode("#000000");
    private static final Color PIXEL_GRAY_LIGHT = Color.decode("#DDDDDD");
    private static final Color PIXEL_GRAY_DARK = Color.decode("#AAAAAA");
    private static final Color PIXEL_GREEN = Color.decode("#00C000");
    private static final Color PIXEL_RED = Color.decode("#CC0000");

    private static final Border PIXEL_BORDER_RAISED = BorderFactory.createRaisedBevelBorder();
    private static final Border PIXEL_BORDER_LOWERED = BorderFactory.createLoweredBevelBorder();
    private static final Border PIXEL_BORDER_LINE_BLACK = BorderFactory.createLineBorder(PIXEL_BLACK, 2);
    private static final Border PIXEL_BORDER_LINE_GRAY = BorderFactory.createLineBorder(PIXEL_GRAY_DARK, 2);

    private BufferedImage blueFrameImage;
    private BufferedImage redFrameImage;
    private BufferedImage panelBackgroundImage; 
    private BufferedImage marcoNormalFrameImage; 

    // Clase interna ScalableImageLabel (sin cambios)
    private class ScalableImageLabel extends JLabel {
        private BufferedImage masterImage;
        private BufferedImage frameImage;
        private boolean imageLoaded = false;
        private Font fallbackFont;
        private String fallbackText = "?";
        private static final int INSET_PERCENTAGE = 8; 

        public ScalableImageLabel(Font fallbackFont) {
            super();
            this.fallbackFont = fallbackFont;
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        public void setMasterImage(BufferedImage masterImage, String errorText) {
            this.masterImage = masterImage;
            this.imageLoaded = (masterImage != null);
            this.fallbackText = errorText;
            if (!imageLoaded) {
                setText(this.fallbackText);
                setFont(fallbackFont);
                setForeground(this.fallbackText.equals("X") ? PIXEL_RED : PIXEL_GRAY_DARK);
            } else {
                setText(null);
            }
            repaint();
        }

        public void setFrameImage(BufferedImage frameImage) {
            this.frameImage = frameImage;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            
            int labelWidth = getWidth();
            int labelHeight = getHeight();
            if (labelWidth <= 0 || labelHeight <= 0) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); 


            if (frameImage != null) {
                g2d.drawImage(frameImage, 0, 0, labelWidth, labelHeight, null);
            }

            if (imageLoaded && masterImage != null) {
                int insetX = labelWidth * INSET_PERCENTAGE / 100;
                int insetY = labelHeight * INSET_PERCENTAGE / 100;
                int pokemonAreaX = insetX;
                int pokemonAreaY = insetY;
                int pokemonAreaWidth = labelWidth - (2 * insetX);
                int pokemonAreaHeight = labelHeight - (2 * insetY);

                if (pokemonAreaWidth <= 0 || pokemonAreaHeight <= 0) {
                    g2d.dispose();
                    return;
                }
                int imgWidth = masterImage.getWidth();
                int imgHeight = masterImage.getHeight();
                if (imgWidth <= 0 || imgHeight <= 0) {
                    g2d.dispose();
                    return;
                }
                double imgAspect = (double) imgWidth / imgHeight;
                double panelAspect = (double) pokemonAreaWidth / pokemonAreaHeight;
                int drawWidth = pokemonAreaWidth;
                int drawHeight = pokemonAreaHeight;
                if (imgAspect > panelAspect) {
                    drawHeight = (int) (pokemonAreaWidth / imgAspect);
                } else {
                    drawWidth = (int) (pokemonAreaHeight * imgAspect);
                }
                drawWidth = Math.max(1, drawWidth);
                drawHeight = Math.max(1, drawHeight);
                int x = pokemonAreaX + (pokemonAreaWidth - drawWidth) / 2;
                int y = pokemonAreaY + (pokemonAreaHeight - drawHeight) / 2;
                g2d.drawImage(masterImage, x, y, drawWidth, drawHeight, null);
            } else if (!imageLoaded && getText() != null && !getText().isEmpty()) {
                g2d.setFont(getFont());
                g2d.setColor(getForeground());
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getAscent(); 
                int xText = (labelWidth - textWidth) / 2;
                int yText = (labelHeight - fm.getHeight()) / 2 + textHeight; 
                g2d.drawString(getText(), xText, yText);
            }
            g2d.dispose();
        }
    }

    public TeamSelectionPanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        this.allAvailablePokemons = new ArrayList<>();
        this.filteredPokemons = new ArrayList<>();
        this.currentPagePokemons = new ArrayList<>();
        this.player1TeamSelection = new ArrayList<>();
        this.player2TeamSelection = new ArrayList<>();
        this.allAvailableItemsSystem = new ArrayList<>();
        this.player1ItemCheckboxes = new HashMap<>();
        this.player1SelectedItemCounts = new HashMap<>();
        this.player2ItemCheckboxes = new HashMap<>();
        this.player2SelectedItemCounts = new HashMap<>();
        emptySlotIcon = createEmptySlotIcon(60, 60);
        setLayout(new GridBagLayout());
        loadFrameImagesAndBackground();
        initSelectionButtons();
        initComponents();
        initPreviewWindow();
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
            g.drawString("Error: Fondo principal ('/backgrounds/Fondo-seleccion.png') no cargado.", 20, 20);
        }
    }

    private void loadFrameImagesAndBackground() {
        try {
            URL blueFrameUrl = getClass().getResource("/frames/Marco-Azul.png");
            if (blueFrameUrl != null) blueFrameImage = ImageIO.read(blueFrameUrl);
            else System.err.println("Error: No se encontró /frames/Marco-Azul.png");

            URL redFrameUrl = getClass().getResource("/frames/Marco-rojo.png");
            if (redFrameUrl != null) redFrameImage = ImageIO.read(redFrameUrl);
            else System.err.println("Error: No se encontró /frames/Marco-rojo.png");

            panelBackgroundImage = null; 
            String backgroundPath = "/backgrounds/Fondo-seleccion.png";
            URL bgPanelUrl = getClass().getResource(backgroundPath);
            
            if (bgPanelUrl == null) {
                String pathForClassLoader = backgroundPath.startsWith("/") ? backgroundPath.substring(1) : backgroundPath;
                bgPanelUrl = getClass().getClassLoader().getResource(pathForClassLoader);
            }

            if (bgPanelUrl != null) {
                try (InputStream bgInputStream = bgPanelUrl.openStream()) { 
                    panelBackgroundImage = ImageIO.read(bgInputStream);
                } catch (IOException ioe) {
                    System.err.println("Error de IO al leer el stream del fondo del TeamSelectionPanel (" + backgroundPath + "): " + ioe.getMessage());
                    panelBackgroundImage = null; 
                }
            } else {
                System.err.println("Error CRÍTICO: No se encontró el recurso de fondo del TeamSelectionPanel en: " + backgroundPath);
            }

            URL marcoNormalUrl = getClass().getResource("/frames/marco-normal.png");
            if (marcoNormalUrl != null) marcoNormalFrameImage = ImageIO.read(marcoNormalUrl);
            else System.err.println("Error: No se encontró /frames/marco-normal.png");

        } catch (IOException e) { 
            System.err.println("Error de IO al cargar imágenes de marco: " + e.getMessage());
        } catch (Exception e) { 
            System.err.println("Excepción inesperada durante la carga de imágenes: " + e.getMessage());
        }
    }

    private void initSelectionButtons() {
        Font buttonFont = (parentFrame.getPixelArtFont() != null) ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 12f) : new Font("Monospaced", Font.PLAIN, 12);
        Dimension buttonSize = new Dimension(180, 40);
        selectPlayer1Button = new JButton("Añadir a J1");
        selectPlayer1Button.setFont(buttonFont);
        selectPlayer1Button.setBackground(PIXEL_WHITE);
        selectPlayer1Button.setBorder(PIXEL_BORDER_RAISED);
        selectPlayer1Button.setPreferredSize(buttonSize);
        selectPlayer1Button.addActionListener(e -> addSelectedPokemonToTeam(player1TeamSelection, player1TeamDisplayPanel));
        player1SelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        player1SelectPanel.setOpaque(false);
        player1SelectPanel.add(selectPlayer1Button);
        player1SelectPanel.setVisible(false);
        selectPlayer2Button = new JButton("Añadir a J2");
        selectPlayer2Button.setFont(buttonFont);
        selectPlayer2Button.setBackground(PIXEL_WHITE);
        selectPlayer2Button.setBorder(PIXEL_BORDER_RAISED);
        selectPlayer2Button.setPreferredSize(buttonSize);
        selectPlayer2Button.addActionListener(e -> addSelectedPokemonToTeam(player2TeamSelection, player2TeamDisplayPanel));
        player2SelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        player2SelectPanel.setOpaque(false);
        player2SelectPanel.add(selectPlayer2Button);
        player2SelectPanel.setVisible(true); 
        cancelButton = new JButton("Cancelar");
        cancelButton.setFont(buttonFont);
        cancelButton.setBackground(PIXEL_RED);
        cancelButton.setForeground(PIXEL_WHITE);
        cancelButton.setBorder(PIXEL_BORDER_RAISED);
        cancelButton.setPreferredSize(new Dimension(120, 40));
        cancelButton.addActionListener(e -> hideSelectionButtons());
        cancelSelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        cancelSelectPanel.setOpaque(false);
        cancelSelectPanel.add(cancelButton);
        cancelSelectPanel.setVisible(false);
    }

    private void showSelectionButtons(Pokemon pokemon) {
        this.selectedPokemonForTeam = pokemon;
        hidePokemonPreview();
        player1SelectPanel.setVisible(true);
        player2SelectPanel.setVisible(true);
        cancelSelectPanel.setVisible(true);
        revalidate();
        repaint();
    }

    private void hideSelectionButtons() {
        player1SelectPanel.setVisible(false);
        player2SelectPanel.setVisible(false);
        cancelSelectPanel.setVisible(false);
        this.selectedPokemonForTeam = null;
        revalidate();
        repaint();
    }

    private void addSelectedPokemonToTeam(List<Pokemon> teamList, JPanel teamDisplayPanel) {
        if (selectedPokemonForTeam != null) {
            BufferedImage frame = null;
            if (teamDisplayPanel == player1TeamDisplayPanel) frame = blueFrameImage;
            else if (teamDisplayPanel == player2TeamDisplayPanel) frame = redFrameImage;
            addPokemonToTeamList(selectedPokemonForTeam, teamList, teamDisplayPanel, frame);
            hideSelectionButtons();
        }
    }

    private void addPokemonToTeamList(Pokemon pokemon, List<Pokemon> teamList, JPanel teamDisplayPanel, BufferedImage frame) {
        if (teamList.size() < MAX_TEAM_SIZE) {
            teamList.add(new Pokemon(pokemon)); 
            updateTeamDisplayPanel(teamList, teamDisplayPanel, frame);
        } else {
            JOptionPane.showMessageDialog(this, "El equipo ya está lleno (" + MAX_TEAM_SIZE + " Pokémon).", "Equipo Lleno", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void updateAllTeamDisplayPanels() {
        updateTeamDisplayPanel(player1TeamSelection, player1TeamDisplayPanel, blueFrameImage);
        updateTeamDisplayPanel(player2TeamSelection, player2TeamDisplayPanel, redFrameImage);
    }

    private void initComponents() {
        GridBagConstraints gbcRoot = new GridBagConstraints();
        gbcRoot.insets = new Insets(5, 5, 5, 5);
        Font pixelFont = parentFrame.getPixelArtFont();
        Font labelFont = (pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11);
        Font buttonFont = (pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11);
        Font titleFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 13f) : new Font("Monospaced", Font.BOLD, 13);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false); 
        player1TeamDisplayPanel = createTeamDisplayPanel("Equipo Jugador 1", titleFont);
        
        // Crear el contenedor de ítems para J1
        // player1ItemCheckboxPanel (campo de clase) se asigna DENTRO de createItemSelectionPanelContainer
        JPanel p1ItemContainer = createItemSelectionPanelContainer("Ítems Jugador 1", titleFont);
        
        leftPanel.add(player1TeamDisplayPanel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(p1ItemContainer); // Añadir el contenedor con JScrollPane
        
        gbcRoot.gridx = 0; gbcRoot.gridy = 0; gbcRoot.gridheight = 3;
        gbcRoot.weightx = 0.15; gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH; 
        gbcRoot.anchor = GridBagConstraints.NORTHWEST;
        add(leftPanel, gbcRoot);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false); 
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3));
        controlPanel.setOpaque(false); 
        prevPageButton = new JButton("←"); prevPageButton.setFont(buttonFont); prevPageButton.setBackground(PIXEL_WHITE); prevPageButton.setBorder(PIXEL_BORDER_RAISED); prevPageButton.setMargin(new Insets(2, 5, 2, 5)); prevPageButton.addActionListener(e -> navigatePage(-1)); controlPanel.add(prevPageButton);
        searchField = new JTextField(12); searchField.setFont(labelFont); searchField.setBorder(PIXEL_BORDER_LOWERED);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterAndDisplayPokemons(); }
            public void removeUpdate(DocumentEvent e) { filterAndDisplayPokemons(); }
            public void changedUpdate(DocumentEvent e) { filterAndDisplayPokemons(); }
        });
        JLabel searchLabel = new JLabel("Buscar:"); searchLabel.setFont(labelFont); searchLabel.setForeground(PIXEL_YELLOW); controlPanel.add(searchLabel); controlPanel.add(searchField);
        pageInfoLabel = new JLabel("Pág ?/?"); pageInfoLabel.setFont(labelFont); pageInfoLabel.setForeground(PIXEL_YELLOW); controlPanel.add(pageInfoLabel);
        nextPageButton = new JButton("→"); nextPageButton.setFont(buttonFont); nextPageButton.setBackground(PIXEL_WHITE); nextPageButton.setBorder(PIXEL_BORDER_RAISED); nextPageButton.setMargin(new Insets(2, 5, 2, 5)); nextPageButton.addActionListener(e -> navigatePage(1)); controlPanel.add(nextPageButton);
        centerPanel.add(controlPanel, BorderLayout.NORTH);

        ImageBackgroundPanel backgroundPanelForGrid = new ImageBackgroundPanel((Image) null); 
        backgroundPanelForGrid.setOpaque(false); 
        backgroundPanelForGrid.setLayout(new BorderLayout());
        
        pokemonGridPanel = new ScrollableGridLayoutJPanel(0, GRID_COLS, 8, 8);
        pokemonGridPanel.setOpaque(false); 
        backgroundPanelForGrid.add(pokemonGridPanel, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(backgroundPanelForGrid);
        scrollPane.getViewport().setOpaque(false); 
        scrollPane.setOpaque(false); 
        scrollPane.setBorder(PIXEL_BORDER_LINE_BLACK); 
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel teamActionButtonsPanel = new JPanel(new GridBagLayout());
        teamActionButtonsPanel.setOpaque(false); 
        GridBagConstraints tabGbc = new GridBagConstraints();
        tabGbc.insets = new Insets(0, 3, 0, 3); tabGbc.gridy = 0;
        tabGbc.gridx = 0; teamActionButtonsPanel.add(player1SelectPanel, tabGbc);
        tabGbc.gridx = 1; teamActionButtonsPanel.add(cancelSelectPanel, tabGbc);
        tabGbc.gridx = 2; teamActionButtonsPanel.add(player2SelectPanel, tabGbc);
        centerPanel.add(teamActionButtonsPanel, BorderLayout.SOUTH);
        gbcRoot.gridx = 1; gbcRoot.gridy = 0; gbcRoot.gridheight = 2;
        gbcRoot.weightx = 0.7; gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH; gbcRoot.anchor = GridBagConstraints.CENTER;
        add(centerPanel, gbcRoot);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false); 
        player2TeamDisplayPanel = createTeamDisplayPanel("Equipo Jugador 2", titleFont);
        
        // Crear el contenedor de ítems para J2
        JPanel p2ItemContainer = createItemSelectionPanelContainer("Ítems Jugador 2", titleFont);
        
        rightPanel.add(player2TeamDisplayPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(p2ItemContainer); // Añadir el contenedor con JScrollPane
        
        gbcRoot.gridx = 2; gbcRoot.gridy = 0; gbcRoot.gridheight = 3;
        gbcRoot.weightx = 0.15; gbcRoot.weighty = 1.0;
        gbcRoot.fill = GridBagConstraints.BOTH; 
        gbcRoot.anchor = GridBagConstraints.NORTHEAST;
        add(rightPanel, gbcRoot);

        finalizeSelectionButton = new JButton("Finalizar y Escoger Escenario");
        finalizeSelectionButton.setFont(buttonFont);
        finalizeSelectionButton.setBackground(PIXEL_YELLOW);
        finalizeSelectionButton.setForeground(PIXEL_BLACK);
        finalizeSelectionButton.setBorder(PIXEL_BORDER_RAISED);
        finalizeSelectionButton.addActionListener(e -> finalizeTeamAndItemSelection());
        gbcRoot.gridx = 1; gbcRoot.gridy = 2; gbcRoot.gridheight = 1;
        gbcRoot.weightx = 0.7; gbcRoot.weighty = 0.0;
        gbcRoot.fill = GridBagConstraints.HORIZONTAL; gbcRoot.anchor = GridBagConstraints.SOUTH;
        gbcRoot.insets = new Insets(8, 5, 8, 5);
        add(finalizeSelectionButton, gbcRoot);
        updateNavigationButtons();
    }

    private JPanel createTeamDisplayPanel(String title, Font titleFont) {
        JPanel panel = new JPanel(new GridLayout(MAX_TEAM_SIZE, 1, 3, 3));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(PIXEL_BORDER_LINE_BLACK, title, TitledBorder.CENTER, TitledBorder.TOP);
        titledBorder.setTitleColor(PIXEL_YELLOW);
        titledBorder.setTitleFont(titleFont);
        panel.setBorder(BorderFactory.createCompoundBorder(titledBorder, BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        panel.setBackground(PIXEL_BLUE_LIGHT); 
        panel.setPreferredSize(new Dimension(180, MAX_TEAM_SIZE * 80 + 20));
        panel.setMinimumSize(new Dimension(150, MAX_TEAM_SIZE * 70 + 20));
        for (int i = 0; i < MAX_TEAM_SIZE; i++) {
            JPanel emptySlotPlaceholder = new JPanel(new BorderLayout());
            emptySlotPlaceholder.setBackground(PIXEL_GRAY_LIGHT);
            emptySlotPlaceholder.setBorder(PIXEL_BORDER_LINE_GRAY);
            JLabel emptyIconLabel = new JLabel(emptySlotIcon);
            emptyIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptySlotPlaceholder.add(emptyIconLabel, BorderLayout.CENTER);
            panel.add(emptySlotPlaceholder);
        }
        return panel;
    }

    private void updateTeamDisplayPanel(List<Pokemon> teamList, JPanel teamPanel, BufferedImage frameForSlots) {
        teamPanel.removeAll();
        for (int i = 0; i < MAX_TEAM_SIZE; i++) {
            if (i < teamList.size()) {
                Pokemon pokemon = teamList.get(i);
                ScalableImageLabel pokemonSlotLabel = createTeamPokemonSlot(pokemon, i, teamList, teamPanel, frameForSlots);
                teamPanel.add(pokemonSlotLabel);
            } else {
                JPanel emptySlotPlaceholder = new JPanel(new BorderLayout());
                emptySlotPlaceholder.setBackground(PIXEL_GRAY_LIGHT);
                emptySlotPlaceholder.setBorder(PIXEL_BORDER_LINE_GRAY);
                JLabel emptyIconLabel = new JLabel(emptySlotIcon);
                emptyIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                emptySlotPlaceholder.add(emptyIconLabel, BorderLayout.CENTER);
                teamPanel.add(emptySlotPlaceholder);
            }
        }
        teamPanel.revalidate();
        teamPanel.repaint();
    }

    private ScalableImageLabel createTeamPokemonSlot(Pokemon pokemon, int slotIndex, List<Pokemon> teamList, JPanel teamDisplayPanel, BufferedImage frameImageToUse) {
        Font fallbackFontTeam = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 20f) : new Font("Monospaced", Font.PLAIN, 20);
        ScalableImageLabel iconLabel = new ScalableImageLabel(fallbackFontTeam);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(PIXEL_WHITE);
        iconLabel.setFrameImage(frameImageToUse);
        iconLabel.setPreferredSize(new Dimension(150, 70)); 

        String pokemonNameLower = pokemon.getNombre().toLowerCase().replace(" ", "-");
        String resourcePath = "/PokeMiniaturas/" + pokemonNameLower + ".png";
        BufferedImage img = null;
        String errorText = pokemon.getNombre().length() > 3 ? pokemon.getNombre().substring(0, 3) : pokemon.getNombre();
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                img = ImageIO.read(url);
                if (img == null) errorText = "?";
            } else {
                errorText = "?";
            }
        } catch (IOException e) {
            errorText = "X";
        }
        iconLabel.setMasterImage(img, errorText);
        iconLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                List<Pokemon> currentTeamList = null;
                BufferedImage currentFrameImage = null;
                if (teamDisplayPanel == player1TeamDisplayPanel) {
                    currentTeamList = player1TeamSelection;
                    currentFrameImage = blueFrameImage;
                } else if (teamDisplayPanel == player2TeamDisplayPanel) {
                    currentTeamList = player2TeamSelection;
                    currentFrameImage = redFrameImage;
                }
                if (currentTeamList != null && slotIndex < currentTeamList.size()) {
                    Pokemon clickedPokemon = currentTeamList.get(slotIndex);
                    Object[] options = {"Quitar del Equipo", "Ver/Cambiar Movimientos", "Cancelar"};
                    int choice = JOptionPane.showOptionDialog(parentFrame, "¿Qué deseas hacer con " + clickedPokemon.getNombre() + "?", "Acciones de Pokémon", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                    if (choice == 0) {
                        currentTeamList.remove(slotIndex);
                        updateTeamDisplayPanel(currentTeamList, teamDisplayPanel, currentFrameImage);
                    } else if (choice == 1) {
                        MoveManagementDialog moveDialog = new MoveManagementDialog(parentFrame, clickedPokemon);
                        moveDialog.setVisible(true);
                    }
                }
            }
        });
        return iconLabel;
    }

    public void loadAvailablePokemonsAndItems(List<Pokemon> pokemons, List<Item> systemItems) {
        this.allAvailablePokemons = (pokemons != null) ? new ArrayList<>(pokemons) : new ArrayList<>();
        this.filteredPokemons = new ArrayList<>(this.allAvailablePokemons);
        this.allAvailableItemsSystem = (systemItems != null) ? new ArrayList<>(systemItems) : new ArrayList<>();
        this.currentPage = 0; 
        filterAndDisplayPokemons();
        updateAllTeamDisplayPanels(); 
        // Asegurarse de pasar el panel correcto (el checkboxPanel interno)
        populateItemSelectionPanel(player1ItemCheckboxPanel, this.allAvailableItemsSystem, player1ItemCheckboxes, player1SelectedItemCounts);
        populateItemSelectionPanel(player2ItemCheckboxPanel, this.allAvailableItemsSystem, player2ItemCheckboxes, player2SelectedItemCounts);
    }

    private void filterAndDisplayPokemons() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            filteredPokemons = new ArrayList<>(allAvailablePokemons);
        } else {
            filteredPokemons = allAvailablePokemons.stream().filter(p -> p.getNombre().toLowerCase().contains(searchText)).collect(Collectors.toList());
        }

        int totalPokemons = filteredPokemons.size();
        int totalPages = (totalPokemons == 0) ? 1 : (int) Math.ceil((double) totalPokemons / POKEMONS_PER_PAGE);

        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
        if (currentPage < 0) currentPage = 0;

        int startIndex = currentPage * POKEMONS_PER_PAGE;
        int endIndex = Math.min(startIndex + POKEMONS_PER_PAGE, totalPokemons);

        if (startIndex < totalPokemons && startIndex <= endIndex) {
            currentPagePokemons = filteredPokemons.subList(startIndex, endIndex);
        } else {
            currentPagePokemons = new ArrayList<>(); 
        }

        pokemonGridPanel.removeAll();
        for (Pokemon pokemon : currentPagePokemons) {
            pokemonGridPanel.add(createPokemonMiniature(pokemon));
        }

        int currentGridSize = currentPagePokemons.size();
        if (currentGridSize > 0 && currentGridSize < POKEMONS_PER_PAGE) {
            int placeholdersForRowCompletion = (GRID_COLS - (currentGridSize % GRID_COLS)) % GRID_COLS;
            for (int i = 0; i < placeholdersForRowCompletion; i++) {
                JPanel emptyPanel = new JPanel();
                emptyPanel.setOpaque(false);
                emptyPanel.setPreferredSize(MINIATURE_PREFERRED_SIZE);
                pokemonGridPanel.add(emptyPanel);
            }
        } else if (currentGridSize == 0 && totalPokemons == 0 && searchText.isEmpty()) {
            for (int i = 0; i < POKEMONS_PER_PAGE; i++) {
                JPanel emptyPanel = new JPanel();
                emptyPanel.setOpaque(false);
                emptyPanel.setPreferredSize(MINIATURE_PREFERRED_SIZE);
                pokemonGridPanel.add(emptyPanel);
            }
        }

        pageInfoLabel.setText(String.format("Pág %d/%d", currentPage + 1, totalPages));
        updateNavigationButtons();
        pokemonGridPanel.revalidate();
        pokemonGridPanel.repaint();
        if (pokemonGridPanel.getParent() != null && pokemonGridPanel.getParent().getParent() instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) pokemonGridPanel.getParent().getParent();
            scrollPane.revalidate();
            scrollPane.repaint();
        }
    }


    private ScalableImageLabel createPokemonMiniature(Pokemon pokemon) {
        Font fallbackFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 30f) : new Font("Monospaced", Font.PLAIN, 30);
        ScalableImageLabel iconLabel = new ScalableImageLabel(fallbackFont);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(PIXEL_WHITE);
        iconLabel.setPreferredSize(MINIATURE_PREFERRED_SIZE);
        if (marcoNormalFrameImage != null) {
            iconLabel.setFrameImage(marcoNormalFrameImage);
        }

        String pokemonNameOriginal = pokemon.getNombre();
        if (pokemonNameOriginal == null || pokemonNameOriginal.trim().isEmpty()) {
            iconLabel.setMasterImage(null, "N/A");
        } else {
            String pokemonNameLower = pokemonNameOriginal.toLowerCase().replace(" ", "-");
            String resourcePath = "/PokeMiniaturas/" + pokemonNameLower + ".png";
            BufferedImage img = null;
            String errorText = "?";
            try {
                URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    img = ImageIO.read(url);
                    if (img == null) errorText = "?";
                } else {
                    errorText = "?";
                }
            } catch (IOException e) {
                errorText = "X";
            }
            iconLabel.setMasterImage(img, errorText);
        }
        iconLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!player1SelectPanel.isVisible()) showPokemonPreview(pokemon, e.getLocationOnScreen());
            }
            public void mouseExited(MouseEvent e) {
                if (!player1SelectPanel.isVisible()) hidePokemonPreview();
            }
            public void mouseClicked(MouseEvent e) {
                showSelectionButtons(pokemon);
            }
        });
        return iconLabel;
    }

    private void navigatePage(int direction) {
        int totalPokemons = filteredPokemons.size();
        int totalPages = (totalPokemons == 0) ? 1 : (int) Math.ceil((double) totalPokemons / POKEMONS_PER_PAGE);
        int newPage = currentPage + direction;

        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            filterAndDisplayPokemons();
        }
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        int totalPokemons = filteredPokemons.size();
        int totalPages = (totalPokemons == 0) ? 1 : (int) Math.ceil((double) totalPokemons / POKEMONS_PER_PAGE);
        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);
    }

    private void initPreviewWindow() {
        previewWindow = new JWindow(parentFrame);
        previewWindow.setSize(PREVIEW_SIZE);
        previewWindow.setLayout(new BorderLayout());
        previewWindow.setAlwaysOnTop(true);
        previewWindow.getContentPane().setBackground(PIXEL_GRAY_LIGHT);
        previewWindow.getRootPane().setBorder(PIXEL_BORDER_LINE_BLACK);
        previewGifLabel = new JLabel();
        previewGifLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewGifLabel.setVerticalAlignment(SwingConstants.CENTER);
        previewGifLabel.setPreferredSize(GIF_SIZE);
        previewWindow.add(previewGifLabel, BorderLayout.NORTH);
        previewInfoArea = new JTextArea();
        previewInfoArea.setEditable(false);
        previewInfoArea.setLineWrap(true);
        previewInfoArea.setWrapStyleWord(true);
        previewInfoArea.setBackground(PIXEL_GRAY_LIGHT);
        previewInfoArea.setForeground(PIXEL_BLACK);
        if (parentFrame.getPixelArtFont() != null) {
            previewInfoArea.setFont(parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 9f));
        } else {
            previewInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
        }
        previewWindow.add(new JScrollPane(previewInfoArea), BorderLayout.CENTER);
        previewWindow.setVisible(false);
    }

    private void showPokemonPreview(Pokemon pokemon, Point location) {
        String pokemonNameOriginal = pokemon.getNombre();
        if (pokemonNameOriginal == null || pokemonNameOriginal.trim().isEmpty()) {
            previewGifLabel.setIcon(null); previewGifLabel.setText("N/A");
            if (parentFrame.getPixelArtFont() != null) previewGifLabel.setFont(parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 10f));
            else previewGifLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            previewGifLabel.setPreferredSize(GIF_SIZE); previewWindow.pack();
            previewInfoArea.setText("Error: Nombre de Pokémon inválido.");
            previewWindow.setVisible(true); return;
        }
        String pokemonNameLower = pokemonNameOriginal.toLowerCase().replace(" ", "-");
        String gifResourcePath = POKEMON_ANIMATIONS_PATH + pokemonNameLower + ".gif";
        URL gifUrl = getClass().getResource(gifResourcePath);
        if (gifUrl != null) {
            ImageIcon gifIcon = new ImageIcon(gifUrl);
            if (gifIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                previewGifLabel.setIcon(null); previewGifLabel.setText("GIF Err");
            } else if (gifIcon.getIconWidth() <= 0 || gifIcon.getIconHeight() <= 0) {
                previewGifLabel.setIcon(null); previewGifLabel.setText("GIF Dim");
            } else {
                previewGifLabel.setIcon(gifIcon); previewGifLabel.setText(null);
                previewGifLabel.setPreferredSize(new Dimension(Math.min(gifIcon.getIconWidth(), GIF_SIZE.width), Math.min(gifIcon.getIconHeight(), GIF_SIZE.height)));
            }
        } else {
            previewGifLabel.setIcon(null); previewGifLabel.setText("GIF no disp.");
        }
        if (previewGifLabel.getIcon() == null) {
            if (parentFrame.getPixelArtFont() != null) previewGifLabel.setFont(parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 10f));
            else previewGifLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            previewGifLabel.setForeground(PIXEL_BLACK);
            previewGifLabel.setPreferredSize(GIF_SIZE);
        }
        previewWindow.pack(); 
        StringBuilder info = new StringBuilder();
        info.append("Nombre: ").append(pokemon.getNombre()).append("\n");
        info.append("Nivel: ").append(pokemon.getNivel()).append("\n");
        info.append("Tipos: ");
        if (pokemon.getTipos() != null && !pokemon.getTipos().isEmpty()) info.append(pokemon.getTipos().stream().map(Type.Tipo::name).collect(Collectors.joining(", ")));
        else info.append("N/A");
        info.append("\nStats:\n");
        info.append("  PS: ").append(pokemon.getHpMax()).append("\n");
        info.append("  Atk: ").append(pokemon.getAtaque()).append(" Def: ").append(pokemon.getDefensa()).append("\n");
        info.append("  SpA: ").append(pokemon.getAtaqueEspecial()).append(" SpD: ").append(pokemon.getDefensaEspecial()).append("\n");
        info.append("  Vel: ").append(pokemon.getVelocidad()).append("\n");
        previewInfoArea.setText(info.toString());
        previewInfoArea.setCaretPosition(0);
        int x = location.x + 20; int y = location.y + 20;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + previewWindow.getWidth() > screenSize.width) x = location.x - previewWindow.getWidth() - 20;
        if (y + previewWindow.getHeight() > screenSize.height) y = location.y - previewWindow.getHeight() - 20;
        if (x < 0) x = 0; if (y < 0) y = 0;
        previewWindow.setLocation(x, y);
        previewWindow.setVisible(true);
    }

    private void hidePokemonPreview() {
        previewWindow.setVisible(false);
    }

    private ImageIcon createEmptySlotIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(PIXEL_GRAY_DARK);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.dispose();
        return new ImageIcon(img);
    }

    private JPanel createItemSelectionPanelContainer(String title, Font titleFont) {
        JPanel containerPanel = new JPanel(new BorderLayout()); 
        TitledBorder titledBorder = BorderFactory.createTitledBorder(PIXEL_BORDER_LINE_BLACK, title, TitledBorder.CENTER, TitledBorder.TOP);
        titledBorder.setTitleColor(PIXEL_YELLOW);
        titledBorder.setTitleFont(titleFont);
        containerPanel.setBorder(BorderFactory.createCompoundBorder(titledBorder, BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        containerPanel.setBackground(PIXEL_BLUE_LIGHT); 
        
        containerPanel.setPreferredSize(new Dimension(180, 200)); 
        containerPanel.setMinimumSize(new Dimension(150, 150));
        containerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel internalCheckboxPanel = new JPanel();
        internalCheckboxPanel.setLayout(new BoxLayout(internalCheckboxPanel, BoxLayout.Y_AXIS));
        internalCheckboxPanel.setOpaque(false); 

        JScrollPane scrollPane = new JScrollPane(internalCheckboxPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false); 
        scrollPane.getViewport().setOpaque(false); 
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 

        containerPanel.add(scrollPane, BorderLayout.CENTER);
        
        if (title.contains("Jugador 1")) {
            this.player1ItemCheckboxPanel = internalCheckboxPanel; 
        } else {
            this.player2ItemCheckboxPanel = internalCheckboxPanel;
        }
        return containerPanel; 
    }

    private void populateItemSelectionPanel(JPanel checkboxPanel, List<Item> items, Map<Item, JCheckBox> itemCheckboxes, Map<String, Integer> selectedItemCounts) {
        checkboxPanel.removeAll(); 
        itemCheckboxes.clear();
        selectedItemCounts.clear(); 
        Font itemFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 10f) : new Font("Monospaced", Font.PLAIN, 10);
        
        if (items == null || items.isEmpty()) {
            JLabel noItemsLabel = new JLabel("No hay ítems.");
            noItemsLabel.setFont(itemFont);
            noItemsLabel.setForeground(PIXEL_WHITE);
            checkboxPanel.add(noItemsLabel);
        } else {
            for (Item item : items) {
                JCheckBox checkBox = new JCheckBox(item.getNombre());
                checkBox.setFont(itemFont);
                checkBox.setOpaque(false); 
                checkBox.setForeground(PIXEL_WHITE); 
                checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                checkBox.addActionListener(e -> {
                    handleItemSelection(item, checkBox.isSelected(), selectedItemCounts);
                    if (!validateItemLimits(item, selectedItemCounts)) {
                        checkBox.setSelected(false); 
                        handleItemSelection(item, false, selectedItemCounts); 
                        String limitType = (item instanceof Revive) ? "Revivir" : item.getNombre();
                        int max = 2; 
                        if (item instanceof Revive) max = 1;
                        
                        JOptionPane.showMessageDialog(parentFrame, "Solo puedes llevar " + max + " de " + limitType + ".", "Límite de Ítem Alcanzado", JOptionPane.WARNING_MESSAGE);
                    }
                });
                itemCheckboxes.put(item, checkBox);
                checkboxPanel.add(checkBox);
                checkboxPanel.add(Box.createRigidArea(new Dimension(0, 3))); 
            }
        }

        checkboxPanel.revalidate();
        // No es necesario repintar checkboxPanel directamente si es transparente y su padre se repinta.

        if (checkboxPanel.getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) checkboxPanel.getParent();
            if (viewport.getParent() instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) viewport.getParent();
                // Re-establecer la vista puede ayudar a JScrollPane a actualizarse
                scrollPane.setViewportView(checkboxPanel); 
                scrollPane.revalidate();
                scrollPane.repaint();

                if (scrollPane.getParent() != null) {
                    scrollPane.getParent().revalidate();
                    scrollPane.getParent().repaint();
                }
            }
        } else { // Fallback si la estructura no es la esperada
             checkboxPanel.repaint(); // Repintar por si acaso
        }
    }


    private void handleItemSelection(Item item, boolean isSelected, Map<String, Integer> selectedItemCounts) {
        String itemName = item.getName(); 
        int currentCount = selectedItemCounts.getOrDefault(itemName, 0);
        if (isSelected) {
            selectedItemCounts.put(itemName, currentCount + 1);
        } else {
            selectedItemCounts.put(itemName, Math.max(0, currentCount - 1)); 
        }
    }

    private boolean validateItemLimits(Item item, Map<String, Integer> selectedItemCounts) {
        String itemName = item.getName();
        int count = selectedItemCounts.getOrDefault(itemName, 0);

        if (item instanceof Revive) return count <= 1;
        else if (item instanceof Potion || item instanceof SuperPotion || item instanceof HyperPotion) return count <= 2;
        return true; 
    }

    private void finalizeTeamAndItemSelection() {
        if (player1TeamSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El Jugador 1 debe seleccionar al menos un Pokémon.", "Selección Incompleta (J1)", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Item> p1SelectedItems = getSelectedItems(player1ItemCheckboxes, player1SelectedItemCounts);
        List<Item> p2SelectedItems = getSelectedItems(player2ItemCheckboxes, player2SelectedItemCounts);

        if (!validateFinalItemCounts(p1SelectedItems, "Jugador 1")) return;
        if (!validateFinalItemCounts(p2SelectedItems, "Jugador 2")) return;

        Trainer trainer1 = new Trainer("Jugador 1", new ArrayList<>(player1TeamSelection), p1SelectedItems);
        Trainer trainer2 = new Trainer("Jugador 2", new ArrayList<>(player2TeamSelection), p2SelectedItems);

        parentFrame.showScenarioSelection(trainer1, trainer2);
    }

    private List<Item> getSelectedItems(Map<Item, JCheckBox> checkboxes, Map<String, Integer> counts) {
        List<Item> selectedItems = new ArrayList<>();
        for (Map.Entry<Item, JCheckBox> entry : checkboxes.entrySet()) {
            Item item = entry.getKey();
            int count = counts.getOrDefault(item.getName(), 0); 
            for (int i = 0; i < count; i++) {
                selectedItems.add(item); 
            }
        }
        return selectedItems;
    }

    private boolean validateFinalItemCounts(List<Item> items, String playerName) {
        Map<String, Long> finalItemCountsByName = items.stream()
                                             .collect(Collectors.groupingBy(Item::getName, Collectors.counting()));

        for (Map.Entry<String, Long> entry : finalItemCountsByName.entrySet()) {
            String itemNameKey = entry.getKey();
            Long count = entry.getValue();

            Item sampleItem = allAvailableItemsSystem.stream()
                                .filter(it -> it.getName().equals(itemNameKey))
                                .findFirst().orElse(null);

            if (sampleItem != null) {
                if (sampleItem instanceof Revive && count > 1) {
                    JOptionPane.showMessageDialog(this, playerName + ": Solo puedes llevar 1 Revivir.", "Límite de Ítem Excedido", JOptionPane.WARNING_MESSAGE);
                    return false;
                } else if (sampleItem instanceof Potion && count > 2) { 
                    JOptionPane.showMessageDialog(this, playerName + ": Solo puedes llevar 2 de " + sampleItem.getNombre() + ".", "Límite de Ítem Excedido", JOptionPane.WARNING_MESSAGE);
                    return false;
                } else if (sampleItem instanceof SuperPotion && count > 2) { 
                    JOptionPane.showMessageDialog(this, playerName + ": Solo puedes llevar 2 de " + sampleItem.getNombre() + ".", "Límite de Ítem Excedido", JOptionPane.WARNING_MESSAGE);
                    return false;
                } else if (sampleItem instanceof HyperPotion && count > 2) { 
                    JOptionPane.showMessageDialog(this, playerName + ": Solo puedes llevar 2 de " + sampleItem.getNombre() + ".", "Límite de Ítem Excedido", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }
}