package PokeBody.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Necesario para allGameMoves
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import Face.SwingGUI;
import PokeBody.Services.GameModes.GameMode;
import PokeBody.Services.GameModes.SurvivalGameMode; // Importar para instanceof
import PokeBody.Services.events.BattleEndEvent;
import PokeBody.Services.events.BattleStartEvent;
import PokeBody.Services.events.CombatEventListener;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Item;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements; // Necesario para allGameMoves
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;


public class CombatManager {

    public enum PlayerActionType { ATTACK, SWITCH_POKEMON, USE_ITEM, RUN }

    public static class PlayerActionChoice {
        public PlayerActionType type;
        public int moveIndex = -1;
        public int switchToPokemonIndex = -1;
        public Item itemToUse = null;
        public Pokemon itemTargetPokemon = null;
        public List<Integer> targetIndices;

        public PlayerActionChoice(PlayerActionType type) {
            this.type = type;
            this.targetIndices = new ArrayList<>();
        }
        @Override
        public String toString() {
            return "PlayerActionChoice{" +
                    "type=" + type +
                    (type == PlayerActionType.ATTACK ? ", moveIndex=" + moveIndex : "") +
                    (type == PlayerActionType.SWITCH_POKEMON ? ", switchToPokemonIndex=" + switchToPokemonIndex : "") +
                    (type == PlayerActionType.USE_ITEM ? ", itemToUse=" + (itemToUse != null ? itemToUse.getNombre() : "null") +
                                                       ", itemTargetPokemon=" + (itemTargetPokemon != null ? itemTargetPokemon.getNombre() : "N/A") : "") +
                    '}';
        }
    }

    private final Trainer playerTrainer1;
    private final Trainer playerTrainer2;
    private final MovementSelector player1Selector;
    private final MovementSelector player2Selector;
    private final TeamManager teamManager;
    private final EventDispatcher eventDispatcher;
    private final BattlefieldState battlefieldState;
    private final GameMode activeGameMode;
    private final SwingGUI parentUI;
    private ExecutorService inputExecutor;
    private Thread combatThread;

    private final PlayerInputHandler playerInputHandler;
    private final PokemonSwitchService pokemonSwitchService;
    private final ItemUsageService itemUsageService;
    private final FaintedPokemonHandler faintedPokemonHandler;
    private final TurnProcessor turnProcessor;
    private BattlePhaseManager battlePhaseManager;
    private final ActionExecutor actionExecutor;
    private final EffectHandler effectHandler;
    private final DamageCalculator damageCalculator;
    private final Map<String, Movements> allGameMoves;

    private volatile boolean combatLoopShouldStop = false;
    private volatile boolean isPaused = false;
    private volatile boolean playerRanAway = false;
    private volatile boolean waitingForUserToContinueAfterLog = false;


