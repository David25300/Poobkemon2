// Poobemon2/src/main/java/Face/SwingGUI.java
package Face;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import PokeBody.Data.DataLoader;
import PokeBody.Data.MovementsData;
import PokeBody.Data.PokemonData;
import PokeBody.Data.SaveManager;
import PokeBody.Data.SaveManager.PokemonSave;
import PokeBody.Data.SaveManager.SaveData;
import PokeBody.Data.SaveManager.TrainerSave;
import PokeBody.Services.CombatManager;
import PokeBody.Services.DamageCalculator;
import PokeBody.Services.GameModes.GameMode;
import PokeBody.Services.GameModes.NormalGameMode;
import PokeBody.Services.GameModes.SurvivalGameMode;
import PokeBody.Services.GameModes.TestGameMode;
import PokeBody.Services.GameSetupManager;
import PokeBody.domain.Item;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class SwingGUI extends JFrame {
    public CardLayout cardLayout;
    public JPanel mainPanel;

    private TeamSelectionPanel teamSelectionPanel;
    private ScenarioSelectionPanel scenarioSelectionPanel;
    private CombatModeSelectionPanel combatModeSelectionPanel;
    private CombatViewPanel combatViewPanel;
    private LoadGamePanel loadGamePanel;
    private ModeSelectionPanel modeSelectionPanel;
    private PokemonSwitchFrame pokemonSwitchFrame;

    private MovementSelector.GUIQueueMovementSelector player1GuiMoveSelector;
    private MovementSelector.GUIQueueMovementSelector player2GuiMoveSelector;
    private DamageCalculator damageCalculator;
    private GameSetupManager gameSetupManager;

    private GameMode activeGameMode;
    private CombatManager currentCombatManager;

    private List<Pokemon> allAvailablePokemons; // Lista de instancias de Pokemon
    private List<Item> allAvailableItems;
    private Map<String, Movements> allAvailableMoves;

    private int currentSaveSlotIndex = -1;
    private String currentSaveFileName = null;
    private Trainer currentPlayer1TrainerState;
    private Trainer currentPlayer2TrainerState;
    private String currentScenarioName;
    private CombatantType currentP1Type;
    private CombatantType currentP2Type;
    private int currentTurnForSave = 0;

    private Font pixelArtFont;
    private static final String FONT_RESOURCE_PATH = "/fonts/PressStart2P-Regular.ttf";
    public static final String POKEMON_SPRITES_BASE_PATH = "/PokeSprites/";
    private static final String DEFAULT_SURVIVAL_SCENARIO = "/PokeEscenarios/Bosque.png"; // Ruta para el fondo de supervivencia

    private boolean isCombatPaused = false;


    public enum CombatantType { PLAYER, AI }

    public SwingGUI() {
        super("PokeBody");
        player1GuiMoveSelector = new MovementSelector.GUIQueueMovementSelector();
        player2GuiMoveSelector = new MovementSelector.GUIQueueMovementSelector();
        damageCalculator = new DamageCalculator();
        gameSetupManager = new GameSetupManager();
        loadCustomFont();
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
        setVisible(true);
    }

    private void loadCustomFont() {
        try (InputStream is = getClass().getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (is == null) {
                System.err.println("Font resource not found: " + FONT_RESOURCE_PATH);
                pixelArtFont = new Font("Monospaced", Font.PLAIN, 12);
            } else {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
                pixelArtFont = baseFont.deriveFont(12f);
            }
        } catch (IOException | java.awt.FontFormatException e) {
            System.err.println("Error loading custom font: " + e.getMessage());
            pixelArtFont = new Font("Monospaced", Font.PLAIN, 12);
        }
    }

    // Getters
    public Font getPixelArtFont() { return pixelArtFont; }
    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getMainPanel() { return mainPanel; }
    public TeamSelectionPanel getTeamSelectionPanel() { return teamSelectionPanel; }
    public CombatViewPanel getCombatViewPanel() { return combatViewPanel; }
    public DamageCalculator getDamageCalculator() { return damageCalculator; }
    public MovementSelector.GUIQueueMovementSelector getPlayer1GuiMoveSelector() { return player1GuiMoveSelector; }
    public MovementSelector.GUIQueueMovementSelector getPlayer2GuiMoveSelector() { return player2GuiMoveSelector; }
    public List<Pokemon> getAllAvailablePokemons() { return allAvailablePokemons; }
    public List<Item> getAllAvailableItems() { return allAvailableItems; }
    public Map<String, Movements> getAllAvailableMoves() { return allAvailableMoves; }
    public int getCurrentSaveSlotIndex() { return currentSaveSlotIndex; }
    public Trainer getCurrentPlayer1TrainerState() { return currentPlayer1TrainerState; }
    public Trainer getCurrentPlayer2TrainerState() { return currentPlayer2TrainerState; }
    public CombatManager getCurrentCombatManager() { return this.currentCombatManager; }


    public PokemonSwitchFrame getPokemonSwitchFrame() {
        if (pokemonSwitchFrame == null) {
            pokemonSwitchFrame = new PokemonSwitchFrame(this);
        }
        return pokemonSwitchFrame;
    }

    public int getCurrentTurnForSave() {
        return currentTurnForSave;
    }

    public boolean isCombatPaused() {
        return isCombatPaused;
    }


    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        BackgroundPanel menuPanel = new BackgroundPanel("/backgrounds/Menu_Esmeralda.gif"); // Corregir ruta si es necesario
        menuPanel.setName("menuPanel");
        menuPanel.setLayout(new GridBagLayout());
        menuPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (loadGamePanel != null) loadGamePanel.refreshSaveSlotsDisplay();
                cardLayout.show(mainPanel, "LOAD");
            }
        });

        loadGamePanel = new LoadGamePanel(this);
        modeSelectionPanel = new ModeSelectionPanel(this);
        teamSelectionPanel = new TeamSelectionPanel(this);
        scenarioSelectionPanel = new ScenarioSelectionPanel(this);
        combatModeSelectionPanel = new CombatModeSelectionPanel(this);
        combatViewPanel = new CombatViewPanel(this); 

        combatViewPanel.setPlayer1GuiMoveSelector(player1GuiMoveSelector);
        combatViewPanel.setPlayer2GuiMoveSelector(player2GuiMoveSelector);

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(loadGamePanel, "LOAD");
        mainPanel.add(modeSelectionPanel, "MODE_SELECT");
        mainPanel.add(teamSelectionPanel, "TEAM_SELECT");
        mainPanel.add(scenarioSelectionPanel, "SCENARIO_SELECT");
        mainPanel.add(combatModeSelectionPanel, "COMBAT_MODE_SELECT");
        mainPanel.add(combatViewPanel, "COMBAT");

        setContentPane(mainPanel);
        cardLayout.show(mainPanel, "MENU"); 
    }

    public void handleSlotSelection(SaveData loadedDataFromSlot, int slotIndex) {
        this.currentSaveSlotIndex = slotIndex;
        this.currentSaveFileName = "save_slot_" + slotIndex + ".dat";
        System.out.println("Ranura " + slotIndex + " seleccionada. Archivo: " + currentSaveFileName);

        if (loadedDataFromSlot != null) {
            System.out.println("Cargando datos desde la ranura " + slotIndex + "...");
            try {
                loadBaseGameData();
                Trainer loadedP1 = reconstructTrainerFromSave(loadedDataFromSlot.aliado);
                Trainer loadedP2 = reconstructTrainerFromSave(loadedDataFromSlot.rival);

                if (loadedP1 != null && loadedP2 != null && loadedDataFromSlot.gameModeName != null && loadedDataFromSlot.scenarioName != null) {
                    this.currentPlayer1TrainerState = loadedP1;
                    this.currentPlayer2TrainerState = loadedP2;
                    this.currentScenarioName = loadedDataFromSlot.scenarioName;
                    this.currentP1Type = loadedDataFromSlot.player1CombatantType;
                    this.currentP2Type = loadedDataFromSlot.player2CombatantType;
                    this.currentTurnForSave = loadedDataFromSlot.turnNumber;

                    JOptionPane.showMessageDialog(this,
                            "Partida cargada desde la ranura " + (slotIndex + 1) + ".\n" +
                            "Modo: " + loadedDataFromSlot.gameModeName +
                            (loadedDataFromSlot.gameModeName.equals("Normal") && loadedDataFromSlot.normalGameSubMode != null ? " (" + loadedDataFromSlot.normalGameSubMode + ")" : "") +
                            "\nEscenario: " + loadedDataFromSlot.scenarioName + "\nTurno: " + loadedDataFromSlot.turnNumber +
                            "\nEntrenadores: " + loadedP1.getName() + " vs " + loadedP2.getName(),
                            "Partida Cargada", JOptionPane.INFORMATION_MESSAGE);

                    startGameFromLoadedData(loadedDataFromSlot.gameModeName, loadedDataFromSlot.normalGameSubMode,
                                            loadedP1, loadedP2, loadedDataFromSlot.scenarioName,
                                            loadedDataFromSlot.player1CombatantType, loadedDataFromSlot.player2CombatantType);
                } else {
                    showErrorAndReturnToMenu("Error al reconstruir datos esenciales de la partida. Iniciando como nueva partida.");
                    clearCurrentSessionState(true);
                    this.currentTurnForSave = 0;
                    cardLayout.show(mainPanel, "MODE_SELECT");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAndReturnToMenu("Error crítico al procesar datos guardados: " + e.getMessage() + ". Iniciando como nueva partida.");
                clearCurrentSessionState(true);
                this.currentTurnForSave = 0;
                cardLayout.show(mainPanel, "MODE_SELECT");
            }
        } else {
            System.out.println("Iniciando nueva partida en la ranura " + slotIndex + ".");
            clearCurrentSessionState(false); 
            this.currentTurnForSave = 0;
            JOptionPane.showMessageDialog(this,
                    "Iniciando nueva partida en la ranura " + (slotIndex + 1) + ".",
                    "Nueva Partida", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "MODE_SELECT");
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void clearCurrentSessionState(boolean clearSaveFileInfo) {
        this.currentPlayer1TrainerState = null;
        this.currentPlayer2TrainerState = null;
        this.currentScenarioName = null;
        this.activeGameMode = null;
        this.currentCombatManager = null; 
        this.currentTurnForSave = 0;
        this.isCombatPaused = false;
        if (clearSaveFileInfo) {
            this.currentSaveFileName = null;
            this.currentSaveSlotIndex = -1;
        }
    }

    private void startGameFromLoadedData(String gameModeName, String subMode, Trainer p1, Trainer p2, String scenario, CombatantType p1Type, CombatantType p2Type) {
        switch (gameModeName) {
            case "Normal":
                activeGameMode = new NormalGameMode(this);
                if (activeGameMode instanceof NormalGameMode && subMode != null) {
                     ((NormalGameMode) activeGameMode).setNormalGameSubMode(p1Type, p2Type);
                }
                break;
            case "Supervivencia":
                activeGameMode = new SurvivalGameMode(this); 
                break;
            case "Prueba":
                activeGameMode = new TestGameMode(this); 
                break;
            default:
                showErrorAndReturnToMenu("Modo de juego guardado no reconocido: " + gameModeName);
                return;
        }

        MovementSelector selectorP1 = (p1Type == CombatantType.PLAYER) ? player1GuiMoveSelector : new PokeBody.Services.AI.ExpertAISelector(damageCalculator);
        MovementSelector selectorP2 = (p2Type == CombatantType.PLAYER) ? player2GuiMoveSelector : new PokeBody.Services.AI.ExpertAISelector(damageCalculator);

        this.currentCombatManager = new CombatManager(p1, p2, selectorP1, selectorP2, combatViewPanel, this, activeGameMode);

        combatViewPanel.setPlayer2AsHuman(p2Type == CombatantType.PLAYER);
        combatViewPanel.setScenario(scenario); // El escenario se carga desde los datos guardados
        combatViewPanel.setActivePokemonAndTrainers(
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer1(), p1,
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer2(), p2
        );
        combatViewPanel.clearCombatLog();
        cardLayout.show(mainPanel, "COMBAT");

        isCombatPaused = false;
        this.currentCombatManager.setPaused(false);
        combatViewPanel.updatePauseButtonText(false);
        combatViewPanel.updateActionPromptLabel();

        new Thread(() -> {
            currentCombatManager.run(this.currentTurnForSave);
        }).start();
    }

    private List<Pokemon> createRandomTeamForPlayer(int teamSize, String context) {
        List<Pokemon> randomTeam = new ArrayList<>();
        if (allAvailablePokemons == null || allAvailablePokemons.isEmpty() || allAvailableMoves == null || allAvailableMoves.isEmpty()) {
            String errorMsg = "SwingGUI.createRandomTeamForPlayer ("+context+"): Datos base no disponibles para crear equipo.";
            System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        List<PokemonData> allPokemonDataList;
        try {
            allPokemonDataList = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);
        } catch (Exception e) {
            String errorMsg = "SwingGUI.createRandomTeamForPlayer ("+context+"): Error cargando PokemonData: " + e.getMessage();
            System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }

        if (allPokemonDataList.isEmpty()) {
            String errorMsg = "SwingGUI.createRandomTeamForPlayer ("+context+"): No hay PokemonData disponibles.";
            System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        List<PokemonData> availableToPick = new ArrayList<>(allPokemonDataList);
        Random random = new Random();

        for (int i = 0; i < teamSize; i++) {
            if (availableToPick.isEmpty()) { // Si nos quedamos sin Pokémon únicos, empezamos a repetir
                 if (allPokemonDataList.isEmpty()) break; // No hay nada que elegir
                 availableToPick.addAll(allPokemonDataList); // Rellenar para permitir repetidos
            }
            PokemonData pickedData = availableToPick.remove(random.nextInt(availableToPick.size()));
            Pokemon teamMember = new Pokemon(
                pickedData,
                this.allAvailableMoves, 
                100 
            );
            randomTeam.add(teamMember);
        }
        if (randomTeam.size() < teamSize && teamSize > 0) {
             System.out.println("Advertencia: No se pudieron crear " + teamSize + " Pokémon para el equipo aleatorio del jugador ("+context+"). Se crearon " + randomTeam.size());
        }
        if (randomTeam.isEmpty() && teamSize > 0) {
             String errorMsg = "No se pudieron crear Pokémon para el equipo aleatorio del jugador ("+context+").";
             System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        System.out.println("Equipo aleatorio creado para " + context + " con " + randomTeam.size() + " Pokémon.");
        return randomTeam;
    }

    public void startGame(String mode) {
         try {
             loadBaseGameData();
         } catch (Exception e) {
              showErrorAndReturnToMenu("Error CRÍTICO al cargar datos base del juego:\n" + e.getMessage());
              e.printStackTrace();
              return;
         }

         switch (mode) {
             case "Normal":
                 activeGameMode = new NormalGameMode(this);
                 ((NormalGameMode) activeGameMode).setupAndStart();
                 break;
             case "Supervivencia":
                 activeGameMode = new SurvivalGameMode(this);
                 try {
                     List<Pokemon> playerTeamSurvival = createRandomTeamForPlayer(6, "Supervivencia");
                     this.currentPlayer1TrainerState = new Trainer("Jugador (Supervivencia)", playerTeamSurvival, new ArrayList<>());
                 } catch (IllegalStateException e) {
                     showErrorAndReturnToMenu("Error al crear equipo para Supervivencia: " + e.getMessage());
                     return;
                 }
                 Trainer opponentSurvival = activeGameMode.getInitialOpponent(this.currentPlayer1TrainerState);
                 
                 if (this.currentPlayer1TrainerState.getteam().isEmpty()) {
                     showErrorAndReturnToMenu("Error: El equipo del jugador no se configuró para el Modo Supervivencia.");
                     return;
                 }

                 if (opponentSurvival != null && !opponentSurvival.getteam().isEmpty()) {
                     proceedToCombat(this.currentPlayer1TrainerState, opponentSurvival, DEFAULT_SURVIVAL_SCENARIO, CombatantType.PLAYER, CombatantType.AI);
                 } else {
                     showErrorAndReturnToMenu("No se pudo crear el oponente para Supervivencia (equipo vacío o nulo).");
                 }
                 break;
             case "Prueba":
                 activeGameMode = new TestGameMode(this);
                 try {
                     List<Pokemon> playerTeamTest = createRandomTeamForPlayer(3, "Prueba"); 
                     this.currentPlayer1TrainerState = new Trainer("Tester Alfa", playerTeamTest, new ArrayList<>());
                 } catch (IllegalStateException e) {
                     showErrorAndReturnToMenu("Error al crear equipo para Modo Prueba: " + e.getMessage());
                     return;
                 }
                 Trainer opponentTest = activeGameMode.getInitialOpponent(this.currentPlayer1TrainerState);
                 
                 if (this.currentPlayer1TrainerState.getteam().isEmpty()) {
                     showErrorAndReturnToMenu("Error: El equipo del jugador no se configuró para el Modo Prueba.");
                     return;
                 }
                 
                 if (opponentTest != null && !opponentTest.getteam().isEmpty()) {
                     proceedToCombat(this.currentPlayer1TrainerState, opponentTest, "Gimnasio_1", CombatantType.PLAYER, CombatantType.AI);
                 } else {
                     showErrorAndReturnToMenu("No se pudo crear el oponente para Prueba (equipo vacío o nulo).");
                 }
                 break;
             default:
                 showErrorAndReturnToMenu("Modo de juego no reconocido: " + mode);
                 break;
         }
         mainPanel.revalidate();
         mainPanel.repaint();
    }

    /**
     * Continúa el Modo Supervivencia con un nuevo oponente.
     * Este método es llamado por SurvivalGameMode después de que el jugador gana una ronda.
     */
    public void continueSurvivalModeCombat(Trainer playerTrainer, Trainer nextOpponent, String scenarioName) {
        System.out.println("[SwingGUI] Continuando Modo Supervivencia. Jugador: " + playerTrainer.getName() + ", Nuevo Oponente: " + nextOpponent.getName());
        
        // Asegurar que activeGameMode sigue siendo SurvivalGameMode
        if (!(activeGameMode instanceof SurvivalGameMode)) {
            showErrorAndReturnToMenu("Error: Intento de continuar Supervivencia sin el modo de juego correcto activo.");
            return;
        }
        
        // Actualizar los entrenadores para la nueva ronda
        this.currentPlayer1TrainerState = playerTrainer;
        this.currentPlayer2TrainerState = nextOpponent;
        this.currentScenarioName = scenarioName;
        this.currentP1Type = CombatantType.PLAYER; // El jugador siempre es PLAYER
        this.currentP2Type = CombatantType.AI;     // El oponente en supervivencia siempre es AI

        // Crear un nuevo CombatManager para la nueva ronda
        MovementSelector selectorP1 = player1GuiMoveSelector; // El jugador sigue siendo humano
        MovementSelector selectorP2 = new PokeBody.Services.AI.ExpertAISelector(damageCalculator); // Nueva IA para el nuevo oponente

        // El CombatManager se crea con el mismo activeGameMode (SurvivalGameMode)
        this.currentCombatManager = new CombatManager(
            this.currentPlayer1TrainerState, 
            this.currentPlayer2TrainerState, 
            selectorP1, 
            selectorP2, 
            combatViewPanel, // El CombatViewPanel se reutiliza
            this, 
            activeGameMode // El mismo activeGameMode (SurvivalGameMode)
        );

        // Configurar CombatViewPanel para la nueva ronda
        combatViewPanel.setPlayer2AsHuman(false); // Oponente es AI
        combatViewPanel.setScenario(scenarioName); // Usar el escenario de supervivencia
        combatViewPanel.setActivePokemonAndTrainers(
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer1(), 
            this.currentPlayer1TrainerState,
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer2(), 
            this.currentPlayer2TrainerState
        );
        // No limpiar el log aquí, para que se acumule entre rondas de supervivencia, o limpiarlo si se prefiere.
        // combatViewPanel.clearCombatLog(); 
        cardLayout.show(mainPanel, "COMBAT"); // Asegurarse de que el panel de combate esté visible

        // Iniciar el bucle de combate para la nueva ronda
        isCombatPaused = false;
        this.currentCombatManager.setPaused(false);
        combatViewPanel.updatePauseButtonText(false);
        combatViewPanel.updateActionPromptLabel();

        new Thread(() -> {
            currentCombatManager.run(); // Iniciar desde el turno 1 para la nueva ronda
        }).start();
    }


    public void showErrorAndReturnToMenu(String message) {
         JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
         cardLayout.show(mainPanel, "MENU");
    }

    public void loadBaseGameData() throws Exception {
        if (allAvailablePokemons == null || allAvailableMoves == null || allAvailableItems == null) {
             System.out.println("Cargando datos base del juego...");
             GameSetupManager.NormalModeSetupData setupData = gameSetupManager.setupNormalMode();
             this.allAvailablePokemons = setupData.getAvailablePokemons();
             this.allAvailableItems = setupData.getAvailableItems();

             List<MovementsData> movementsDataList = DataLoader.loadMovementsData(GameSetupManager.MOVEMENTS_RESOURCE_PATH);
             if (movementsDataList == null) throw new IOException("No se pudieron cargar los datos de movimientos.");
             this.allAvailableMoves = movementsDataList.stream()
                     .map(Movements::fromData)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toMap(Movements::getNombre, Function.identity(), (existing, replacement) -> existing));

             if (this.allAvailablePokemons == null || this.allAvailablePokemons.isEmpty()) {
                 throw new Exception("No se pudieron cargar los datos de Pokémon desde GameSetupManager.");
             }
             if (this.allAvailableMoves == null || this.allAvailableMoves.isEmpty()) {
                 throw new Exception("No se pudieron cargar los datos de movimientos.");
             }
             if (this.allAvailableItems == null || this.allAvailableItems.isEmpty()){
                 System.err.println("Advertencia: Ningún ítem fue inicializado por GameSetupManager.");
                 this.allAvailableItems = new ArrayList<>();
             }
             System.out.println("Datos base cargados: " + allAvailablePokemons.size() + " Pokémon, " + allAvailableMoves.size() + " Movimientos, " + allAvailableItems.size() + " Ítems.");
        }
    }

    public PokemonData findPokemonDataByName(String name) {
         try {
             List<PokemonData> pokemonDataList = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);
             return pokemonDataList.stream()
                     .filter(pd -> pd != null && name.equalsIgnoreCase(pd.getNombre()))
                     .findFirst()
                     .orElse(null);
         } catch (Exception e) {
             System.err.println("Error cargando PokemonData para encontrar " + name + ": " + e.getMessage());
             return null;
         }
     }

    public void showScenarioSelection(Trainer player1Trainer, Trainer player2Trainer) {
        this.currentPlayer1TrainerState = player1Trainer;
        this.currentPlayer2TrainerState = player2Trainer;
        scenarioSelectionPanel.setSelectedTrainers(player1Trainer, player2Trainer);
        cardLayout.show(mainPanel, "SCENARIO_SELECT");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void showCombatModeSelection(Trainer p1, Trainer p2, String scenarioName) {
        this.currentPlayer1TrainerState = p1;
        this.currentPlayer2TrainerState = p2;
        this.currentScenarioName = scenarioName;
        combatModeSelectionPanel.setSelectedTrainersAndScenario(p1, p2, scenarioName);
        cardLayout.show(mainPanel, "COMBAT_MODE_SELECT");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void proceedToCombat(Trainer player1, Trainer player2, String scenario, CombatantType p1Type, CombatantType p2Type) {
        this.currentPlayer1TrainerState = player1;
        this.currentPlayer2TrainerState = player2;
        this.currentScenarioName = scenario;
        this.currentP1Type = p1Type;
        this.currentP2Type = p2Type;

        if (activeGameMode == null) {
            System.err.println("Error: activeGameMode es null en proceedToCombat. No se puede iniciar.");
            if (this.currentPlayer1TrainerState != null && this.currentPlayer2TrainerState != null) {
                System.out.println("Advertencia: activeGameMode era null. Instanciando NormalGameMode por defecto.");
                activeGameMode = new NormalGameMode(this); 
            } else {
                showErrorAndReturnToMenu("Error interno: Modo de juego no establecido y no se pueden crear entrenadores.");
                return;
            }
        }
        
        if (activeGameMode instanceof NormalGameMode) {
            ((NormalGameMode) activeGameMode).setNormalGameSubMode(p1Type, p2Type);
        }
        
        MovementSelector selectorP1 = (p1Type == CombatantType.PLAYER) ? player1GuiMoveSelector : new PokeBody.Services.AI.ExpertAISelector(damageCalculator);
        MovementSelector selectorP2 = (p2Type == CombatantType.PLAYER) ? player2GuiMoveSelector : new PokeBody.Services.AI.ExpertAISelector(damageCalculator);

        this.currentCombatManager = new CombatManager(player1, player2, selectorP1, selectorP2, combatViewPanel, this, activeGameMode);

        combatViewPanel.setPlayer2AsHuman(p2Type == CombatantType.PLAYER);
        // Usar la ruta del fondo especificada si es Supervivencia, sino el escenario normal
        if (activeGameMode instanceof SurvivalGameMode) {
            combatViewPanel.setScenario(DEFAULT_SURVIVAL_SCENARIO); // Usar la constante para el escenario de supervivencia
        } else {
            combatViewPanel.setScenario(scenario);
        }
        
        combatViewPanel.setActivePokemonAndTrainers(
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer1(), player1,
            currentCombatManager.getBattlefieldState().getActivePokemonPlayer2(), player2
        );
        combatViewPanel.clearCombatLog();
        cardLayout.show(mainPanel, "COMBAT");

        isCombatPaused = false;
        this.currentCombatManager.setPaused(false);
        combatViewPanel.updatePauseButtonText(false);
        combatViewPanel.updateActionPromptLabel();

        new Thread(() -> {
            currentCombatManager.run(); 
        }).start();
    }

    public void startCombat(Trainer playerTrainer, Trainer opponentTrainer, String scenarioName,
                            CombatantType player1Type, CombatantType player2Type) {
        if (activeGameMode == null) {
            System.err.println("Advertencia: activeGameMode es null en startCombat. Instanciando NormalGameMode.");
            activeGameMode = new NormalGameMode(this); 
        }
        if (activeGameMode instanceof NormalGameMode) {
            ((NormalGameMode) activeGameMode).setNormalGameSubMode(player1Type, player2Type);
        }
        proceedToCombat(playerTrainer, opponentTrainer, scenarioName, player1Type, player2Type);
        
    }

    public void requestAutoSave(Trainer p1, Trainer p2, int turnNumber) {
        if (currentSaveFileName == null || currentSaveSlotIndex == -1) {
            System.out.println("Guardado automático omitido: No hay ranura de guardado seleccionada.");
            return;
        }
        if (activeGameMode == null) {
            System.err.println("Guardado automático omitido: No hay modo de juego activo.");
            return;
        }

        String gameModeName = activeGameMode.getModeName();
        String normalSubMode = null;
        
        CombatantType p1TypeToSave = this.currentP1Type;
        CombatantType p2TypeToSave = this.currentP2Type;

        if (activeGameMode instanceof NormalGameMode) {
             normalSubMode = ((NormalGameMode) activeGameMode).getNormalGameSubMode();
        }
      
        try {
            SaveManager.guardar(currentSaveFileName, p1, p2,
                                gameModeName, normalSubMode, this.currentScenarioName, turnNumber,
                                p1TypeToSave, p2TypeToSave);
            System.out.println("Partida guardada (" + gameModeName + ") en " + currentSaveFileName + ", Turno: " + turnNumber);
            if (combatViewPanel != null) {
                combatViewPanel.appendCombatLog("Partida guardada automáticamente (Turno " + turnNumber + ")");
            }
        } catch (IOException e) {
            System.err.println("Error durante el guardado automático en " + currentSaveFileName + ": " + e.getMessage());
            if (combatViewPanel != null) {
                combatViewPanel.appendCombatLog("¡Error al guardar automáticamente la partida!");
            }
        }
    }

    private Trainer reconstructTrainerFromSave(TrainerSave trainerSave) {
        if (trainerSave == null) return null;
        System.out.println("[SwingGUI reconstructTrainerFromSave] Reconstruyendo entrenador: " + trainerSave.nombreEntrenador);
        try {
            if (this.allAvailableMoves == null || this.allAvailableItems == null ) {
                System.out.println("[SwingGUI reconstructTrainerFromSave] Datos base (movimientos/items) no cargados. Cargando ahora...");
                loadBaseGameData(); 
            }
        } catch (Exception e) {
            System.err.println("Error crítico al cargar datos base para reconstruir entrenador: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        List<Pokemon> reconstructedTeam = new ArrayList<>();
        if (trainerSave.equipo != null) {
            for (PokemonSave ps : trainerSave.equipo) {
                if (ps == null) {
                    System.err.println("  PokemonSave en la lista del equipo es null, omitiendo.");
                    continue;
                }
                System.out.println("  Reconstruyendo Pokémon: " + ps.nombreEspecie + " Nivel guardado: " + ps.nivel);
                PokemonData pData = findPokemonDataByName(ps.nombreEspecie); 
                if (pData != null && this.allAvailableMoves != null) { 
                    Pokemon pokemon = new Pokemon(pData, this.allAvailableMoves, ps.nivel);
                    pokemon.setHpActual(ps.hpActual);
                    if (ps.estadoNombre != null && !ps.estadoNombre.isEmpty()) {
                        pokemon.setEstadoActivo(ps.estadoNombre, ps.estadoDuracion);
                    }  
                    if (ps.statBoosts != null) {
                        pokemon.setBoosts(ps.statBoosts);
                    }
                    if (ps.precisionBoost != 0) pokemon.modificarPrecisionBoost(ps.precisionBoost);
                    if (ps.evasionBoost != 0) pokemon.modificarEvasionBoost(ps.evasionBoost);
                    if (ps.movimientos != null && ps.movimientos.size() == 4) {
                        for (int i = 0; i < 4; i++) {
                            SaveManager.MoveSave ms = ps.movimientos.get(i);
                            if (ms != null && ms.moveName != null) {
                                Movements baseMove = this.allAvailableMoves.get(ms.moveName);
                                if (baseMove != null) {
                                    Movements learnedMoveInstance = new Movements(baseMove);
                                    learnedMoveInstance.setPpActual(ms.currentPp);
                                    pokemon.setLearnedMove(i, learnedMoveInstance);
                                } else {
                                    pokemon.setLearnedMove(i, null);
                                    System.err.println("Advertencia: Movimiento base '" + ms.moveName + "' no encontrado en allAvailableMoves al reconstruir " + ps.nombreEspecie);
                                }
                            } else {
                                pokemon.setLearnedMove(i, null);
                            }
                        }
                    }
                    reconstructedTeam.add(pokemon);
                } else {
                    System.err.println("No se encontró PokemonData para: " + ps.nombreEspecie + " o allAvailableMoves es null. Pokémon omitido.");
                }
            }
        }

        List<Item> reconstructedItems = new ArrayList<>();
        if (trainerSave.itemsNombres != null && this.allAvailableItems != null) {
            for (String itemName : trainerSave.itemsNombres) {
                this.allAvailableItems.stream()
                    .filter(item -> item.getName().equals(itemName))
                    .findFirst()
                    .ifPresent(reconstructedItems::add);
            }
        }

        return new Trainer(trainerSave.nombreEntrenador, reconstructedTeam, reconstructedItems);
    }

    public void showPokemonSwitchUI(Trainer trainer, Pokemon currentInBattle, boolean isMandatorySwitch) {
        System.out.println("[SwingGUI] Solicitando mostrar PokemonSwitchUI para " + trainer.getName() + ". Obligatorio: " + isMandatorySwitch);
        if (getPokemonSwitchFrame() != null) {
            getPokemonSwitchFrame().displaySwitchOptions(trainer, currentInBattle, isMandatorySwitch);
        } else {
            System.err.println("[SwingGUI] Error: pokemonSwitchFrame es null. No se puede mostrar la UI de cambio.");
        }
    }

    // --- Métodos para Pausa y Reinicio ---
    public void toggleCombatPause() {
        isCombatPaused = !isCombatPaused;
        System.out.println("[SwingGUI] Combat Paused: " + isCombatPaused);
        if (combatViewPanel != null) {
            combatViewPanel.updatePauseButtonText(isCombatPaused);
            combatViewPanel.updateActionPromptLabel();
            if (isCombatPaused) {
                combatViewPanel.stopTurnTimer();
            } else {
                boolean isHumanInputExpected = (combatViewPanel.isPlayer1TurnForInput() && player1GuiMoveSelector != null) ||
                                             (!combatViewPanel.isPlayer1TurnForInput() && combatViewPanel.isPlayer2Human() && player2GuiMoveSelector != null);
                if(isHumanInputExpected) {
                    combatViewPanel.startTurnTimer();
                } else {
                    combatViewPanel.updateTimerLabel();
                }
            }
        }
        if (currentCombatManager != null) {
            currentCombatManager.setPaused(isCombatPaused);
        }
    }

    public void requestCombatRestart() {
        if (currentCombatManager != null) {
            System.out.println("[SwingGUI] Solicitando reinicio de combate...");
            boolean restartInitiated = currentCombatManager.restartCombat();
            if (restartInitiated) {
                isCombatPaused = false;
                if (combatViewPanel != null) {
                    combatViewPanel.updatePauseButtonText(false);
                }
                System.out.println("[SwingGUI] Proceso de reinicio de combate iniciado por CombatManager.");
            } else {
                System.err.println("[SwingGUI] CombatManager no pudo iniciar el reinicio.");
                JOptionPane.showMessageDialog(this, "No se pudo reiniciar el combate en este momento.", "Error de Reinicio", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.err.println("[SwingGUI] No se puede reiniciar: currentCombatManager es null.");
            JOptionPane.showMessageDialog(this, "No hay un combate activo para reiniciar.", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void requestRunFromCombat() {
        if (currentCombatManager != null) {
            System.out.println("[SwingGUI] Solicitando huida del combate...");
            currentCombatManager.playerAttemptedToRun();
        } else {
            System.err.println("[SwingGUI] No se puede huir: currentCombatManager es null.");
            handleRunFromCombat();
        }
    }

    public void handleRunFromCombat() {
        System.out.println("[SwingGUI] handleRunFromCombat: El jugador ha huido.");
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "¡Has huido del combate!", "Huida", JOptionPane.INFORMATION_MESSAGE);
            clearCurrentSessionState(false); 
            if (loadGamePanel != null) {
                loadGamePanel.refreshSaveSlotsDisplay();
            }
            cardLayout.show(mainPanel, "LOAD");
            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    public void handleCombatEnd(Trainer winner) {
        System.out.println("[SwingGUI] handleCombatEnd: El combate ha terminado.");
        SwingUtilities.invokeLater(() -> {
            String message;
            if (winner != null) {
                message = "¡" + winner.getName() + " ha ganado el combate!";
            } else {
                message = "El combate ha terminado en empate.";
            }

            if (currentSaveFileName != null && !currentSaveFileName.isEmpty()) {
                File saveFile = new File(currentSaveFileName);
                if (saveFile.exists()) {
                    if (saveFile.delete()) {
                        System.out.println("[SwingGUI] Archivo de guardado " + currentSaveFileName + " eliminado exitosamente.");
                        message += "\nLa partida guardada en la ranura " + (currentSaveSlotIndex + 1) + " ha sido eliminada.";
                    } else {
                        System.err.println("[SwingGUI] Falló al eliminar el archivo de guardado: " + currentSaveFileName);
                        message += "\nNo se pudo eliminar la partida guardada en la ranura " + (currentSaveSlotIndex + 1) + ".";
                    }
                } else {
                     message += "\nNo había un archivo de guardado activo para esta ranura (" + currentSaveFileName + ").";
                }
            } else {
                message += "\nNo había una ranura de guardado activa.";
            }

            JOptionPane.showMessageDialog(this, message, "Combate Terminado", JOptionPane.INFORMATION_MESSAGE);
            clearCurrentSessionState(true);
            if (loadGamePanel != null) {
                loadGamePanel.refreshSaveSlotsDisplay();
            }
            cardLayout.show(mainPanel, "LOAD");
            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    public void signalCombatToContinueAfterLog() {
        System.out.println("[SwingGUI] signalCombatToContinueAfterLog: Señalando a CombatManager para continuar.");
        if (currentCombatManager != null) {
            currentCombatManager.userRequestsToContinueAfterLog();
        } else {
            System.err.println("[SwingGUI] Error: No hay CombatManager activo para señalar continuación del log.");
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwingGUI::new);
    }

    private static class BackgroundPanel extends JPanel {
        private transient ImageIcon backgroundImageIcon;
        private transient Image backgroundImage; 
        private String internalResourceName; 

        public BackgroundPanel(String resourceName) {
            this.internalResourceName = resourceName; 
            String fullResourcePath = resourceName;
            if (resourceName != null && !resourceName.startsWith("/")) {
                fullResourcePath = "/" + resourceName;
            } else if (resourceName == null) {
                System.err.println("[SwingGUI.BackgroundPanel] CRITICAL: resourceName es null.");
                return;
            }


            URL resourceUrl = getClass().getResource(fullResourcePath);

            if (resourceUrl != null) {
                System.out.println("[SwingGUI.BackgroundPanel] Attempting to load background from: " + resourceUrl.toExternalForm());
                try {
                    if (internalResourceName.toLowerCase().endsWith(".gif")) {
                        backgroundImageIcon = new ImageIcon(resourceUrl);
                        // Forzar la carga y verificar errores
                        if (backgroundImageIcon.getImageLoadStatus() == MediaTracker.ERRORED || 
                            backgroundImageIcon.getIconWidth() == -1 || 
                            backgroundImageIcon.getIconHeight() == -1) {
                            System.err.println("[SwingGUI.BackgroundPanel] Error loading GIF (MediaTracker error or invalid dimensions): " + internalResourceName + " from " + resourceUrl);
                            backgroundImageIcon = null; 
                        } else {
                            System.out.println("[SwingGUI.BackgroundPanel] GIF loaded successfully: " + internalResourceName + " (Width: " + backgroundImageIcon.getIconWidth() + ")");
                        }
                    } else { 
                        backgroundImage = ImageIO.read(resourceUrl);
                        if (backgroundImage == null) {
                            System.err.println("[SwingGUI.BackgroundPanel] Error: ImageIO.read returned null for static image: " + internalResourceName + " from " + resourceUrl);
                        } else {
                            System.out.println("[SwingGUI.BackgroundPanel] Static image loaded successfully: " + internalResourceName);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[SwingGUI.BackgroundPanel] IOException while loading image: " + internalResourceName + " from " + resourceUrl + "; " + e.getMessage());
                    backgroundImageIcon = null;
                    backgroundImage = null;
                } catch (Exception e) { 
                    System.err.println("[SwingGUI.BackgroundPanel] Unexpected exception while loading image: " + internalResourceName + " from " + resourceUrl + "; " + e.getMessage());
                    e.printStackTrace();
                    backgroundImageIcon = null;
                    backgroundImage = null;
                }
            } else {
                System.err.println("[SwingGUI.BackgroundPanel] CRITICAL: Background resource NOT FOUND in classpath using path: " + fullResourcePath + " (Original name: " + internalResourceName + ")");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            boolean paintedImage = false;
            if (backgroundImageIcon != null && backgroundImageIcon.getImage() != null && backgroundImageIcon.getIconWidth() > 0) {
                g.drawImage(backgroundImageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                paintedImage = true;
            } else if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                paintedImage = true;
            }
            
            if (!paintedImage) { 
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                Font errorFont = getFont(); 
                SwingGUI pGUI = null;
                if (SwingUtilities.getWindowAncestor(this) instanceof SwingGUI) {
                     pGUI = (SwingGUI) SwingUtilities.getWindowAncestor(this);
                }
                if (pGUI != null && pGUI.getPixelArtFont() != null) {
                    errorFont = pGUI.getPixelArtFont().deriveFont(12f);
                } else if (errorFont == null) { 
                    errorFont = new Font("Monospaced", Font.BOLD, 12);
                }
                g.setFont(errorFont);
                String errorMessage = "Fondo no cargado: " + (internalResourceName != null ? internalResourceName : "Ruta desconocida");
                FontMetrics fm = g.getFontMetrics();
                int y = getHeight() / 2;
                g.drawString(errorMessage, (getWidth() - fm.stringWidth(errorMessage)) / 2, y);
                
                String resourceCheckPath = (internalResourceName != null && internalResourceName.startsWith("/") ? internalResourceName : "/" + internalResourceName);
                if (internalResourceName != null && getClass().getResource(resourceCheckPath) == null ) {
                    String pathMsg = "Recurso no encontrado en: " + resourceCheckPath;
                     g.drawString(pathMsg, (getWidth() - fm.stringWidth(pathMsg)) / 2, y + fm.getHeight() + 5);
                }
            }
        }
    }
}