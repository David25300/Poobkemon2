package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class PokemonSwitchFrame extends JFrame {

    private SwingGUI parentUI;
    private Trainer currentTrainer;
    private Pokemon currentPokemonInBattle; 
    private boolean isMandatorySwitch;

    private JPanel pokemonListPanel;
    private JButton switchButton;
    private JButton cancelButton;
    private JLabel titleLabel;

    private List<PokemonTeamEntryPanel> teamEntryPanels;
    private int selectedPokemonIndex = -1;

    private static final Color PANEL_BACKGROUND_COLOR = new Color(240, 240, 240); 
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    private static final Color SELECTED_BORDER_COLOR = Color.decode("#FFDE00"); 
    private static final Color FAINTED_COLOR = new Color(200, 200, 200); 
    private static final Color IN_BATTLE_COLOR = new Color(220, 220, 255); 

    private static class ScalableImageLabel extends JLabel {
        private BufferedImage masterImage;
        private boolean imageLoaded = false;
        private Font fallbackFont;
        private String fallbackText = "?";
        private static final int INSET_PERCENTAGE = 5;

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
                setForeground(this.fallbackText.equals("X") ? Color.RED : Color.DARK_GRAY);
            } else {
                setText(null);
            }
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

            if (imageLoaded && masterImage != null) {
                int insetX = labelWidth * INSET_PERCENTAGE / 100;
                int insetY = labelHeight * INSET_PERCENTAGE / 100;
                int pokemonAreaX = insetX;
                int pokemonAreaY = insetY;
                int pokemonAreaWidth = labelWidth - (2 * insetX);
                int pokemonAreaHeight = labelHeight - (2 * insetY);

                if (pokemonAreaWidth <= 0 || pokemonAreaHeight <= 0) {
                    g2d.dispose(); return;
                }
                int imgWidth = masterImage.getWidth();
                int imgHeight = masterImage.getHeight();
                if (imgWidth <= 0 || imgHeight <= 0) {
                    g2d.dispose(); return;
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

    private class PokemonTeamEntryPanel extends JPanel {
        private Pokemon pokemon;
        private int indexInTeam; 
        private ScalableImageLabel iconLabel;
        private JLabel nameLabel;
        private JLabel hpLabel;
        private JLabel statusLabel;
        private final Border defaultBorder = BorderFactory.createLineBorder(BORDER_COLOR, 1);
        private final Border selectedBorder = BorderFactory.createLineBorder(SELECTED_BORDER_COLOR, 3); 

        public PokemonTeamEntryPanel(Pokemon pokemon, int indexInTeam, Font baseFont) {
            this.pokemon = pokemon;
            this.indexInTeam = indexInTeam;
            setLayout(new BorderLayout(5, 0));
            setBorder(defaultBorder);
            setBackground(PANEL_BACKGROUND_COLOR);
            setPreferredSize(new Dimension(280, 65)); 

            Font smallFont = baseFont.deriveFont(baseFont.getSize() - 2f);
            Font iconFallbackFont = baseFont.deriveFont(Font.BOLD, 20f);

            iconLabel = new ScalableImageLabel(iconFallbackFont);
            iconLabel.setPreferredSize(new Dimension(55, 55)); 
            loadMiniature();
            add(iconLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setOpaque(false);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBorder(new EmptyBorder(5,8,5,5)); 

            nameLabel = new JLabel(pokemon.getNombre());
            nameLabel.setFont(baseFont.deriveFont(Font.BOLD));
            infoPanel.add(nameLabel);

            hpLabel = new JLabel("PS: " + pokemon.getHpActual() + "/" + pokemon.getHpMax());
            hpLabel.setFont(smallFont);
            infoPanel.add(hpLabel);

            statusLabel = new JLabel("Estado: " + (pokemon.getEstado() != null ? pokemon.getEstado() : "OK"));
            statusLabel.setFont(smallFont);
            infoPanel.add(statusLabel);
            
            add(infoPanel, BorderLayout.CENTER);

            // Un Pokémon no es seleccionable si está debilitado O
            // si es un cambio voluntario Y es el que ya está en batalla.
            boolean isSelectable = !pokemon.estaDebilitado() && 
                                   !( !isMandatorySwitch && pokemon == PokemonSwitchFrame.this.currentPokemonInBattle );

            if (!isSelectable) {
                if (pokemon.estaDebilitado()) {
                    setBackground(FAINTED_COLOR);
                    nameLabel.setForeground(Color.DARK_GRAY);
                    hpLabel.setForeground(Color.DARK_GRAY);
                    statusLabel.setText("DEBILITADO");
                    statusLabel.setForeground(Color.RED.darker());
                } else { // Es el que está en batalla (y no es cambio obligatorio)
                    setBackground(IN_BATTLE_COLOR);
                    statusLabel.setText("EN COMBATE");
                    statusLabel.setForeground(Color.BLUE.darker());
                }
            } else { 
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectThis();
                    }
                });
            }
        }

        private void loadMiniature() {
            String pokemonNameLower = pokemon.getNombre().toLowerCase().replace(" ", "-");
            String resourcePath = "/PokeMiniaturas/" + pokemonNameLower + ".png";
            BufferedImage img = null;
            String errorText = pokemon.getNombre().length() > 2 ? pokemon.getNombre().substring(0,2) : "?";
            try {
                URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    img = ImageIO.read(url);
                }
            } catch (IOException e) {
                System.err.println("Error cargando miniatura para " + pokemon.getNombre() + ": " + e.getMessage());
                errorText = "X";
            }
            iconLabel.setMasterImage(img, errorText);
        }
        
        public void setSelected(boolean selected) {
            setBorder(selected ? selectedBorder : defaultBorder);
             boolean isCurrentlyInBattleAndNotMandatory = !isMandatorySwitch && pokemon == PokemonSwitchFrame.this.currentPokemonInBattle;
            setBackground(selected ? SELECTED_BORDER_COLOR.brighter().brighter() : 
                            (pokemon.estaDebilitado() ? FAINTED_COLOR : 
                            (isCurrentlyInBattleAndNotMandatory ? IN_BATTLE_COLOR : PANEL_BACKGROUND_COLOR)));
        }

        private void selectThis() {
            selectedPokemonIndex = this.indexInTeam;
            for (PokemonTeamEntryPanel entry : teamEntryPanels) {
                entry.setSelected(entry == this);
            }
            switchButton.setEnabled(true); 
        }
    }


    public PokemonSwitchFrame(SwingGUI parentUI) {
        this.parentUI = parentUI;
        this.teamEntryPanels = new ArrayList<>();

        setTitle("Seleccionar Pokémon");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        setMinimumSize(new Dimension(320, 450));
        setPreferredSize(new Dimension(350, 500));
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().setBackground(new Color(50, 50, 70)); 

        Font titleFont = parentUI.getPixelArtFont() != null ? parentUI.getPixelArtFont().deriveFont(Font.BOLD, 16f) : new Font("Monospaced", Font.BOLD, 16);
        Font buttonFont = parentUI.getPixelArtFont() != null ? parentUI.getPixelArtFont().deriveFont(Font.PLAIN, 12f) : new Font("Monospaced", Font.PLAIN, 12);

        titleLabel = new JLabel("Selecciona un Pokémon para cambiar", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(5,0,10,0));
        add(titleLabel, BorderLayout.NORTH);

        pokemonListPanel = new JPanel();
        pokemonListPanel.setLayout(new BoxLayout(pokemonListPanel, BoxLayout.Y_AXIS));
        pokemonListPanel.setBackground(PANEL_BACKGROUND_COLOR);
        
        JScrollPane scrollPane = new JScrollPane(pokemonListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 2));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setOpaque(false);

        switchButton = new JButton("Cambiar");
        switchButton.setFont(buttonFont);
        switchButton.setEnabled(false); 
        switchButton.addActionListener(e -> performSwitch());
        buttonPanel.add(switchButton);

        cancelButton = new JButton("Cancelar");
        cancelButton.setFont(buttonFont);
        cancelButton.addActionListener(e -> {
            if (isMandatorySwitch) {
                JOptionPane.showMessageDialog(this, "Debes seleccionar un Pokémon para continuar.", "Cambio Obligatorio", JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println("[PokemonSwitchFrame] Cambio voluntario cancelado por el usuario.");
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parentUI); 
    }

    public void displaySwitchOptions(Trainer trainer, Pokemon pokemonInContext, boolean isMandatory) {
        this.currentTrainer = trainer;
        this.currentPokemonInBattle = pokemonInContext; // Este es el Pokémon que está/estaba en batalla
        this.isMandatorySwitch = isMandatory;
        this.selectedPokemonIndex = -1; 

        teamEntryPanels.clear();
        pokemonListPanel.removeAll();

        if (isMandatorySwitch) {
            titleLabel.setText( (currentPokemonInBattle != null ? currentPokemonInBattle.getNombre() : "Tu Pokémon") + " se ha debilitado. ¡Elige otro!");
            cancelButton.setToolTipText("Debes seleccionar un Pokémon.");
        } else {
            titleLabel.setText("¿Cambiar Pokémon?");
            cancelButton.setToolTipText("Cierra esta ventana sin cambiar.");
            cancelButton.setEnabled(true);
        }
        switchButton.setEnabled(false);


        List<Pokemon> team = trainer.getteam();
        Font pokemonEntryFont = parentUI.getPixelArtFont() != null ? parentUI.getPixelArtFont().deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11);

        boolean hasSwitchablePokemon = false;
        for (int i = 0; i < team.size(); i++) {
            Pokemon p = team.get(i);
            if (p != null) { 
                PokemonTeamEntryPanel entryPanel = new PokemonTeamEntryPanel(p, i, pokemonEntryFont);
                teamEntryPanels.add(entryPanel);
                pokemonListPanel.add(entryPanel);
                if (i < team.size() -1 ) { 
                    pokemonListPanel.add(Box.createRigidArea(new Dimension(0,3)));
                }
                // Un Pokémon es seleccionable si NO está debilitado Y (es un cambio obligatorio O no es el que ya está en combate)
                if (!p.estaDebilitado() && (isMandatorySwitch || p != currentPokemonInBattle)) {
                    hasSwitchablePokemon = true;
                }
            }
        }
        
        if (isMandatorySwitch && !hasSwitchablePokemon) {
             System.out.println("[PokemonSwitchFrame] No hay Pokémon válidos para cambiar (obligatorio). Esto debería ser manejado por CombatManager como fin de juego.");
             // Si no hay opciones válidas en un cambio obligatorio, el juego debería terminar.
             // Aquí podríamos cerrar la ventana y CombatManager debería detectar que no se hizo un cambio.
             JOptionPane.showMessageDialog(this, "No tienes Pokémon disponibles para cambiar.", "Equipo Derrotado", JOptionPane.ERROR_MESSAGE);
             dispose(); // Cerrar, CombatManager se encargará del resto.
             return;
        }


        pokemonListPanel.revalidate();
        pokemonListPanel.repaint();
        
        this.setVisible(true);
        System.out.println("[PokemonSwitchFrame] displaySwitchOptions - Ventana de cambio mostrada. Obligatorio: " + isMandatorySwitch + ". Pokémon en contexto: " + (currentPokemonInBattle != null ? currentPokemonInBattle.getNombre() : "N/A"));
    }

    private void performSwitch() {
        if (selectedPokemonIndex == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un Pokémon de la lista.", "Ningún Pokémon Seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Pokemon selectedPokemonInstance = currentTrainer.getteam().get(selectedPokemonIndex);
        if (selectedPokemonInstance.estaDebilitado()) {
            JOptionPane.showMessageDialog(this, selectedPokemonInstance.getNombre() + " está debilitado y no puede luchar.", "Pokémon Debilitado", JOptionPane.ERROR_MESSAGE);
            return;
        }
      
        if (isMandatorySwitch && selectedPokemonInstance == currentPokemonInBattle) { 
             JOptionPane.showMessageDialog(this, "No puedes seleccionar al Pokémon que acaba de debilitarse.", "Selección Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
         if (!isMandatorySwitch && selectedPokemonInstance == currentPokemonInBattle) { 
             JOptionPane.showMessageDialog(this, selectedPokemonInstance.getNombre() + " ya está en combate.", "Selección Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }


        PlayerActionChoice choice = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
        choice.switchToPokemonIndex = selectedPokemonIndex;

        MovementSelector.GUIQueueMovementSelector selectorToUse = null;
        if (currentTrainer == parentUI.getCurrentPlayer1TrainerState()) { 
            selectorToUse = parentUI.getPlayer1GuiMoveSelector();
            System.out.println("[PokemonSwitchFrame performSwitch] Acción de cambio para Jugador 1, Pokémon índice: " + selectedPokemonIndex + " (" + selectedPokemonInstance.getNombre() + ")");
        } else if (currentTrainer == parentUI.getCurrentPlayer2TrainerState() && parentUI.getCombatViewPanel().isPlayer2Human()) { 
            selectorToUse = parentUI.getPlayer2GuiMoveSelector();
             System.out.println("[PokemonSwitchFrame performSwitch] Acción de cambio para Jugador 2 (Humano), Pokémon índice: " + selectedPokemonIndex + " (" + selectedPokemonInstance.getNombre() + ")");
        }


        if (selectorToUse != null) {
            selectorToUse.submitPlayerAction(choice);
            System.out.println("[PokemonSwitchFrame performSwitch] Acción de cambio enviada a la cola: " + choice);
            dispose(); 
        } else {
            System.err.println("[PokemonSwitchFrame performSwitch] Error: No se pudo determinar el selector de GUI para el entrenador: " + currentTrainer.getName());
            JOptionPane.showMessageDialog(this, "Error interno al procesar el cambio.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
