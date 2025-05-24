package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics; // Necesario para paintComponent
import java.awt.GridBagConstraints; // Necesario para paintComponent
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL; // Necesario para cargar recursos
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import PokeBody.Data.SaveManager;
import PokeBody.Data.SaveManager.SaveData;

public class LoadGamePanel extends JPanel {
    private SwingGUI parentFrame;
    private static final int NUM_SAVE_SLOTS = 5;
    private static final int POKEMON_ICON_SIZE = 30;
    private static final int SLOT_BUTTON_HEIGHT = 80;
    private static final int HORIZONTAL_PADDING = 80;
    private List<SaveData> loadedSaveDataList; 

    private BufferedImage panelBackgroundImage; // NUEVO: Para la imagen de fondo del panel

    // Pixel Art Colors (se mantiene PIXEL_BLUE_DARK_BG como fallback)
    private static final Color PIXEL_BLUE_DARK_BG = Color.decode("#001F5C");
    private static final Color PIXEL_YELLOW_TITLE = Color.decode("#FFCC00");
    private static final Color PIXEL_SLOT_BG_EMPTY = Color.decode("#4A4A4A"); // Gris oscuro para ranuras vacías
    private static final Color PIXEL_SLOT_BG_SAVED = Color.decode("#6A8A3A");  // Verde oscuro para ranuras guardadas
    private static final Color PIXEL_SLOT_TEXT_EMPTY = Color.decode("#B0B0B0");
    private static final Color PIXEL_SLOT_TEXT_SAVED = Color.WHITE;
    private static final Color PIXEL_SLOT_BORDER = Color.decode("#202020");
    private static final Color PIXEL_SLOT_HOVER_BG = Color.decode("#5C7A99"); // Azul grisáceo para hover
    private static final Color PIXEL_RED_DELETE = Color.decode("#CC0000");
    private static final Color PIXEL_WHITE_TEXT = Color.WHITE;