    public CombatManager(Trainer playerTrainer1, Trainer playerTrainer2,
                         MovementSelector selectorP1, MovementSelector selectorP2,
                         CombatEventListener eventListener, SwingGUI parentUI, GameMode gameMode) {

        if (playerTrainer1 == null || playerTrainer2 == null || selectorP1 == null || selectorP2 == null ||
            eventListener == null || parentUI == null || gameMode == null) {
            throw new IllegalArgumentException("Los argumentos del constructor de CombatManager no pueden ser nulos.");
        }
        this.playerTrainer1 = playerTrainer1;
        this.playerTrainer2 = playerTrainer2;
        this.player1Selector = selectorP1;
        this.player2Selector = selectorP2;
        this.parentUI = parentUI;
        this.activeGameMode = gameMode;
        
        this.allGameMoves = parentUI.getAllAvailableMoves();
        if (this.allGameMoves == null) {
            throw new IllegalStateException("CombatManager: allGameMoves no pudo ser obtenido de parentUI (es null).");
        }

        this.eventDispatcher = new EventDispatcher();
        this.eventDispatcher.registerListener(eventListener);
        // Si GameMode implementa CombatEventListener y necesita ser notificado (ej. TestGameMode)
        if (activeGameMode instanceof CombatEventListener) {
            this.eventDispatcher.registerListener((CombatEventListener) activeGameMode);
        }


        this.teamManager = new TeamManager(playerTrainer1.getteam(), playerTrainer2.getteam());
        Pokemon p1Initial = teamManager.getPokemonActivo(TeamManager.Equipo.JUGADOR);
        Pokemon p2Initial = teamManager.getPokemonActivo(TeamManager.Equipo.RIVAL);

        if (p1Initial == null || p2Initial == null) {
            throw new IllegalStateException("Error: Uno o ambos entrenadores no tienen Pokémon disponibles al crear CombatManager.");
        }

        this.battlefieldState = new BattlefieldState(playerTrainer1, playerTrainer2, p1Initial, p2Initial);
        this.inputExecutor = Executors.newSingleThreadExecutor();
        this.damageCalculator = new DamageCalculator();
        this.effectHandler = new EffectHandler();

        this.playerInputHandler = new PlayerInputHandler(this.inputExecutor, 30, this.eventDispatcher, this.parentUI, this);
        this.pokemonSwitchService = new PokemonSwitchService(this.battlefieldState, this.teamManager, this.eventDispatcher);
        this.itemUsageService = new ItemUsageService(this.battlefieldState, this.eventDispatcher);

        this.faintedPokemonHandler = new FaintedPokemonHandler(
            this.battlefieldState, this.teamManager, this.playerInputHandler,
            this.pokemonSwitchService, this.eventDispatcher, this.activeGameMode,
            this
        );

        this.actionExecutor = new ActionExecutor(
            this.battlefieldState, this.damageCalculator, this.effectHandler,
            this.eventDispatcher, this.allGameMoves
        );

        this.turnProcessor = new TurnProcessor(
            this.actionExecutor, this.pokemonSwitchService, this.itemUsageService,
            this.battlefieldState, this.eventDispatcher, this.faintedPokemonHandler
        );

        this.battlePhaseManager = new BattlePhaseManager(
            this,
            this.playerTrainer1, this.playerTrainer2, this.playerInputHandler,
            this.turnProcessor, this.faintedPokemonHandler, this.effectHandler,
            this.activeGameMode, this.battlefieldState, this.eventDispatcher, this.parentUI
        );
        
        // Inicializar el GameMode con las dependencias necesarias
        // Esto es crucial para que GameMode (ej. SurvivalGameMode) pueda interactuar con el CombatManager.
        activeGameMode.initialize(playerTrainer1, this, eventDispatcher, battlefieldState);


        System.out.println("[CombatManager Constructor] P1 ("+playerTrainer1.getName()+") Pokémon activo: " + p1Initial.getNombre());
        System.out.println("[CombatManager Constructor] P2 ("+playerTrainer2.getName()+") Pokémon activo: " + p2Initial.getNombre());
    }

    public MovementSelector getPlayer1Selector() { return player1Selector; }
    public MovementSelector getPlayer2Selector() { return player2Selector; }
    public BattlefieldState getBattlefieldState() { return this.battlefieldState; }
    public boolean getCombatLoopShouldStop() { return combatLoopShouldStop; }
    public boolean isPaused() { return isPaused; }
    public TurnProcessor getTurnProcessor() { return turnProcessor; }


    public void setPaused(boolean paused) {
        System.out.println("[CombatManager] setPaused: " + paused);
        this.isPaused = paused;
    }

    public void playerAttemptedToRun() {
        System.out.println("[CombatManager] playerAttemptedToRun: El jugador intenta huir.");
        this.playerRanAway = true;
        this.signalCombatLoopToStop();
    }

    public boolean isWaitingForUserToContinueAfterLog() { return this.waitingForUserToContinueAfterLog; }
    public void setWaitingForUserToContinueAfterLog(boolean waiting) { this.waitingForUserToContinueAfterLog = waiting; }
    public void userRequestsToContinueAfterLog() {
        System.out.println("[CombatManager] userRequestsToContinueAfterLog: Petición recibida para continuar.");
        this.waitingForUserToContinueAfterLog = false;
    }

