package Face;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import PokeBody.domain.Trainer;

public class CombatModeSelectionPanel extends JPanel {
    private SwingGUI parentFrame;
    private Trainer player1Trainer; 
    private Trainer player2Trainer; 
    private String selectedScenarioName; 
    private ImageIcon backgroundGif;

    // Colores y estilos (puedes reutilizar los de ModeSelectionPanel si son los mismos)
    private static final Color AMONG_US_BUTTON_BG_GREEN = new Color(36, 145, 57); // Verde para PVE
    private static final Color AMONG_US_BUTTON_BG_RED = new Color(197, 17, 17);   // Rojo para PVP
    private static final Color AMONG_US_BUTTON_BG_GRAY = new Color(108, 122, 137); // Gris para AVA
    private static final Color AMONG_US_BUTTON_FG_WHITE = Color.WHITE;
    private static final Color AMONG_US_BUTTON_BORDER_DARK = new Color(30, 30, 60);
    private static final Color AMONG_US_BACK_BUTTON_BG = new Color(75, 75, 95); // Gris oscuro para Volver
    private static final Color AMONG_US_TITLE_FG = Color.WHITE;


    public CombatModeSelectionPanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        loadBackgroundImage();
        initComponents();
    }
    
    private void loadBackgroundImage() {
        try {
            String gifPath = "/backgrounds/Fondo-combate.gif"; // Ruta al GIF
            URL gifUrl = getClass().getResource(gifPath);
            if (gifUrl != null) {
                backgroundGif = new ImageIcon(gifUrl);
                backgroundGif.getImage().getWidth(null); 
                System.out.println("[CombatModeSelectionPanel] GIF de fondo cargado desde: " + gifPath);
            } else {
                System.err.println("[CombatModeSelectionPanel] Error CRÍTICO: No se encontró el recurso GIF de fondo en: " + gifPath);
                backgroundGif = null;
            }
        } catch (Exception e) {
            System.err.println("[CombatModeSelectionPanel] Excepción al cargar el GIF de fondo: " + e.getMessage());
            e.printStackTrace();
            backgroundGif = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundGif != null && backgroundGif.getImage() != null) {
            g.drawImage(backgroundGif.getImage(), 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK); 
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            Font errorFont = getFont();
             if (parentFrame != null && parentFrame.getPixelArtFont() != null) {
                errorFont = parentFrame.getPixelArtFont().deriveFont(12f);
            } else if (errorFont == null) {
                errorFont = new Font("Monospaced", Font.BOLD, 12);
            }
            g.setFont(errorFont);
            g.drawString("Error: No se pudo cargar Fondo-combate.gif", 20, getHeight() - 20);
        }
    }

    public void setSelectedTrainersAndScenario(Trainer p1, Trainer p2, String scenarioName) {
        this.player1Trainer = p1;
        this.player2Trainer = p2;
        this.selectedScenarioName = scenarioName;
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbcMain = new GridBagConstraints();
        
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false); 

        JLabel titleLabel = new JLabel("ELIGE EL TIPO DE COMBATE");
        titleLabel.setForeground(AMONG_US_TITLE_FG);
        Font titleFont = (parentFrame.getPixelArtFont() != null) ? 
                         parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 26f) : 
                         new Font("SansSerif", Font.BOLD, 26);
        titleLabel.setFont(titleFont);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0)); 

        buttonContainer.add(titleLabel);

        Dimension buttonSize = new Dimension(300, 60); 
        Font buttonFont = (parentFrame.getPixelArtFont() != null) ? 
                          parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 17f) : 
                          new Font("SansSerif", Font.BOLD, 17);
        Border buttonBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AMONG_US_BUTTON_BORDER_DARK, 3),
            BorderFactory.createEmptyBorder(8, 18, 8, 18) 
        );

        JButton pveButton = createStyledButton("JUGADOR vs IA", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_GREEN, AMONG_US_BUTTON_FG_WHITE);
        pveButton.addActionListener(e -> {
            if (player1Trainer != null && player2Trainer != null && selectedScenarioName != null) {
                parentFrame.startCombat(player1Trainer, player2Trainer, selectedScenarioName, SwingGUI.CombatantType.PLAYER, SwingGUI.CombatantType.AI);
            } else {
                 handleMissingDataError("PVE");
            }
        });
        buttonContainer.add(pveButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton pvpButton = createStyledButton("JUGADOR vs JUGADOR", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_RED, AMONG_US_BUTTON_FG_WHITE);
        pvpButton.addActionListener(e -> {
             if (player1Trainer != null && player2Trainer != null && selectedScenarioName != null) {
                if (player2Trainer.getteam() == null || player2Trainer.getteam().isEmpty()) {
                    JOptionPane.showMessageDialog(parentFrame, 
                        "El Jugador 2 no tiene un equipo seleccionado.\nPor favor, selecciona un equipo para el Jugador 2.", 
                        "Equipo Jugador 2 Vacío", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                parentFrame.startCombat(player1Trainer, player2Trainer, selectedScenarioName, SwingGUI.CombatantType.PLAYER, SwingGUI.CombatantType.PLAYER);
            } else {
                 handleMissingDataError("PVP");
            }
        });
        buttonContainer.add(pvpButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton avaButton = createStyledButton("IA vs IA", buttonSize, buttonFont, buttonBorder, AMONG_US_BUTTON_BG_GRAY, AMONG_US_BUTTON_FG_WHITE);
        avaButton.addActionListener(e -> {
            if (player1Trainer != null && player2Trainer != null && selectedScenarioName != null) {
                parentFrame.startCombat(player1Trainer, player2Trainer, selectedScenarioName, SwingGUI.CombatantType.AI, SwingGUI.CombatantType.AI);
            } else {
                 handleMissingDataError("AVA");
            }
        });
        buttonContainer.add(avaButton);
        buttonContainer.add(Box.createRigidArea(new Dimension(0, 25)));

        JButton backButton = createStyledButton("VOLVER", new Dimension(200, 50), buttonFont, buttonBorder, AMONG_US_BACK_BUTTON_BG, AMONG_US_BUTTON_FG_WHITE);
        backButton.addActionListener(e -> parentFrame.getCardLayout().show(parentFrame.getMainPanel(), "SCENARIO_SELECT"));
        buttonContainer.add(backButton);
        
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1.0; 
        gbcMain.weighty = 1.0; 
        gbcMain.anchor = GridBagConstraints.CENTER; 
        add(buttonContainer, gbcMain);
    }

    private JButton createStyledButton(String text, Dimension size, Font font, Border border, Color bgColor, Color fgColor) {
        JButton button = new JButton(text.toUpperCase());
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setFont(font);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setBorder(border);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    private void handleMissingDataError(String mode) {
         System.err.println("Error: Trainers or Scenario not set before selecting " + mode + " mode.");
         JOptionPane.showMessageDialog(parentFrame,
             "Error interno: Faltan datos de entrenador o escenario.",
             "Error de Configuración", JOptionPane.ERROR_MESSAGE);
         parentFrame.getCardLayout().show(parentFrame.getMainPanel(), "TEAM_SELECT");
    }
}