    public LoadGamePanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        // setBackground(PIXEL_BLUE_DARK_BG); // El fondo se pintará en paintComponent
        loadBackgroundImage(); // NUEVO: Cargar imagen de fondo
        loadAllSaveData();
        initComponents();
    }

    private void loadBackgroundImage() {
        try {
            panelBackgroundImage = null; 
            String backgroundPath = "/backgrounds/Fondo-archivos.png";
            URL bgUrl = getClass().getResource(backgroundPath);
            
            if (bgUrl == null) {
                String pathForClassLoader = backgroundPath.startsWith("/") ? backgroundPath.substring(1) : backgroundPath;
                System.out.println("[LoadGamePanel] Intentando cargar fondo con ClassLoader desde: " + pathForClassLoader);
                bgUrl = getClass().getClassLoader().getResource(pathForClassLoader);
            }

            if (bgUrl != null) {
                System.out.println("[LoadGamePanel] URL del fondo encontrada: " + bgUrl.toExternalForm());
                try (InputStream bgInputStream = bgUrl.openStream()) {
                    panelBackgroundImage = ImageIO.read(bgInputStream);
                    if (panelBackgroundImage != null) {
                        System.out.println("[LoadGamePanel] Fondo cargado CORRECTAMENTE desde: " + backgroundPath);
                    } else {
                        System.err.println("[LoadGamePanel] Error: ImageIO.read devolvió null para el fondo (" + backgroundPath + ").");
                    }
                } catch (IOException ioe) {
                    System.err.println("[LoadGamePanel] Error de IO al leer el stream del fondo (" + backgroundPath + "): " + ioe.getMessage());
                    ioe.printStackTrace();
                    panelBackgroundImage = null;
                }
            } else {
                System.err.println("[LoadGamePanel] Error CRÍTICO: No se encontró el recurso de fondo en: " + backgroundPath);
            }
        } catch (Exception e) {
            System.err.println("[LoadGamePanel] Excepción inesperada durante la carga de la imagen de fondo: " + e.getMessage());
            e.printStackTrace();
            panelBackgroundImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (panelBackgroundImage != null) {
            // Escalar la imagen para que cubra todo el panel
            g.drawImage(panelBackgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Si no hay imagen de fondo, pintar con el color por defecto
            g.setColor(PIXEL_BLUE_DARK_BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            // Opcional: Dibujar un mensaje de error si la imagen no cargó
            g.setColor(Color.RED);
            Font errorFont = getFont(); // Usar la fuente actual del panel
             if (parentFrame != null && parentFrame.getPixelArtFont() != null) {
                errorFont = parentFrame.getPixelArtFont().deriveFont(12f);
            } else if (errorFont == null) { // Fallback si getFont() es null
                errorFont = new Font("Monospaced", Font.BOLD, 12);
            }
            g.setFont(errorFont);
            g.drawString("Error: Fondo de LoadGamePanel ('/backgrounds/Fondo-archivos.png') no cargado.", 20, 20);
        }
    }


    private void loadAllSaveData() {
        loadedSaveDataList = new ArrayList<>(NUM_SAVE_SLOTS);
        for (int i = 0; i < NUM_SAVE_SLOTS; i++) {
            String fileName = "save_slot_" + i + ".dat";
            try {
                File saveFile = new File(fileName);
                if (saveFile.exists() && saveFile.length() > 0) {
                    SaveData data = SaveManager.cargar(fileName);
                    loadedSaveDataList.add(data);
                    System.out.println("Datos de guardado cargados para ranura " + i + " desde: " + fileName);
                } else {
                    loadedSaveDataList.add(null);
                }
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                loadedSaveDataList.add(null);
                System.err.println("Error al cargar la partida del archivo: " + fileName + ". " + e.getMessage());
            }
        }
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, HORIZONTAL_PADDING, 20, HORIZONTAL_PADDING);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel titleLabel = new JLabel("CARGAR O CREAR PARTIDA");
        titleLabel.setForeground(PIXEL_YELLOW_TITLE);
        Font pixelFont = parentFrame.getPixelArtFont();
        if (pixelFont != null) {
             titleLabel.setFont(pixelFont.deriveFont(Font.BOLD, 22f));
        } else {
             titleLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        }
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Hacer el label no opaco si el fondo del panel principal es una imagen
        titleLabel.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.1; // Menos peso para el título
        gbc.anchor = GridBagConstraints.PAGE_START;
        add(titleLabel, gbc);

        JPanel slotsContainer = new JPanel(new GridBagLayout());
        slotsContainer.setOpaque(false); // HACER TRANSPARENTE para ver el fondo del LoadGamePanel
        GridBagConstraints slotsGbc = new GridBagConstraints();
        slotsGbc.gridx = 0;
        slotsGbc.weightx = 1.0;
        slotsGbc.fill = GridBagConstraints.HORIZONTAL;
        slotsGbc.insets = new Insets(8, 0, 8, 0); // Espacio entre ranuras

        for (int i = 0; i < NUM_SAVE_SLOTS; i++) {
            JPanel saveSlotPanel = createSaveSlotPanel(i, loadedSaveDataList.get(i));
            // saveSlotPanel ya maneja su propio fondo y borde, no necesita ser transparente aquí
            slotsGbc.gridy = i;
            slotsContainer.add(saveSlotPanel, slotsGbc);
        }

        gbc.gridy = 1;
        gbc.weighty = 0.9; // Más peso para el contenedor de ranuras
        gbc.anchor = GridBagConstraints.CENTER; // Centrar el contenedor de ranuras
        add(slotsContainer, gbc);
    }

    private JPanel createSaveSlotPanel(int slotIndex, SaveData saveData) {
        JPanel panel = new JPanel(new BorderLayout(10, 0)); // Espacio horizontal entre componentes
        panel.setBorder(BorderFactory.createLineBorder(PIXEL_SLOT_BORDER, 2));
        panel.setPreferredSize(new Dimension(450, SLOT_BUTTON_HEIGHT)); // Altura fija, ancho flexible

        JPanel textPanel = new JPanel(new GridLayout(0, 1)); // 0 filas = tantas como sea necesario
        textPanel.setOpaque(false); // Para que se vea el fondo del 'panel'
        textPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        JLabel titleLine;
        JLabel detailLine = new JLabel(" "); // Inicializar para evitar NullPointer
        Font pixelFont = parentFrame.getPixelArtFont();
        Font titleFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 14f) : new Font("Monospaced", Font.BOLD, 14);
        Font detailFont = (pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11);

        List<String> pokemonNames = new ArrayList<>();

        if (saveData != null && saveData.aliado != null) {
            panel.setBackground(PIXEL_SLOT_BG_SAVED);
            titleLine = new JLabel("Partida Guardada " + (slotIndex + 1));
            titleLine.setForeground(PIXEL_SLOT_TEXT_SAVED);
            titleLine.setFont(titleFont);

            String trainerName = saveData.aliado.nombreEntrenador != null ? saveData.aliado.nombreEntrenador : "Entrenador";
            String firstPokemonName = (saveData.aliado.equipo != null && !saveData.aliado.equipo.isEmpty() && saveData.aliado.equipo.get(0).nombreEspecie != null) ?
                                      saveData.aliado.equipo.get(0).nombreEspecie : "???";
            int teamSize = (saveData.aliado.equipo != null) ? saveData.aliado.equipo.size() : 0;
            detailLine.setText(trainerName + " - Equipo: " + teamSize + " (" + firstPokemonName + (teamSize > 1 ? ", ..." : "") + ")");
            detailLine.setForeground(PIXEL_SLOT_TEXT_SAVED);
            detailLine.setFont(detailFont);

            if (saveData.aliado.equipo != null) {
                for (SaveManager.PokemonSave pkmnSave : saveData.aliado.equipo) {
                    if (pkmnSave != null) pokemonNames.add(pkmnSave.nombreEspecie);
                }
            }
        } else {
            panel.setBackground(PIXEL_SLOT_BG_EMPTY);
            titleLine = new JLabel("Ranura Vacía " + (slotIndex + 1) + " - Nueva Partida");
            titleLine.setForeground(PIXEL_SLOT_TEXT_EMPTY);
            titleLine.setFont(titleFont);
            detailLine.setText("Selecciona para iniciar una nueva aventura.");
            detailLine.setForeground(PIXEL_SLOT_TEXT_EMPTY);
            detailLine.setFont(detailFont);
        }

        textPanel.add(titleLine);
        textPanel.add(detailLine);

        JPanel iconsPanel = createSaveSlotPokemonIconRow(pokemonNames);
        iconsPanel.setOpaque(false); // Para que se vea el fondo del 'panel'

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        eastPanel.setOpaque(false); 

        JButton deleteButton = new JButton("X");
        deleteButton.setFont((pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 12f) : new Font("Monospaced", Font.BOLD, 12));
        deleteButton.setForeground(PIXEL_WHITE_TEXT);
        deleteButton.setBackground(PIXEL_RED_DELETE);
        deleteButton.setToolTipText("Borrar esta ranura de guardado");
        deleteButton.setMargin(new Insets(2, 5, 2, 5)); 
        deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
        deleteButton.setVisible(saveData != null); 
        deleteButton.addActionListener(e -> handleDeleteSlot(slotIndex));

        eastPanel.add(deleteButton); 

        JLabel arrowLabel = new JLabel("▶ ");
        arrowLabel.setForeground(saveData != null ? PIXEL_YELLOW_TITLE : PIXEL_SLOT_TEXT_EMPTY);
        if (pixelFont != null) {
             arrowLabel.setFont(pixelFont.deriveFont(Font.BOLD, 18f));
        } else {
             arrowLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        }
        eastPanel.add(arrowLabel); 

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(iconsPanel, BorderLayout.CENTER);
        panel.add(eastPanel, BorderLayout.EAST); 

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == deleteButton || (e.getComponent() != null && e.getComponent().getParent() == deleteButton)) {
                    return;
                }
                parentFrame.handleSlotSelection(loadedSaveDataList.get(slotIndex), slotIndex);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(PIXEL_SLOT_HOVER_BG);
                panel.setBorder(BorderFactory.createLineBorder(PIXEL_YELLOW_TITLE, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(saveData != null ? PIXEL_SLOT_BG_SAVED : PIXEL_SLOT_BG_EMPTY);
                panel.setBorder(BorderFactory.createLineBorder(PIXEL_SLOT_BORDER, 2));
            }
        });
        return panel;
    }
    
    private void handleDeleteSlot(int slotIndex) {
        int confirmation = JOptionPane.showConfirmDialog(
                parentFrame,
                "¿Estás seguro de que quieres borrar los datos de la Ranura " + (slotIndex + 1) + "?\nEsta acción no se puede deshacer.",
                "Confirmar Borrado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            String fileName = "save_slot_" + slotIndex + ".dat";
            File saveFile = new File(fileName);
            boolean deleted = false;
            if (saveFile.exists()) {
                try {
                    if (saveFile.delete()) {
                        deleted = true;
                        JOptionPane.showMessageDialog(parentFrame, "Ranura " + (slotIndex + 1) + " borrada exitosamente.", "Borrado Exitoso", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(parentFrame, "No se pudo borrar la Ranura " + (slotIndex + 1) + ".\nEl archivo podría estar en uso o protegido.", "Error al Borrar", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SecurityException se) {
                    JOptionPane.showMessageDialog(parentFrame, "Error de seguridad al intentar borrar la Ranura " + (slotIndex + 1) + ".\n" + se.getMessage(), "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
                    se.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(parentFrame, "La Ranura " + (slotIndex + 1) + " ya estaba vacía o el archivo no existe.", "Información", JOptionPane.INFORMATION_MESSAGE);
            }

            if (deleted) {
                loadedSaveDataList.set(slotIndex, null); 
            }
            refreshSaveSlotsDisplay(); 
        }
    }


    private JPanel createSaveSlotPokemonIconRow(List<String> pokemonNames) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        int iconsToShow = Math.min(pokemonNames != null ? pokemonNames.size() : 0, 6);

        for (int i = 0; i < iconsToShow; i++) {
            String pokemonName = pokemonNames.get(i);
            JLabel iconLabel = createIconLabel(pokemonName, POKEMON_ICON_SIZE);
            row.add(iconLabel);
        }
        for (int i = iconsToShow; i < 6; i++) {
            JLabel placeholder = new JLabel();
            placeholder.setPreferredSize(new Dimension(POKEMON_ICON_SIZE, POKEMON_ICON_SIZE));
            placeholder.setOpaque(true);
            placeholder.setBackground(new Color(0,0,0,0)); // Completamente transparente
            row.add(placeholder);
        }
        return row;
    }

    private JLabel createIconLabel(String pokemonName, int size) {
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(size, size));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (pokemonName == null || pokemonName.isEmpty()) {
            return iconLabel;
        }

        String formattedName = pokemonName.toLowerCase().replace(" ", "-");
        String resourcePath = "/PokeMiniaturas/" + formattedName + ".png";

        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                Image scaledImg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledImg));
            } else {
                iconLabel.setText("?");
                Font pixelFont = parentFrame.getPixelArtFont();
                iconLabel.setFont((pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, (float)size * 0.6f) : new Font("Monospaced", Font.PLAIN, (int)(size*0.6)));
                iconLabel.setForeground(PIXEL_SLOT_TEXT_EMPTY);
            }
        } catch (IOException e) {
            System.err.println("Error cargando miniatura: " + resourcePath + " - " + e.getMessage());
            iconLabel.setText("X");
        }
        return iconLabel;
    }

    public void refreshSaveSlotsDisplay() {
        loadAllSaveData();
        this.removeAll(); // Limpiar el panel antes de re-inicializar componentes
        initComponents(); // Re-construir los componentes con los datos actualizados
        this.revalidate();
        this.repaint();
    }
}
