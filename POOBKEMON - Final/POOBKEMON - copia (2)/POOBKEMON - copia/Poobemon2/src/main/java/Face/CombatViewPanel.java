// Poobemon2/src/main/java/Face/CombatViewPanel.java
package Face;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
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
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.events.BattleEndEvent;
import PokeBody.Services.events.BattleStartEvent;
import PokeBody.Services.events.CombatEventListener;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.MoveUsedEvent;
import PokeBody.Services.events.PokemonChangeEvent;
import PokeBody.Services.events.PokemonFaintedEvent;
import PokeBody.Services.events.PokemonHpChangedEvent;
import PokeBody.Services.events.StatusAppliedEvent;
import PokeBody.Services.events.TurnStartEvent;
import PokeBody.domain.Item;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.Type;
import PokeBody.domain.items.BombaAliada;
import PokeBody.domain.items.BombaRival;
import PokeBody.domain.items.HyperPotion;
import PokeBody.domain.items.Potion;
import PokeBody.domain.items.Revive;
import PokeBody.domain.items.SuperPotion;


public class CombatViewPanel extends JPanel implements CombatEventListener {

    private transient Image backgroundImage;
    private String currentScenarioName;
    private SwingGUI parentFrame; 

    private Pokemon playerPokemon;
    private Pokemon opponentPokemon;
    private Trainer player1Trainer;
    private Trainer player2Trainer;

    private int playerDisplayedHp;
    private int opponentDisplayedHp;
    private int playerTargetHp;
    private int opponentTargetHp;
    private Timer hpAnimationTimer;
    private static final int HP_ANIMATION_STEP_DURATION = 25;
    private static final int HP_POINTS_PER_STEP = 2;

    private transient ImageIcon playerPokemonSprite;
    private transient ImageIcon opponentPokemonSprite;
    private String playerPokemonNameForError;
    private String opponentPokemonNameForError;

    private JPanel bottomMenuPanel;
    private CardLayout bottomMenuCardLayout;
    private JPanel actionsDisplayPanel;
    private ItemsAtkPanel movesDisplayPanel;
    private ItemsAtkPanel itemsDisplayPanel; // Ahora con paginación


    private JButton fightButton, pokemonButton, bagButton, runButton;
    private JLabel actionPromptLabel;

    private ImageIcon fightButtonIcon, pokemonButtonIcon, bagButtonIcon, runButtonIcon;
    private BufferedImage bottomMenuBackgroundImage;
    private BufferedImage itemsAtkPanelBackground;

    private JTextArea combatLogArea;
    private JScrollPane logScrollPane;
    private JPanel combatLogDisplayPanel;

    private MovementSelector.GUIQueueMovementSelector player1GuiMoveSelector;
    private MovementSelector.GUIQueueMovementSelector player2GuiMoveSelector;
    private boolean isPlayer2Human = false;
    private boolean isPlayer1TurnForInput = true; 

    private JLabel turnTimerLabel;
    private javax.swing.Timer turnCountdownTimer;
    private int remainingTurnTime;
    private static final int TURN_TIME_SECONDS = 30;

    private JButton pauseButton;
    private JButton restartButton;
    private JPanel topControlPanel; 


    private static final String POKEMON_ANIMATIONS_PATH = "/PokeAnimations/";
    private static final String SCENARIOS_RESOURCE_PATH = "/PokeEscenarios/";
    private static final String INTERFACE_RESOURCE_PATH = "/Interfaz/";
    private static final String BACKGROUNDS_RESOURCE_PATH = "/backgrounds/"; 

    private static final double PLAYER_SPRITE_X_RATIO = 0.18;
    private static final double PLAYER_SPRITE_Y_RATIO = 0.52;
    private static final double OPPONENT_SPRITE_X_RATIO = 0.62;
    private static final double OPPONENT_SPRITE_Y_RATIO = 0.18;
    private static final double SPRITE_WIDTH_RATIO = 0.22;
    private static final double SPRITE_HEIGHT_RATIO = 0.28;
    private static final double PLAYER_STAT_BOX_X_RATIO = 0.55;
    private static final double PLAYER_STAT_BOX_Y_RATIO = 0.65;
    private static final double OPPONENT_STAT_BOX_X_RATIO = 0.05;
    private static final double OPPONENT_STAT_BOX_Y_RATIO = 0.05;
    private static final double STAT_BOX_WIDTH_RATIO = 0.40;
    private static final double STAT_BOX_HEIGHT_RATIO = 0.18;
    private static final Color HP_GREEN = new Color(30, 200, 30);
    private static final Color HP_YELLOW = new Color(230, 210, 20);
    private static final Color HP_RED = new Color(220, 30, 30);
    private static final Color HP_BAR_BACKGROUND = Color.DARK_GRAY;
    private static final Color STAT_BOX_BACKGROUND = new Color(248, 248, 248, 220);
    private static final Color STAT_BOX_BORDER_COLOR = new Color(50, 50, 50);
    private static final double BOTTOM_MENU_HEIGHT_RATIO = 0.28;
    private static final Color SLOT_TEXT_COLOR = Color.WHITE;
    private static final String ACTIONS_PANEL_CARD = "ACTIONS";
    private static final String MOVES_PANEL_CARD = "MOVES";
    private static final String ITEMS_PANEL_CARD = "ITEMS";
    private static final String LOG_DISPLAY_PANEL_CARD = "LOG_DISPLAY";
    private static final Color TIMER_TEXT_COLOR = Color.decode("#FFDE00"); 
    private static final Color ACTION_PROMPT_TEXT_COLOR = Color.WHITE;
    private static final Color INFO_AREA_TEXT_COLOR = Color.WHITE;
    private static final Color PAUSE_RESTART_BUTTON_BG = new Color(75, 75, 95, 200); 
    private static final Color PAUSE_RESTART_BUTTON_FG = Color.WHITE;