    public void run(int initialTurn) {
        System.out.println("[CombatManager run] Iniciando combate. Turno inicial efectivo: " + initialTurn + ". Estado Pausa inicial (CM): " + this.isPaused);
        this.isPaused = false;
        this.combatLoopShouldStop = false;
        this.playerRanAway = false;
        this.waitingForUserToContinueAfterLog = false;


        if (initialTurn <= 1) {
            teamManager.resetearEquiposParaNuevoCombate();
            Pokemon p1ActiveAfterReset = teamManager.getPokemonActivo(TeamManager.Equipo.JUGADOR);
            Pokemon p2ActiveAfterReset = teamManager.getPokemonActivo(TeamManager.Equipo.RIVAL);

            if (p1ActiveAfterReset == null || p2ActiveAfterReset == null) {
                eventDispatcher.dispatchEvent(new MessageEvent("Error crítico: No hay Pokémon activos después del reseteo del equipo."));
                shutdownExecutor();
                parentUI.handleCombatEnd(null);
                return;
            }
            battlefieldState.setActivePokemonPlayer1(p1ActiveAfterReset);
            battlefieldState.setActivePokemonPlayer2(p2ActiveAfterReset);
            System.out.println("[CombatManager run] Equipos reseteados. P1: " + p1ActiveAfterReset.getNombre() + ", P2: " + p2ActiveAfterReset.getNombre());
        }

        if (battlefieldState.getActivePokemonPlayer1() == null || battlefieldState.getActivePokemonPlayer2() == null) {
            eventDispatcher.dispatchEvent(new MessageEvent("Error: Uno o ambos entrenadores no tienen Pokémon disponibles al iniciar."));
            Trainer winner = (battlefieldState.getActivePokemonPlayer1() != null) ? playerTrainer1 : (battlefieldState.getActivePokemonPlayer2() != null ? playerTrainer2 : null);
            eventDispatcher.dispatchEvent(new BattleEndEvent(winner, (winner == playerTrainer1) ? playerTrainer2 : playerTrainer1));
            shutdownExecutor();
            parentUI.handleCombatEnd(winner);
            return;
        }

        if (initialTurn <= 1) {
            eventDispatcher.dispatchEvent(new BattleStartEvent(playerTrainer1, playerTrainer2));
            eventDispatcher.dispatchEvent(new MessageEvent(playerTrainer1.getName() + " saca a " + battlefieldState.getActivePokemonPlayer1().getNombre() + "!"));
            eventDispatcher.dispatchEvent(new MessageEvent(playerTrainer2.getName() + " saca a " + battlefieldState.getActivePokemonPlayer2().getNombre() + "!"));
        } else {
            eventDispatcher.dispatchEvent(new MessageEvent("Combate reanudado desde el turno " + initialTurn));
        }

        combatThread = new Thread(() -> {
            boolean combatEndedNaturally = false;
            Trainer winner = null; 
            Trainer loser = null;  

            try {
                battlePhaseManager.executeCombatLoop(initialTurn);
                
                if (!this.combatLoopShouldStop && !this.playerRanAway) {
                    combatEndedNaturally = true;
                }

            } catch (Exception e) {
                if (!this.playerRanAway && !this.combatLoopShouldStop) {
                    eventDispatcher.dispatchEvent(new MessageEvent("¡Error inesperado durante el combate!"));
                    System.err.println("[CombatManager] Error en CombatManager.run() -> battlePhaseManager.executeCombatLoop(): ");
                    e.printStackTrace();
                    combatEndedNaturally = true; 
                } else {
                    System.out.println("[CombatManager] Excepción durante el bucle de combate, pero se detectó señal de parada/huida.");
                }
            } finally {
                if (this.playerRanAway) {
                    System.out.println("[CombatManager] Combate terminado por huida del jugador.");
                    SwingUtilities.invokeLater(() -> {
                        parentUI.handleRunFromCombat();
                    });
                    shutdownExecutor();
                } else if (combatEndedNaturally) {
                    boolean p1Defeated = teamManager.estaEquipoDerrotado(TeamManager.Equipo.JUGADOR);
                    boolean p2Defeated = teamManager.estaEquipoDerrotado(TeamManager.Equipo.RIVAL);
                    boolean callHandleCombatEnd = true;

                    if (p1Defeated && !p2Defeated) {
                        winner = playerTrainer2; loser = playerTrainer1;
                        activeGameMode.onPlayerLoseBattle(playerTrainer1, playerTrainer2);
                    } else if (!p1Defeated && p2Defeated) {
                        winner = playerTrainer1; loser = playerTrainer2;
                        activeGameMode.onPlayerWinBattle(playerTrainer1, playerTrainer2);
                        if (activeGameMode instanceof SurvivalGameMode) {
                            // En Supervivencia, si el jugador gana la ronda, NO se llama a handleCombatEnd aquí.
                            // SurvivalGameMode.onPlayerWinBattle -> iniciarSiguienteBatalla -> parentUI.proceedToCombat
                            // iniciará un nuevo CombatManager para la siguiente ronda.
                            callHandleCombatEnd = false;
                        }
                    } else { 
                        activeGameMode.onPlayerDraw(playerTrainer1, playerTrainer2);
                    }

                    if (callHandleCombatEnd) {
                        eventDispatcher.dispatchEvent(new MessageEvent("--------------------"));
                        if (winner != null) {
                             eventDispatcher.dispatchEvent(new MessageEvent("¡" + winner.getName() + " ha ganado la batalla!"));
                             System.out.println("[CombatManager] Ganador final determinado: " + winner.getName());
                        } else {
                             eventDispatcher.dispatchEvent(new MessageEvent("¡La batalla ha terminado en empate o sin un ganador claro!"));
                             System.out.println("[CombatManager] Empate final o sin ganador claro.");
                        }
                        eventDispatcher.dispatchEvent(new BattleEndEvent(winner, loser));
                        final Trainer finalWinner = winner;
                        SwingUtilities.invokeLater(() -> {
                            parentUI.handleCombatEnd(finalWinner);
                        });
                    }
                    shutdownExecutor();
                } else {
                    System.out.println("[CombatManager] Bucle de combate detenido para reinicio o por señal externa, no se procesa fin de batalla normal ni huida.");
                }
            }
        });
        combatThread.setName("CombatThread-" + System.currentTimeMillis());
        combatThread.start();
    }