    private class BottomMenuBackgroundPanel extends JPanel {
        public BottomMenuBackgroundPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bottomMenuBackgroundImage != null) {
                g.drawImage(bottomMenuBackgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(new Color(30,30,30));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                Font errorFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(10f) : new Font("Monospaced", Font.BOLD, 10);
                g.setFont(errorFont);
                g.drawString("Fondo de menú no cargado (batalla_log.png)", 10, 20);
            }
        }
    }

    // Clase interna ItemsAtkPanel AHORA CON PAGINACIÓN PARA ÍTEMS
    private class ItemsAtkPanel extends JPanel {
        private JTextArea infoArea;
        private JPanel slotsPanel; // Para movimientos o ítems
        private JPanel paginationPanel; // Panel para botones de paginación de ítems
        private JButton prevPageButton, nextPageButton;
        private JLabel pageInfoLabel; // Para mostrar "Pág X/Y"

        private List<Item> allPlayerItemsUnique; // Lista completa de ítems únicos del jugador para paginación
        private int currentItemPage = 0;
        private static final int ITEMS_PER_PAGE = 4; // 2x2 grid
        private String panelType; // "MOVES" o "ITEMS"

        public ItemsAtkPanel(String type) {
            super(new GridBagLayout());
            this.panelType = type;
            setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();

            slotsPanel = new JPanel(new GridLayout(2, 2, 7, 7));
            slotsPanel.setOpaque(false);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 1; // Ocupa una fila
            gbc.weightx = 0.65;
            gbc.weighty = 0.8; // Dar más peso a los slots
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(10, 15, 5, 8); // Ajustar insets
            add(slotsPanel, gbc);

            // Panel de paginación (solo para ítems)
            paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            paginationPanel.setOpaque(false);
            prevPageButton = new JButton("<<");
            pageInfoLabel = new JLabel("Pág -/-");
            nextPageButton = new JButton(">>");
            
            Font pageButtonFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 10f) : new Font("Monospaced", Font.PLAIN, 10);
            prevPageButton.setFont(pageButtonFont);
            nextPageButton.setFont(pageButtonFont);
            pageInfoLabel.setFont(pageButtonFont);
            pageInfoLabel.setForeground(INFO_AREA_TEXT_COLOR);

            prevPageButton.addActionListener(e -> navigateItemsPage(-1));
            nextPageButton.addActionListener(e -> navigateItemsPage(1));

            paginationPanel.add(prevPageButton);
            paginationPanel.add(pageInfoLabel);
            paginationPanel.add(nextPageButton);
            
            gbc.gridx = 0;
            gbc.gridy = 1; // Debajo de los slots
            gbc.gridheight = 1;
            gbc.weighty = 0.1; // Menos peso para paginación
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(0, 15, 5, 8);
            add(paginationPanel, gbc);
            paginationPanel.setVisible(this.panelType.equals("ITEMS")); // Solo visible para ítems

            // Panel derecho (Info y Volver)
            JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
            rightPanel.setOpaque(false);

            infoArea = new JTextArea("Selecciona un " + (type.equals("MOVES") ? "ataque" : "ítem") + "...");
            infoArea.setEditable(false);
            infoArea.setLineWrap(true);
            infoArea.setWrapStyleWord(true);
            Font infoFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(11f) : new Font("Monospaced", Font.PLAIN, 11);
            infoArea.setFont(infoFont);
            infoArea.setOpaque(false);
            infoArea.setForeground(INFO_AREA_TEXT_COLOR);
            infoArea.setBorder(new EmptyBorder(8, 12, 8, 12));
            infoArea.setColumns(18); // Ajustar columnas si es necesario

            JScrollPane infoScrollPane = new JScrollPane(infoArea);
            infoScrollPane.setOpaque(false);
            infoScrollPane.getViewport().setOpaque(false);
            infoScrollPane.setBorder(null);
            infoScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            JPanel infoWrapperPanel = new JPanel(new BorderLayout());
            infoWrapperPanel.setOpaque(false);
            infoWrapperPanel.add(infoScrollPane, BorderLayout.CENTER);
            rightPanel.add(infoWrapperPanel, BorderLayout.CENTER);

            JButton backButton = new JButton("VOLVER");
            if (parentFrame.getPixelArtFont() != null) {
                backButton.setFont(parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 12f));
            }
            backButton.addActionListener(e -> showActionButtonsPanel());
            backButton.setBackground(new Color(100, 100, 100, 200));
            backButton.setForeground(Color.WHITE);
            backButton.setBorder(BorderFactory.createRaisedBevelBorder());
            backButton.setFocusPainted(false);

            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);
            rightPanel.add(backButtonPanel, BorderLayout.SOUTH);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridheight = 2; // Ocupa ambas filas del lado izquierdo
            gbc.weightx = 0.35;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(10, 8, 10, 15); // Ajustar insets
            add(rightPanel, gbc);
        }

        public JPanel getSlotsPanel() { return slotsPanel; }
        public JTextArea getInfoArea() { return infoArea; }
        
        public void setAllPlayerItemsUnique(List<Item> items) {
            this.allPlayerItemsUnique = items;
            this.currentItemPage = 0;
            updateItemPaginationButtons();
        }

        private void navigateItemsPage(int direction) {
            if (allPlayerItemsUnique == null || allPlayerItemsUnique.isEmpty()) return;
            int totalPages = (int) Math.ceil((double) allPlayerItemsUnique.size() / ITEMS_PER_PAGE);
            int newPage = currentItemPage + direction;

            if (newPage >= 0 && newPage < totalPages) {
                currentItemPage = newPage;
                // Volver a llamar a showItemsSelectionPanel para refrescar con la nueva página
                // Esto es un poco indirecto, idealmente showItemsSelectionPanel tomaría la página como arg.
                // Por ahora, showItemsSelectionPanel usará this.currentItemPage.
                CombatViewPanel.this.showItemsSelectionPanel(); 
            }
        }
        
        public void updateItemPaginationButtons() {
            if (!this.panelType.equals("ITEMS") || allPlayerItemsUnique == null || allPlayerItemsUnique.isEmpty()) {
                paginationPanel.setVisible(false);
                return;
            }
            
            int totalItems = allPlayerItemsUnique.size();
            int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
            
            paginationPanel.setVisible(totalPages > 1);
            prevPageButton.setEnabled(currentItemPage > 0);
            nextPageButton.setEnabled(currentItemPage < totalPages - 1);
            pageInfoLabel.setText(String.format("Pág %d/%d", currentItemPage + 1, Math.max(1,totalPages)));
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (itemsAtkPanelBackground != null) {
                g.drawImage(itemsAtkPanelBackground, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(new Color(50, 50, 70));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                Font errorFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(10f) : new Font("Monospaced", Font.BOLD, 10);
                g.setFont(errorFont);
                g.drawString("Fondo items_ataques.png no cargado", 10, 20);
            }
        }
    }


    public CombatViewPanel(SwingGUI parentFrame) {
        this.parentFrame = parentFrame;
        this.setPreferredSize(new Dimension(800, 600));
        this.setLayout(new BorderLayout());
        loadInterfaceImages();
        initComponents();
        initHpAnimationTimer();
        initTurnCountdownTimer(); 
    }

    public boolean isPlayer2Human() {
        return this.isPlayer2Human;
    }

    public boolean isPlayer1TurnForInput() {
        return this.isPlayer1TurnForInput;
    }


    private void loadInterfaceImages() {
        try {
            fightButtonIcon = loadImageIcon(INTERFACE_RESOURCE_PATH + "luchar_boton.png");
            pokemonButtonIcon = loadImageIcon(INTERFACE_RESOURCE_PATH + "pokemon_boton.png");
            bagButtonIcon = loadImageIcon(INTERFACE_RESOURCE_PATH + "bolsa_boton.png");
            runButtonIcon = loadImageIcon(INTERFACE_RESOURCE_PATH + "huir_boton.png");

            URL bgUrl = getClass().getResource(INTERFACE_RESOURCE_PATH + "batalla_log.png");
            if (bgUrl != null) {
                bottomMenuBackgroundImage = ImageIO.read(bgUrl);
            } else {
                System.err.println("Error: No se pudo cargar " + INTERFACE_RESOURCE_PATH + "batalla_log.png");
            }

            URL itemsAtkBgUrl = getClass().getResource(INTERFACE_RESOURCE_PATH + "items_ataques.png");
            if (itemsAtkBgUrl != null) {
                itemsAtkPanelBackground = ImageIO.read(itemsAtkBgUrl);
            } else {
                System.err.println("Error CRÍTICO: No se pudo cargar " + INTERFACE_RESOURCE_PATH + "items_ataques.png");
            }

        } catch (Exception e) {
            System.err.println("Error al cargar imágenes de la interfaz de combate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ImageIcon loadImageIcon(String path) {
        URL imgUrl = getClass().getResource(path);
        if (imgUrl != null) {
            return new ImageIcon(imgUrl);
        } else {
            System.err.println("Error: No se pudo cargar la imagen del botón: " + path);
            return null;
        }
    }

    public void updateActionPromptLabel() { 
        Pokemon currentAttacker = isPlayer1TurnForInput ? playerPokemon : (isPlayer2Human ? opponentPokemon : null) ;
        if (parentFrame.isCombatPaused()) {
            actionPromptLabel.setText("PAUSADO");
        } else if (actionPromptLabel != null && currentAttacker != null && !currentAttacker.estaDebilitado()) {
            actionPromptLabel.setText("¿Qué hará " + currentAttacker.getNombre() + "?");
        } else if (actionPromptLabel != null) {
            actionPromptLabel.setText("Esperando...");
        }
    }


    public void setPlayer1GuiMoveSelector(MovementSelector.GUIQueueMovementSelector selector) {
        this.player1GuiMoveSelector = selector;
    }

    public void setPlayer2GuiMoveSelector(MovementSelector.GUIQueueMovementSelector selector) {
        this.player2GuiMoveSelector = selector;
    }

    public void setPlayer2AsHuman(boolean isHuman) {
        this.isPlayer2Human = isHuman;
    }

    public void setActivePlayerTurnForInput(boolean isP1Turn) {
        this.isPlayer1TurnForInput = isP1Turn;
        String currentTurnPlayerName = isP1Turn ?
            (player1Trainer != null ? player1Trainer.getName() : "Jugador 1") :
            (player2Trainer != null ? player2Trainer.getName() : "Jugador 2");
        System.out.println("[CombatViewPanel] setActivePlayerTurnForInput: Es turno de " + currentTurnPlayerName + " (isP1Turn=" + isP1Turn + ", isPlayer2Human=" + isPlayer2Human + ")");

        updateActionPromptLabel();
        boolean isHumanInputExpected = (isP1Turn && player1GuiMoveSelector != null) ||
                                     (!isP1Turn && isPlayer2Human && player2GuiMoveSelector != null);

        if (isHumanInputExpected && !parentFrame.isCombatPaused()) {
            System.out.println("[CombatViewPanel] Se espera entrada humana. Iniciando temporizador y mostrando panel de acciones.");
            startTurnTimer();
            showActionButtonsPanel();
        } else if (parentFrame.isCombatPaused()){
            System.out.println("[CombatViewPanel] El combate está pausado. No se inicia el temporizador.");
            stopTurnTimer(); 
            turnTimerLabel.setText("--");
        }
        else {
            System.out.println("[CombatViewPanel] No se espera entrada humana. Deteniendo temporizador.");
            stopTurnTimer();
            turnTimerLabel.setText(" ");
        }
    }


    private void initHpAnimationTimer() {
        hpAnimationTimer = new Timer(HP_ANIMATION_STEP_DURATION, e -> animateHpStep());
        hpAnimationTimer.setRepeats(true);
    }

    private void initTurnCountdownTimer() {
        topControlPanel = new JPanel(new GridBagLayout());
        topControlPanel.setOpaque(false);
        topControlPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints gbcTop = new GridBagConstraints();

        Font generalButtonFont = (parentFrame.getPixelArtFont() != null) ? parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 11f) : new Font("Monospaced", Font.BOLD, 11);
        Dimension smallButtonSize = new Dimension(90, 30);

        pauseButton = new JButton("Pausa");
        pauseButton.setFont(generalButtonFont);
        pauseButton.setBackground(PAUSE_RESTART_BUTTON_BG);
        pauseButton.setForeground(PAUSE_RESTART_BUTTON_FG);
        pauseButton.setPreferredSize(smallButtonSize);
        pauseButton.setMargin(new Insets(2,2,2,2));
        pauseButton.addActionListener(e -> parentFrame.toggleCombatPause());
        gbcTop.gridx = 0; gbcTop.gridy = 0; gbcTop.weightx = 0.1; gbcTop.anchor = GridBagConstraints.WEST;
        topControlPanel.add(pauseButton, gbcTop);

        turnTimerLabel = new JLabel(" ", SwingConstants.CENTER);
        if (parentFrame.getPixelArtFont() != null) {
            turnTimerLabel.setFont(parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 28f));
        } else {
            turnTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        }
        turnTimerLabel.setForeground(TIMER_TEXT_COLOR);
        gbcTop.gridx = 1; gbcTop.gridy = 0; gbcTop.weightx = 0.8; gbcTop.anchor = GridBagConstraints.CENTER;
        topControlPanel.add(turnTimerLabel, gbcTop);


        restartButton = new JButton("Reiniciar");
        restartButton.setFont(generalButtonFont);
        restartButton.setBackground(PAUSE_RESTART_BUTTON_BG);
        restartButton.setForeground(PAUSE_RESTART_BUTTON_FG);
        restartButton.setPreferredSize(smallButtonSize);
        restartButton.setMargin(new Insets(2,2,2,2));
        restartButton.addActionListener(e -> {
            if (parentFrame.isCombatPaused()) { 
                 int choice = JOptionPane.showConfirmDialog(this,
                        "¿Seguro que quieres reiniciar el combate?\nSe perderá el progreso actual.",
                        "Reiniciar Combate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    parentFrame.requestCombatRestart();
                }
            } else {
                 JOptionPane.showMessageDialog(this, "Debes pausar el combate para poder reiniciarlo.", "Pausa Requerida", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        gbcTop.gridx = 2; gbcTop.gridy = 0; gbcTop.weightx = 0.1; gbcTop.anchor = GridBagConstraints.EAST;
        topControlPanel.add(restartButton, gbcTop);

        this.add(topControlPanel, BorderLayout.NORTH);

        turnCountdownTimer = new Timer(1000, e -> {
            if (parentFrame.isCombatPaused()) return; 

            remainingTurnTime--;
            updateTimerLabel();
            if (remainingTurnTime <= 0) {
                stopTurnTimer();
                appendCombatLog("¡Tiempo agotado para " + (isPlayer1TurnForInput ? player1Trainer.getName() : player2Trainer.getName()) + "!");
                MovementSelector.GUIQueueMovementSelector currentSelector = isPlayer1TurnForInput ? player1GuiMoveSelector : (isPlayer2Human ? player2GuiMoveSelector : null);
                Pokemon currentPokemon = isPlayer1TurnForInput ? playerPokemon : opponentPokemon;
                if (currentSelector != null && currentPokemon != null) {
                    PlayerActionChoice timeoutAction = new PlayerActionChoice(PlayerActionType.ATTACK);
                    int moveIdx = 0;
                    if (currentPokemon.getMovimientos() != null && !currentPokemon.getMovimientos().isEmpty()) {
                        for (int i = 0; i < currentPokemon.getMovimientos().size(); i++) {
                            if (currentPokemon.getMovimientos().get(i) != null && currentPokemon.getMovimientos().get(i).puedeUsarse()) {
                                moveIdx = i;
                                break;
                            }
                        }
                    }
                    timeoutAction.moveIndex = moveIdx;
                    System.out.println("[CombatViewPanel] Tiempo agotado. Enviando acción por defecto: Ataque, mov. índice " + moveIdx);
                    currentSelector.submitPlayerAction(timeoutAction);
                }
                 showLogPanelWithMessage("¡Tiempo agotado! Se seleccionó una acción por defecto."); 
            }
        });
        turnCountdownTimer.setRepeats(true);
    }

    public void startTurnTimer() {
        if (parentFrame.isCombatPaused()) {
            System.out.println("[CombatViewPanel] No se inicia el temporizador, el combate está pausado.");
            updateTimerLabel(); 
            return;
        }
        remainingTurnTime = TURN_TIME_SECONDS;
        updateTimerLabel();
        turnCountdownTimer.start();
        System.out.println("[CombatViewPanel] Temporizador de turno iniciado (" + TURN_TIME_SECONDS + "s).");
    }

    public void stopTurnTimer() {
        turnCountdownTimer.stop();
        System.out.println("[CombatViewPanel] Temporizador de turno detenido.");
    }

    public void updateTimerLabel() {
        if (parentFrame.isCombatPaused()) {
            turnTimerLabel.setText("--");
        } else {
            turnTimerLabel.setText(String.format("%02d", remainingTurnTime));
        }
    }

    public void updatePauseButtonText(boolean isPaused) {
        pauseButton.setText(isPaused ? "Reanudar" : "Pausa");
    }


    private void animateHpStep() {
        boolean changed = false;
        if (playerPokemon != null && playerDisplayedHp != playerTargetHp) {
            if (playerDisplayedHp < playerTargetHp) playerDisplayedHp = Math.min(playerTargetHp, playerDisplayedHp + HP_POINTS_PER_STEP);
            else playerDisplayedHp = Math.max(playerTargetHp, playerDisplayedHp - HP_POINTS_PER_STEP);
            changed = true;
        }
        if (opponentPokemon != null && opponentDisplayedHp != opponentTargetHp) {
            if (opponentDisplayedHp < opponentTargetHp) opponentDisplayedHp = Math.min(opponentTargetHp, opponentDisplayedHp + HP_POINTS_PER_STEP);
            else opponentDisplayedHp = Math.max(opponentTargetHp, opponentDisplayedHp - HP_POINTS_PER_STEP);
            changed = true;
        }
        if (changed) repaint();
        if ((playerPokemon == null || playerDisplayedHp == playerTargetHp) &&
            (opponentPokemon == null || opponentDisplayedHp == opponentTargetHp)) {
            hpAnimationTimer.stop();
        }
    }

    public void updatePokemonHp(Pokemon pokemonAfectado, int nuevoHpReal) {
        if (pokemonAfectado == null) return;
        nuevoHpReal = Math.max(0, Math.min(nuevoHpReal, pokemonAfectado.getHpMax()));
        boolean isPlayerTarget = (pokemonAfectado == this.playerPokemon);
        boolean isOpponentTarget = (pokemonAfectado == this.opponentPokemon);

        if (isPlayerTarget) this.playerTargetHp = nuevoHpReal;
        else if (isOpponentTarget) this.opponentTargetHp = nuevoHpReal;
        else return;

        if (!hpAnimationTimer.isRunning()) {
            if ((isPlayerTarget && playerDisplayedHp != playerTargetHp) ||
                (isOpponentTarget && opponentDisplayedHp != opponentTargetHp)) {
                hpAnimationTimer.start();
            }
        }
    }

    public Pokemon getPlayerPokemon() { return playerPokemon; }
    public Pokemon getOpponentPokemon() { return opponentPokemon; }


    private void initComponents() {
        Font pixelFont = parentFrame.getPixelArtFont();
        Font logFont = (pixelFont != null) ? pixelFont.deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11);
        Font promptFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 16f) : new Font("SansSerif", Font.BOLD, 16);

        bottomMenuCardLayout = new CardLayout();
        bottomMenuPanel = new JPanel(bottomMenuCardLayout);
        bottomMenuPanel.setOpaque(false);
        bottomMenuPanel.setBorder(new EmptyBorder(0,0,0,0));

        actionsDisplayPanel = new BottomMenuBackgroundPanel(new GridBagLayout());
        GridBagConstraints gbcActionsLayout = new GridBagConstraints();

        JPanel textPromptPanel = new JPanel(new BorderLayout());
        textPromptPanel.setOpaque(false);
        actionPromptLabel = new JLabel("¿Qué hará...?", SwingConstants.CENTER);
        actionPromptLabel.setFont(promptFont);
        actionPromptLabel.setForeground(ACTION_PROMPT_TEXT_COLOR);
        textPromptPanel.add(actionPromptLabel, BorderLayout.CENTER);

        gbcActionsLayout.gridx = 0;
        gbcActionsLayout.gridy = 0;
        gbcActionsLayout.weightx = 0.4;
        gbcActionsLayout.weighty = 1.0;
        gbcActionsLayout.fill = GridBagConstraints.BOTH;
        gbcActionsLayout.anchor = GridBagConstraints.CENTER;
        gbcActionsLayout.insets = new Insets(5, 10, 5, 5);
        actionsDisplayPanel.add(textPromptPanel, gbcActionsLayout);

        JPanel commandButtonsGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        commandButtonsGrid.setOpaque(false);
        fightButton = createCommandButton(fightButtonIcon, "LUCHAR");
        pokemonButton = createCommandButton(pokemonButtonIcon, "POKéMON");
        bagButton = createCommandButton(bagButtonIcon, "BOLSA");
        runButton = createCommandButton(runButtonIcon, "HUIR");
        commandButtonsGrid.add(fightButton);
        commandButtonsGrid.add(pokemonButton);
        commandButtonsGrid.add(bagButton);
        commandButtonsGrid.add(runButton);

        gbcActionsLayout.gridx = 1;
        gbcActionsLayout.weightx = 0.6;
        actionsDisplayPanel.add(commandButtonsGrid, gbcActionsLayout);

        movesDisplayPanel = new ItemsAtkPanel("MOVES");
        itemsDisplayPanel = new ItemsAtkPanel("ITEMS");

        combatLogDisplayPanel = new BottomMenuBackgroundPanel(new BorderLayout(5,5));
        combatLogArea = new JTextArea(4, 20);
        setupTextArea(combatLogArea, logFont);
        logScrollPane = new JScrollPane(combatLogArea);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.setOpaque(false);
        logScrollPane.setBorder(BorderFactory.createEmptyBorder());
        combatLogArea.setMargin(new Insets(5, 25, 5, 10));

        JButton continueButton = new JButton("ACCIONES");
        Font generalButtonFont = (pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 14) : new Font("Monospaced", Font.BOLD, 14);
        continueButton.setFont(generalButtonFont);
        continueButton.setBackground(new Color(70, 70, 160, 220));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);
        continueButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            new EmptyBorder(5,10,5,10)
        ));
        continueButton.addActionListener(e -> {
            System.out.println("[CombatViewPanel] Botón 'ACCIONES' (desde panel de log) presionado para continuar al siguiente turno.");
            if (parentFrame != null && !parentFrame.isCombatPaused()) {
                parentFrame.signalCombatToContinueAfterLog();
            } else if (parentFrame != null && parentFrame.isCombatPaused()) {
                JOptionPane.showMessageDialog(parentFrame, "El combate está pausado. Reanuda para continuar.", "Pausa Activa", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        JPanel continueButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        continueButtonPanel.setOpaque(false);
        continueButtonPanel.add(continueButton);
        combatLogDisplayPanel.add(logScrollPane, BorderLayout.CENTER);
        combatLogDisplayPanel.add(continueButtonPanel, BorderLayout.SOUTH);

        bottomMenuPanel.add(actionsDisplayPanel, ACTIONS_PANEL_CARD);
        bottomMenuPanel.add(movesDisplayPanel, MOVES_PANEL_CARD);
        bottomMenuPanel.add(itemsDisplayPanel, ITEMS_PANEL_CARD);
        bottomMenuPanel.add(combatLogDisplayPanel, LOG_DISPLAY_PANEL_CARD);

        this.add(bottomMenuPanel, BorderLayout.SOUTH);

        fightButton.addActionListener(e -> {
            if (parentFrame.isCombatPaused()) return;
            System.out.println("[CombatViewPanel] Botón LUCHAR presionado.");
            showMovesSelectionPanel();
        });
        bagButton.addActionListener(e -> {
            if (parentFrame.isCombatPaused()) return;
            System.out.println("[CombatViewPanel] Botón BOLSA presionado.");
            showItemsSelectionPanel();
        });
        pokemonButton.addActionListener(e -> {
            if (parentFrame.isCombatPaused()) return;
            System.out.println("[CombatViewPanel] Botón POKéMON presionado.");
            Trainer trainerActivo = isPlayer1TurnForInput ?
                                    player1Trainer :
                                    (isPlayer2Human() ? player2Trainer : null);
            Pokemon pokemonActivoEnCampo = isPlayer1TurnForInput ?
                                        playerPokemon :
                                        opponentPokemon;

            if (trainerActivo != null) {
                parentFrame.showPokemonSwitchUI(trainerActivo, pokemonActivoEnCampo, false);
            } else {
                System.err.println("[CombatViewPanel] No se pudo determinar el entrenador activo para el cambio voluntario.");
            }
        });
        runButton.addActionListener(e -> {
            if (parentFrame.isCombatPaused()) return;
            System.out.println("[CombatViewPanel] Botón HUIR presionado.");
            PlayerActionChoice choice = new PlayerActionChoice(PlayerActionType.RUN);
            MovementSelector.GUIQueueMovementSelector currentSelector = isPlayer1TurnForInput ? player1GuiMoveSelector : (isPlayer2Human() ? player2GuiMoveSelector : null);
            if (currentSelector != null) {
                currentSelector.submitPlayerAction(choice);
                stopTurnTimer();
                showLogPanelWithMessage("Intentando huir...");
            } else {
                 System.err.println("[CombatViewPanel] Error: currentSelector es null al intentar huir.");
            }
        });

        updateActionPromptLabel();
        showActionButtonsPanel();
    }


    private JButton createCommandButton(ImageIcon icon, String fallbackText) {
        JButton button;
        Dimension fallbackButtonSize = new Dimension(120, 50);

        if (icon != null && icon.getImage() != null && icon.getIconWidth() > 0) {
            final Image btnImage = icon.getImage();
            button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    int buttonWidth = getWidth();
                    int buttonHeight = getHeight();

                    if (btnImage != null && buttonWidth > 0 && buttonHeight > 0) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int imgWidth = btnImage.getWidth(this);
                        int imgHeight = btnImage.getHeight(this);

                        if (imgWidth <= 0 || imgHeight <= 0) {
                            g2d.dispose();
                            return;
                        }
                        double imgAspect = (double) imgWidth / imgHeight;
                        int padding = (int) (Math.min(buttonWidth, buttonHeight) * 0.05);
                        int paddedButtonWidth = buttonWidth - (2 * padding);
                        int paddedButtonHeight = buttonHeight - (2 * padding);

                        if (paddedButtonWidth <=0 || paddedButtonHeight <=0) {
                            paddedButtonWidth = buttonWidth;
                            paddedButtonHeight = buttonHeight;
                            padding = 0;
                        }
                        int drawWidth, drawHeight;
                        if (imgAspect > (double)paddedButtonWidth/paddedButtonHeight) {
                            drawWidth = paddedButtonWidth;
                            drawHeight = (int) (paddedButtonWidth / imgAspect);
                        } else {
                            drawHeight = paddedButtonHeight;
                            drawWidth = (int) (paddedButtonHeight * imgAspect);
                        }
                        int x = padding + (paddedButtonWidth - drawWidth) / 2;
                        int y = padding + (paddedButtonHeight - drawHeight) / 2;
                        g2d.drawImage(btnImage, x, y, drawWidth, drawHeight, this);
                        g2d.dispose();
                    }
                }
            };
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
        } else {
            System.err.println("Icono nulo o inválido para el botón: " + fallbackText + ". Usando texto de fallback.");
            button = new JButton(fallbackText.toUpperCase());
            Font pixelFont = parentFrame.getPixelArtFont();
            button.setFont((pixelFont != null) ? pixelFont.deriveFont(Font.BOLD, 12f) : new Font("Monospaced", Font.BOLD, 12));
            button.setBackground(new Color(70, 70, 160, 200));
            button.setForeground(Color.WHITE);
            button.setPreferredSize(fallbackButtonSize);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
        }
        return button;
    }

    private void setupTextArea(JTextArea textArea, Font font) {
        textArea.setFont(font);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setForeground(Color.WHITE);
        textArea.setBackground(new Color(0,0,0, 120));
    }

    public void showActionButtonsPanel() {
        System.out.println("[CombatViewPanel] Mostrando panel de acciones (ACTIONS_PANEL_CARD).");
        updateActionPromptLabel();
        bottomMenuCardLayout.show(bottomMenuPanel, ACTIONS_PANEL_CARD);
    }

    public void showMovesSelectionPanel() {
        Pokemon currentActingPokemon = isPlayer1TurnForInput ? playerPokemon : (isPlayer2Human() ? opponentPokemon : null);
        MovementSelector.GUIQueueMovementSelector currentSelector = isPlayer1TurnForInput ? player1GuiMoveSelector : (isPlayer2Human() ? player2GuiMoveSelector : null);

        if (currentActingPokemon == null) {
            System.err.println("[CombatViewPanel] showMovesSelectionPanel: currentActingPokemon es null.");
            showLogPanelWithMessage("Error: No hay Pokémon activo para seleccionar movimiento.");
            return;
        }
        if (currentActingPokemon.getMovimientos() == null) {
            System.err.println("[CombatViewPanel] showMovesSelectionPanel: " + currentActingPokemon.getNombre() + " no tiene lista de movimientos (es null).");
            showLogPanelWithMessage("Error: " + currentActingPokemon.getNombre() + " no tiene movimientos.");
            return;
        }
         if (currentSelector == null) {
            System.err.println("[CombatViewPanel] showMovesSelectionPanel: currentSelector es null. No se puede enviar la acción.");
            showLogPanelWithMessage("Error interno: Selector de movimiento no disponible.");
            return;
        }
        System.out.println("[CombatViewPanel] Mostrando selección de movimientos para: " + currentActingPokemon.getNombre());


        JPanel slots = movesDisplayPanel.getSlotsPanel();
        JTextArea info = movesDisplayPanel.getInfoArea();
        slots.removeAll();
        info.setText("Selecciona un ataque.");

        List<Movements> moves = currentActingPokemon.getMovimientos();
        System.out.println("[CombatViewPanel] Movimientos de " + currentActingPokemon.getNombre() + ": " + moves);


        Font moveButtonFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 11f) : new Font("Monospaced", Font.BOLD, 11);

        for (int i = 0; i < 4; i++) {
            Movements move = (i < moves.size()) ? moves.get(i) : null;

            if (move == null) {
                 JButton emptySlotButton = new JButton("(Vacío)");
                 emptySlotButton.setFont(moveButtonFont);
                 emptySlotButton.setEnabled(false);
                 emptySlotButton.setOpaque(false);
                 emptySlotButton.setContentAreaFilled(false);
                 emptySlotButton.setBorderPainted(false);
                 emptySlotButton.setForeground(Color.GRAY);
                 slots.add(emptySlotButton);
                 continue;
            }


            JButton moveButton = new JButton(move.getNombre().toUpperCase());
            moveButton.setFont(moveButtonFont);
            moveButton.setOpaque(false);
            moveButton.setContentAreaFilled(false);
            moveButton.setBorderPainted(false);
            moveButton.setForeground(SLOT_TEXT_COLOR);
            moveButton.setHorizontalAlignment(SwingConstants.CENTER);

            moveButton.setEnabled(move.puedeUsarse());
            moveButton.setToolTipText("PP: " + move.getPpActual() + "/" + move.getPpMax());

            final int moveIndex = i;
            final Movements finalMove = move;

            moveButton.addActionListener(e -> {
                if (parentFrame.isCombatPaused()) return;
                System.out.println("[CombatViewPanel] Botón de movimiento '" + finalMove.getNombre() + "' (índice " + moveIndex + ") presionado.");
                PlayerActionChoice choice = new PlayerActionChoice(PlayerActionType.ATTACK);
                choice.moveIndex = moveIndex;
                currentSelector.submitPlayerAction(choice);
                stopTurnTimer();
                showLogPanelWithMessage(currentActingPokemon.getNombre() + " seleccionó " + finalMove.getNombre() + ". Esperando...");
            });
            moveButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    String desc = "Tipo: " + finalMove.getTipo().name() + " | Cat: " + finalMove.getCategoria().name() + "\n" +
                                  "PP: " + finalMove.getPpActual() + "/" + finalMove.getPpMax() + "\n" +
                                  "Poder: " + (finalMove.getPotencia() > 0 ? finalMove.getPotencia() : "-") +
                                  " | Prec: " + (finalMove.getPrecision() > 0 ? finalMove.getPrecision() : "-") + "\n";
                    if (finalMove.getEfecto() != null && finalMove.getEfecto().getNombreEfecto() != null) {
                        desc += "Efecto: " + finalMove.getEfecto().getNombreEfecto().replace("_", " ").toLowerCase();
                        if (finalMove.getEfecto().getProbabilidad() < 1.0 && finalMove.getEfecto().getProbabilidad() > 0) {
                            desc += " (" + (int)(finalMove.getEfecto().getProbabilidad() * 100) + "%)";
                        }
                    }
                    info.setText(desc);
                }
            });
            slots.add(moveButton);
        }

        slots.revalidate();
        slots.repaint();
        bottomMenuCardLayout.show(bottomMenuPanel, MOVES_PANEL_CARD);
    }

    // MODIFICADO para paginación de ítems
    public void showItemsSelectionPanel() {
        Trainer currentActingTrainer = isPlayer1TurnForInput ? player1Trainer : (isPlayer2Human() ? player2Trainer : null);
        MovementSelector.GUIQueueMovementSelector currentSelector = isPlayer1TurnForInput ? player1GuiMoveSelector : (isPlayer2Human() ? player2GuiMoveSelector : null);

        if (currentActingTrainer == null) {
             System.err.println("[CombatViewPanel] showItemsSelectionPanel: currentActingTrainer es null.");
             showLogPanelWithMessage("Error: No se puede acceder a la mochila del jugador actual.");
             return;
        }
        if (currentActingTrainer.getItems() == null) {
             System.err.println("[CombatViewPanel] showItemsSelectionPanel: La lista de ítems de " + currentActingTrainer.getName() + " es null.");
             showLogPanelWithMessage("Error: " + currentActingTrainer.getName() + " no tiene una mochila de ítems.");
             return;
        }
        if (currentSelector == null) {
            System.err.println("[CombatViewPanel] showItemsSelectionPanel: currentSelector es null.");
            showLogPanelWithMessage("Error interno: Selector de ítems no disponible.");
            return;
        }
        System.out.println("[CombatViewPanel] Mostrando selección de ítems para: " + currentActingTrainer.getName());

        JPanel slots = itemsDisplayPanel.getSlotsPanel();
        JTextArea info = itemsDisplayPanel.getInfoArea();
        slots.removeAll();
        info.setText("Selecciona un ítem.");

        // Obtener ítems únicos y sus conteos
        List<Item> allItemsInBag = currentActingTrainer.getItems();
        Map<String, Long> itemCounts = allItemsInBag.stream()
            .collect(Collectors.groupingBy(Item::getName, Collectors.counting()));
        List<Item> uniqueDisplayableItems = new ArrayList<>();
        java.util.Set<String> addedItemNames = new java.util.HashSet<>();
        for (Item item : allItemsInBag) {
            if (addedItemNames.add(item.getName())) {
                uniqueDisplayableItems.add(item);
            }
        }
        itemsDisplayPanel.setAllPlayerItemsUnique(uniqueDisplayableItems); // Configurar para paginación

        Font itemButtonFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.BOLD, 11f) : new Font("Monospaced", Font.BOLD, 11);

        if (uniqueDisplayableItems.isEmpty()) {
            JLabel emptyLabel = new JLabel("Mochila vacía");
            emptyLabel.setFont(itemButtonFont);
            emptyLabel.setForeground(INFO_AREA_TEXT_COLOR);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            slots.setLayout(new BorderLayout()); // Cambiar layout para centrar
            slots.add(emptyLabel, BorderLayout.CENTER);
        } else {
            slots.setLayout(new GridLayout(2, 2, 7, 7)); // Asegurar GridLayout
            int startIndex = itemsDisplayPanel.currentItemPage * ItemsAtkPanel.ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ItemsAtkPanel.ITEMS_PER_PAGE, uniqueDisplayableItems.size());

            for (int i = startIndex; i < endIndex; i++) {
                Item item = uniqueDisplayableItems.get(i);
                long count = itemCounts.getOrDefault(item.getName(), 0L);
                JButton itemButton = new JButton(item.getNombre() + " x" + count);
                itemButton.setFont(itemButtonFont);
                itemButton.setOpaque(false);
                itemButton.setContentAreaFilled(false);
                itemButton.setBorderPainted(false);
                itemButton.setForeground(SLOT_TEXT_COLOR);
                itemButton.setHorizontalAlignment(SwingConstants.CENTER);

                final Item finalItem = item;
                itemButton.addActionListener(e -> {
                    if (parentFrame.isCombatPaused()) return;
                    System.out.println("[CombatViewPanel] Botón de ítem '" + finalItem.getNombre() + "' presionado.");
                    
                    Pokemon targetForThisItem = null;
                    // Determinar el objetivo basado en el tipo de ítem
                    if (finalItem instanceof Revive) {
                        // Para Revivir, la UI debe permitir seleccionar un Pokémon KO del equipo.
                        // Esta lógica de selección de objetivo para Revivir necesita implementación en PlayerInputHandler/PokemonSwitchFrame
                        // Por ahora, asumimos que PlayerActionChoice vendrá con itemTargetPokemon ya establecido para Revive.
                        // Si no, PlayerInputHandler debería solicitarlo.
                        // Aquí, si es Revive, el target DEBE ser provisto por la elección del usuario.
                        // Si choice.itemTargetPokemon no está seteado para Revive, es un error de flujo.
                         System.out.println("[CombatViewPanel] " + finalItem.getNombre() + " seleccionado. Se espera que el objetivo (Pokémon KO) sea manejado por PlayerInputHandler.");
                        // No se asigna targetForThisItem aquí, ItemUsageService lo tomará de PlayerActionChoice.
                    } else if (finalItem instanceof BombaRival) {
                        targetForThisItem = isPlayer1TurnForInput ? opponentPokemon : playerPokemon;
                         System.out.println("[CombatViewPanel] " + finalItem.getNombre() + " seleccionado. Objetivo: " + (targetForThisItem != null ? targetForThisItem.getNombre() : "N/A"));
                    } else { // Pociones, X-Items, BombaAliada
                        targetForThisItem = isPlayer1TurnForInput ? playerPokemon : (isPlayer2Human() ? opponentPokemon : null);
                         System.out.println("[CombatViewPanel] " + finalItem.getNombre() + " seleccionado. Objetivo (propio activo): " + (targetForThisItem != null ? targetForThisItem.getNombre() : "N/A"));
                    }

                    handleItemUsage(finalItem, targetForThisItem, currentSelector, currentActingTrainer);
                });
                itemButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        String desc = "Ítem: " + finalItem.getNombre() + "\nCantidad: " + count + "\n";
                        if (finalItem instanceof Potion) desc += "Restaura 20 PS.";
                        else if (finalItem instanceof SuperPotion) desc += "Restaura 50 PS.";
                        else if (finalItem instanceof HyperPotion) desc += "Restaura 200 PS.";
                        else if (finalItem instanceof Revive) desc += "Revive un Pokémon KO con la mitad de sus PS.";
                        else if (finalItem instanceof BombaAliada) desc += "Inflige 200 PS de daño al Pokémon aliado activo.";
                        else if (finalItem instanceof BombaRival) desc += "Inflige 200 PS de daño al Pokémon oponente activo.";
                        else desc += "Un ítem misterioso."; // Default
                        info.setText(desc);
                    }
                });
                slots.add(itemButton);
            }
        }
        itemsDisplayPanel.updateItemPaginationButtons();
        slots.revalidate();
        slots.repaint();
        bottomMenuCardLayout.show(bottomMenuPanel, ITEMS_PANEL_CARD);
    }

    private void handleItemUsage(Item item, Pokemon target, MovementSelector.GUIQueueMovementSelector activeSelector, Trainer activeTrainer) {
        System.out.println("[CombatViewPanel] handleItemUsage: Ítem: " + item.getNombre() +
                           ", Objetivo: " + (target != null ? target.getNombre() : "N/A") +
                           ", Entrenador: " + activeTrainer.getName());

        if (item == null || activeTrainer == null || activeSelector == null) {
            appendCombatLog("No se puede usar el ítem en este momento (dependencia nula).");
            showLogPanel();
            return;
        }

        PlayerActionChoice choice = new PlayerActionChoice(PlayerActionType.USE_ITEM);
        choice.itemToUse = item;
        choice.itemTargetPokemon = target; // El objetivo ya está determinado aquí

        activeSelector.submitPlayerAction(choice);
        stopTurnTimer();

        showLogPanelWithMessage(item.getNombre() + " seleccionado. Esperando acción del oponente...");
    }

    public void showLogPanel() {
        System.out.println("[CombatViewPanel] Mostrando panel de log (LOG_DISPLAY_PANEL_CARD).");
        bottomMenuCardLayout.show(bottomMenuPanel, LOG_DISPLAY_PANEL_CARD);
    }

    public void showLogPanelWithMessage(String initialMessage) {
        appendCombatLog(initialMessage);
        showLogPanel();
    }

    public void appendCombatLog(String message) {
        if (combatLogArea != null) {
            combatLogArea.append(message + "\n");
            combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());
        }
    }
    public void clearCombatLog() {
        if (combatLogArea != null) combatLogArea.setText("");
    }

    private Color getColorForType(Type.Tipo tipo) {
        if (tipo == null) return Color.LIGHT_GRAY;
        switch (tipo) {
            case FUEGO: return new Color(240, 128, 48); case AGUA: return new Color(104, 144, 240);
            case PLANTA: return new Color(120, 200, 80); case ELECTRICO: return new Color(248, 208, 48);
            case NORMAL: return new Color(168, 168, 120); case LUCHA: return new Color(192, 48, 40);
            case VOLADOR: return new Color(168, 144, 240); case VENENO: return new Color(160, 64, 160);
            case TIERRA: return new Color(224, 192, 104); case ROCA: return new Color(184, 160, 56);
            case BICHO: return new Color(168, 184, 32); case FANTASMA: return new Color(112, 88, 152);
            case ACERO: return new Color(184, 184, 208); case PSIQUICO: return new Color(248, 88, 136);
            case HIELO: return new Color(152, 216, 216); case DRAGON: return new Color(112, 56, 248);
            case SINIESTRO: return new Color(112, 88, 72); case HADA: return new Color(238, 153, 172);
            default: return Color.LIGHT_GRAY;
        }
    }


    public void setScenario(String scenarioNameOrPath) {
        this.currentScenarioName = scenarioNameOrPath;
        String resourcePath;

        if (scenarioNameOrPath.startsWith("/")) { 
            resourcePath = scenarioNameOrPath;
        } else { 
            resourcePath = SCENARIOS_RESOURCE_PATH + scenarioNameOrPath + ".png";
        }
        System.out.println("[CombatViewPanel setScenario] Attempting to load scenario from: " + resourcePath);


        URL imageUrl = getClass().getResource(resourcePath);
        try {
            backgroundImage = (imageUrl != null) ? ImageIO.read(imageUrl) : null;
            if (backgroundImage == null && imageUrl != null) {
                System.err.println("Error al leer la imagen del escenario (ImageIO.read devolvió null): " + resourcePath);
            } else if (imageUrl == null) {
                System.err.println("Imagen del escenario no encontrada en classpath: " + resourcePath);
                URL defaultBgUrl = getClass().getResource(BACKGROUNDS_RESOURCE_PATH + "default_battle_background.png");
                if (defaultBgUrl != null) {
                    backgroundImage = ImageIO.read(defaultBgUrl);
                    System.out.println("[CombatViewPanel setScenario] Fallback background loaded: " + BACKGROUNDS_RESOURCE_PATH + "default_battle_background.png");
                } else {
                     System.err.println("[CombatViewPanel setScenario] Fallback background also not found: " + BACKGROUNDS_RESOURCE_PATH + "default_battle_background.png");
                }
            }
        } catch (IOException e) {
            System.err.println("Excepción al cargar la imagen del escenario: " + resourcePath + "; " + e.getMessage());
            backgroundImage = null;
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setActivePokemonAndTrainers(Pokemon playerPokemon, Trainer player1Trainer, Pokemon opponentPokemon, Trainer player2Trainer) {
        System.out.println("[CombatViewPanel] setActivePokemonAndTrainers llamado.");
        if(playerPokemon != null) System.out.println("  Jugador 1: " + playerPokemon.getNombre() + " Movimientos: " + playerPokemon.getMovimientos()); else System.out.println("  Jugador 1: null");
        if(opponentPokemon != null) System.out.println("  Jugador 2: " + opponentPokemon.getNombre() + " Movimientos: " + opponentPokemon.getMovimientos()); else System.out.println("  Jugador 2: null");

        this.playerPokemon = playerPokemon;
        this.player1Trainer = player1Trainer;
        this.opponentPokemon = opponentPokemon;
        this.player2Trainer = player2Trainer;
        updateActionPromptLabel();

        if (playerPokemon != null) {
            this.playerPokemonNameForError = playerPokemon.getNombre();
            this.playerDisplayedHp = playerPokemon.getHpActual();
            this.playerTargetHp = playerPokemon.getHpActual();
            String playerSpritePath = POKEMON_ANIMATIONS_PATH + playerPokemon.getNombre().toLowerCase().replace(' ', '-') + "_back.gif";
            URL playerSpriteUrl = getClass().getResource(playerSpritePath);
            this.playerPokemonSprite = (playerSpriteUrl != null) ? new ImageIcon(playerSpriteUrl) : null;
            if(this.playerPokemonSprite == null && playerPokemon.getNombre() != null && !playerPokemon.getNombre().equals("Dummy")) System.err.println("Sprite del jugador no encontrado: " + playerSpritePath);

        } else {
            this.playerPokemonSprite = null; this.playerPokemonNameForError = "N/A";
            this.playerDisplayedHp = 0; this.playerTargetHp = 0;
        }

        if (opponentPokemon != null) {
            this.opponentPokemonNameForError = opponentPokemon.getNombre();
            this.opponentDisplayedHp = opponentPokemon.getHpActual();
            this.opponentTargetHp = opponentPokemon.getHpActual();
            String opponentSpritePath = POKEMON_ANIMATIONS_PATH + opponentPokemon.getNombre().toLowerCase().replace(' ', '-') + ".gif";
            URL opponentSpriteUrl = getClass().getResource(opponentSpritePath);
            this.opponentPokemonSprite = (opponentSpriteUrl != null) ? new ImageIcon(opponentSpriteUrl) : null;
            if(this.opponentPokemonSprite == null && opponentPokemon.getNombre() != null && !opponentPokemon.getNombre().equals("Dummy")) System.err.println("Sprite del oponente no encontrado: " + opponentSpritePath);
        } else {
            this.opponentPokemonSprite = null; this.opponentPokemonNameForError = "N/A";
            this.opponentDisplayedHp = 0; this.opponentTargetHp = 0;
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int scenarioDrawHeight = panelHeight;
        if (bottomMenuPanel != null && bottomMenuPanel.isVisible()) {
            int menuHeight = (int)(panelHeight * BOTTOM_MENU_HEIGHT_RATIO);
            menuHeight = Math.min(menuHeight, panelHeight);
            if (bottomMenuPanel.getPreferredSize().height != menuHeight || bottomMenuPanel.getPreferredSize().width != panelWidth) {
                 bottomMenuPanel.setPreferredSize(new Dimension(panelWidth, menuHeight));
                 SwingUtilities.invokeLater(this::revalidate);
            }
            scenarioDrawHeight = panelHeight - menuHeight;
             if (topControlPanel != null && topControlPanel.isVisible()) { 
                scenarioDrawHeight -= topControlPanel.getHeight();
            }
        }


        if (backgroundImage != null) {
             int imgWidth = backgroundImage.getWidth(null);
            int imgHeight = backgroundImage.getHeight(null);
            if (imgWidth > 0 && imgHeight > 0 && panelWidth > 0 && scenarioDrawHeight > 0) {
                double imgAspect = (double) imgWidth / imgHeight;
                double panelAspect = (double) panelWidth / scenarioDrawHeight;
                int drawX = 0, drawY = 0, drawWidth, drawHeight;

                if (imgAspect > panelAspect) {
                    drawHeight = scenarioDrawHeight;
                    drawWidth = (int) (drawHeight * imgAspect);
                    drawX = (panelWidth - drawWidth) / 2;
                } else {
                    drawWidth = panelWidth;
                    drawHeight = (int) (drawWidth / imgAspect);
                    drawY = (scenarioDrawHeight - drawHeight) / 2;
                }
                if (topControlPanel != null && topControlPanel.isVisible()) { 
                    drawY += topControlPanel.getHeight();
                }
                g2d.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, this);
            } else {
                g2d.drawImage(backgroundImage, 0, (topControlPanel != null && topControlPanel.isVisible() ? topControlPanel.getHeight() : 0), panelWidth, scenarioDrawHeight, this);
            }
        } else {
            g2d.setColor(Color.DARK_GRAY);
            int topOffset = (topControlPanel != null && topControlPanel.isVisible() ? topControlPanel.getHeight() : 0);
            g2d.fillRect(0,topOffset, panelWidth, scenarioDrawHeight);
            g2d.setColor(Color.WHITE);
            Font errorFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(16f) : new Font("Monospaced", Font.BOLD, 16);
            g2d.setFont(errorFont);
            String em1 = "Error: Escenario no cargado";
            String em2 = currentScenarioName != null ? currentScenarioName : "(Nombre no disponible)";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(em1, (panelWidth - fm.stringWidth(em1)) / 2, topOffset + scenarioDrawHeight / 2 - fm.getHeight() / 2);
            g2d.drawString(em2, (panelWidth - fm.stringWidth(em2)) / 2, topOffset + scenarioDrawHeight / 2 + fm.getHeight() / 2);
        }

        int topAreaHeight = 0;
        if (topControlPanel != null && topControlPanel.isVisible()) { 
            topAreaHeight = topControlPanel.getHeight();
        }

        int spriteDrawYOffset = topAreaHeight;


        int spriteW = (int) (panelWidth * SPRITE_WIDTH_RATIO);
        int spriteH = (int) ((panelHeight - topAreaHeight - (int)(panelHeight*BOTTOM_MENU_HEIGHT_RATIO)) * SPRITE_HEIGHT_RATIO);
        int pX = (int) (panelWidth * PLAYER_SPRITE_X_RATIO);
        int pY = spriteDrawYOffset + (int) ((panelHeight - topAreaHeight - (int)(panelHeight*BOTTOM_MENU_HEIGHT_RATIO)) * PLAYER_SPRITE_Y_RATIO);
        int oX = (int) (panelWidth * OPPONENT_SPRITE_X_RATIO);
        int oY = spriteDrawYOffset + (int) ((panelHeight - topAreaHeight- (int)(panelHeight*BOTTOM_MENU_HEIGHT_RATIO)) * OPPONENT_SPRITE_Y_RATIO);


        if (playerPokemonSprite != null && playerPokemonSprite.getImage() != null) g2d.drawImage(playerPokemonSprite.getImage(), pX, pY, spriteW, spriteH, this);
        else if (playerPokemonNameForError != null && !playerPokemonNameForError.equals("N/A")) drawSpriteErrorMessage(g2d, "Err: " + playerPokemonNameForError + "_back", pX, pY, spriteW, spriteH);

        if (opponentPokemonSprite != null && opponentPokemonSprite.getImage() != null) g2d.drawImage(opponentPokemonSprite.getImage(), oX, oY, spriteW, spriteH, this);
        else if (opponentPokemonNameForError != null && !opponentPokemonNameForError.equals("N/A")) drawSpriteErrorMessage(g2d, "Err: " + opponentPokemonNameForError, oX, oY, spriteW, spriteH);

        int statBoxDrawYOffset = topAreaHeight;
        if (playerPokemon != null && player1Trainer != null) drawStatBox(g2d, playerPokemon, player1Trainer, panelWidth, (panelHeight - topAreaHeight - (int)(panelHeight*BOTTOM_MENU_HEIGHT_RATIO)), true, playerDisplayedHp, statBoxDrawYOffset);
        if (opponentPokemon != null && player2Trainer != null) drawStatBox(g2d, opponentPokemon, player2Trainer, panelWidth, (panelHeight - topAreaHeight- (int)(panelHeight*BOTTOM_MENU_HEIGHT_RATIO)), false, opponentDisplayedHp, statBoxDrawYOffset);
    }

    private void drawSpriteErrorMessage(Graphics2D g2d, String message, int x, int y, int width, int height) {
        g2d.setColor(Color.RED);
        Font font = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(10f) : new Font("Monospaced", Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(message)) / 2;
        int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(message, textX, textY);
    }

    private void drawStatBox(Graphics2D g2d, Pokemon pokemon, Trainer trainer, int availW, int availH, boolean isPlayerBox, int dispHp, int yOffset) {
        int boxW = (int) (availW * STAT_BOX_WIDTH_RATIO);
        int boxH = (int) (availH * STAT_BOX_HEIGHT_RATIO);
        int boxX = isPlayerBox ? (int) (availW * PLAYER_STAT_BOX_X_RATIO) : (int) (availW * OPPONENT_STAT_BOX_X_RATIO);
        int boxY = yOffset + (isPlayerBox ? (int) (availH * PLAYER_STAT_BOX_Y_RATIO) : (int) (availH * OPPONENT_STAT_BOX_Y_RATIO));


        g2d.setColor(STAT_BOX_BACKGROUND);
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
        g2d.setColor(STAT_BOX_BORDER_COLOR);
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 15, 15);

        Font baseFont = parentFrame.getPixelArtFont()!=null ? parentFrame.getPixelArtFont() : new Font("Monospaced", Font.BOLD,12);
        Font nameFont = baseFont.deriveFont(Font.BOLD, (float)boxH * 0.20f);
        Font levelFont = baseFont.deriveFont(Font.PLAIN, (float)boxH * 0.18f);
        Font hpTextFont = baseFont.deriveFont(Font.PLAIN, (float)boxH * 0.17f);

        g2d.setColor(Color.BLACK);
        int padding = (int) (boxW * 0.05);
        int internalX = boxX + padding;
        int internalY = boxY + padding;

        String displayName = pokemon.getNombre().toUpperCase();
        g2d.setFont(nameFont);
        FontMetrics fmName = g2d.getFontMetrics();
        int nameY = internalY + fmName.getAscent();
        g2d.drawString(displayName, internalX, nameY);

        g2d.setFont(levelFont);
        FontMetrics fmLevel = g2d.getFontMetrics();
        String levelText = "Lv" + pokemon.getNivel();
        g2d.drawString(levelText, boxX + boxW - padding - fmLevel.stringWidth(levelText), nameY);

        int hpBarYOffset = (int)(boxH * 0.1);
        int hpBarActualY = nameY + fmName.getDescent() + hpBarYOffset;
        int hpBarHeight = (int) (boxH * 0.20);
        int hpBarComponentX = internalX + (int)(boxW * 0.01);
        int hpBarComponentWidth = boxW - (2 * padding) - (int)(boxW * 0.02);

        g2d.setColor(HP_BAR_BACKGROUND);
        g2d.fillRect(hpBarComponentX, hpBarActualY, hpBarComponentWidth, hpBarHeight);

        double hpPercentage = (pokemon.getHpMax() > 0) ? (double) dispHp / pokemon.getHpMax() : 0;
        int currentHpBarWidth = (int) (hpBarComponentWidth * hpPercentage);

        if (hpPercentage > 0.5) g2d.setColor(HP_GREEN);
        else if (hpPercentage > 0.2) g2d.setColor(HP_YELLOW);
        else g2d.setColor(HP_RED);
        g2d.fillRect(hpBarComponentX, hpBarActualY, currentHpBarWidth, hpBarHeight);

        g2d.setColor(STAT_BOX_BORDER_COLOR.darker());
        g2d.drawRect(hpBarComponentX, hpBarActualY, hpBarComponentWidth, hpBarHeight);

        if (isPlayerBox || (!isPlayerBox && isPlayer2Human())) {
            g2d.setFont(hpTextFont);
            g2d.setColor(Color.BLACK);
            String hpNumbers = pokemon.getHpActual() + "/" + pokemon.getHpMax();
            FontMetrics fmHpNumbers = g2d.getFontMetrics();
            int hpTextY = hpBarActualY + hpBarHeight + fmHpNumbers.getAscent() + (int)(boxH * 0.08);
            g2d.drawString(hpNumbers, boxX + boxW - padding - fmHpNumbers.stringWidth(hpNumbers), hpTextY);
        }
    }


    @Override
    public void onBattleStart(BattleStartEvent event) {
        clearCombatLog();
        appendCombatLog("¡Comienza la batalla entre " + event.getPlayer1().getName() + " y " + event.getPlayer2().getName() + "!");
        updatePauseButtonText(false); 
    }

    @Override
    public void onBattleEnd(BattleEndEvent event) {
        stopTurnTimer();
        turnTimerLabel.setText(" ");
        if (event.getWinner() != null) {
            appendCombatLog("¡" + event.getWinner().getName() + " ha ganado la batalla!");
        } else {
            appendCombatLog("¡La batalla ha terminado en empate!");
        }
        showLogPanel();
        pauseButton.setEnabled(false); 
        restartButton.setEnabled(false); 
    }

    @Override
    public void onTurnStart(TurnStartEvent event) {
        appendCombatLog("--- TURNO " + event.getTurnNumber() + " ---");
        updateActionPromptLabel();
        if (!parentFrame.isCombatPaused()) { 
            pauseButton.setEnabled(true);
            restartButton.setEnabled(true); 
        }
    }

    @Override
    public void onMoveUsed(MoveUsedEvent event) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(event.getAttacker().getNombre()).append(" usó ").append(event.getMove().getNombre());
        if (event.getTarget() != null) {
            logMsg.append(" contra ").append(event.getTarget().getNombre());
        }
        logMsg.append(".");

        if (event.getAdditionalMessage() != null && !event.getAdditionalMessage().isEmpty()) {
            logMsg.append(" ").append(event.getAdditionalMessage());
        }
        if (event.wasCritical()) {
            logMsg.append(" ¡Fue un golpe crítico!");
        }
        if (event.getDamageDealt() > 0 && event.getTarget() != null) {
            logMsg.append(" Causó ").append(event.getDamageDealt()).append(" PS de daño.");
        }
        appendCombatLog(logMsg.toString());
    }

    @Override
    public void onPokemonFainted(PokemonFaintedEvent event) {
        appendCombatLog("¡" + event.getFaintedPokemon().getNombre() + " de " + event.getOwner().getName() + " se ha debilitado!");
        updateActionPromptLabel();
    }

    @Override
    public void onPokemonHpChanged(PokemonHpChangedEvent event) {
        updatePokemonHp(event.getTargetPokemon(), event.getNewHp());
    }

    @Override
    public void onStatusApplied(StatusAppliedEvent event) {
        String sourceMsg = "";
        if (event.getSource() != null) {
            sourceMsg = " por " + event.getSource().getNombre();
        }
        appendCombatLog("¡" + event.getTarget().getNombre() + " ahora está " + event.getStatusName().toLowerCase() + sourceMsg + "!");
    }

    @Override
    public void onMessage(MessageEvent event) {
        appendCombatLog(event.getMessage());
    }

    @Override
    public void onPokemonChange(PokemonChangeEvent event) {
        System.out.println("[CombatViewPanel] Evento PokemonChange recibido: " + event.getNewPokemon().getNombre() + " entra por " + (event.getOldPokemon() != null ? event.getOldPokemon().getNombre() : "N/A"));

        setActivePokemonAndTrainers(
            (event.getTrainer() == player1Trainer) ? event.getNewPokemon() : playerPokemon,
            player1Trainer,
            (event.getTrainer() == player2Trainer) ? event.getNewPokemon() : opponentPokemon,
            player2Trainer
        );
        updateActionPromptLabel();
    }
}