    public void run() {
        run(1);
    }

    private void shutdownExecutor() {
        if (inputExecutor != null && !inputExecutor.isShutdown()) {
            System.out.println("[CombatManager] Intentando apagar inputExecutor...");
            inputExecutor.shutdownNow();
            try {
                if (!inputExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("[CombatManager] El ExecutorService de entrada no terminó a tiempo.");
                } else {
                    System.out.println("[CombatManager] inputExecutor apagado exitosamente.");
                }
            } catch (InterruptedException e) {
                System.err.println("[CombatManager] Interrupción esperando que el ExecutorService de entrada termine.");
                Thread.currentThread().interrupt();
            }
        }
    }

    public int getTurnCount() {
        return battlefieldState.getTurnCount();
    }

    public void signalCombatLoopToStop() {
        this.combatLoopShouldStop = true;
        if (battlePhaseManager != null) {
            battlePhaseManager.signalStop();
        }
        if (combatThread != null && combatThread.isAlive()) {
            combatThread.interrupt();
            System.out.println("[CombatManager] Hilo de combate interrumpido para detener el bucle.");
        }
    }

    public boolean restartCombat() {
        System.out.println("[CombatManager] Iniciando proceso de reinicio de combate...");
        this.playerRanAway = false;
        signalCombatLoopToStop();

        if (combatThread != null && combatThread.isAlive()) {
            try {
                System.out.println("[CombatManager] Esperando a que el hilo de combate actual termine...");
                combatThread.join(2000);
                if (combatThread.isAlive()) {
                    System.err.println("[CombatManager] El hilo de combate no terminó después de la señal de parada y join. Forzando continuación del reinicio.");
                } else {
                    System.out.println("[CombatManager] Hilo de combate actual terminado.");
                }
            } catch (InterruptedException e) {
                System.err.println("[CombatManager] Interrupción mientras se esperaba que el hilo de combate terminara: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        combatThread = null;

        shutdownExecutor();
        inputExecutor = Executors.newSingleThreadExecutor();
        playerInputHandler.setInputExecutor(inputExecutor);

        teamManager.resetearEquiposParaNuevoCombate();
        battlefieldState.setActivePokemonPlayer1(teamManager.getPokemonActivo(TeamManager.Equipo.JUGADOR));
        battlefieldState.setActivePokemonPlayer2(teamManager.getPokemonActivo(TeamManager.Equipo.RIVAL));
        battlefieldState.resetTurnCount();
        isPaused = false;
        waitingForUserToContinueAfterLog = false;


        if (parentUI != null && parentUI.getCombatViewPanel() != null) {
            parentUI.getCombatViewPanel().clearCombatLog();
            parentUI.getCombatViewPanel().setActivePokemonAndTrainers(
                battlefieldState.getActivePokemonPlayer1(), playerTrainer1,
                battlefieldState.getActivePokemonPlayer2(), playerTrainer2
            );
            parentUI.getCombatViewPanel().updatePauseButtonText(false);
            parentUI.getCombatViewPanel().updateActionPromptLabel();
        }
        
        this.battlePhaseManager = new BattlePhaseManager(
            this, this.playerTrainer1, this.playerTrainer2, this.playerInputHandler,
            this.turnProcessor, this.faintedPokemonHandler, this.effectHandler,
            this.activeGameMode, this.battlefieldState, this.eventDispatcher, this.parentUI
        );
        
        // Re-inicializar el GameMode con el nuevo CombatManager (o el estado reseteado)
        activeGameMode.initialize(playerTrainer1, this, eventDispatcher, battlefieldState);


        System.out.println("[CombatManager] Estado del juego reseteado. Reiniciando combate...");
        run(1);
        return true;
    }
}