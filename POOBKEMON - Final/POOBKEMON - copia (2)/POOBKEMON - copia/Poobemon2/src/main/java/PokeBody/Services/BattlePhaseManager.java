// Poobemon2/src/main/java/PokeBody/Services/BattlePhaseManager.java
package PokeBody.Services;

import javax.swing.SwingUtilities;

import Face.SwingGUI;
import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.GameModes.GameMode;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.TurnStartEvent;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer; // Para verificar si se usó Protección
import PokeBody.domain.efectos.ProteccionEfecto; // Para verificar si se usó Protección


public class BattlePhaseManager {
    private final CombatManager combatManager;
    private final Trainer playerTrainer1;
    private final Trainer playerTrainer2;
    private final PlayerInputHandler playerInputHandler;
    private final TurnProcessor turnProcessor;
    private final FaintedPokemonHandler faintedPokemonHandler;
    private final EffectHandler effectHandler;
    private final GameMode activeGameMode; // Sigue siendo útil para onTurnStart/End
    private final BattlefieldState battlefieldState;
    private final EventDispatcher eventDispatcher;
    private final SwingGUI parentUI;
    private int turnNumber = 0;
    private volatile boolean stopLoop = false;
    
    // Para rastrear si se usó Protección/Detección en el turno
    private boolean p1UsedProtectThisTurn = false;
    private boolean p2UsedProtectThisTurn = false;


    public BattlePhaseManager(CombatManager combatManager, Trainer playerTrainer1, Trainer playerTrainer2,
                              PlayerInputHandler playerInputHandler, TurnProcessor turnProcessor,
                              FaintedPokemonHandler faintedPokemonHandler, EffectHandler effectHandler,
                              GameMode activeGameMode, BattlefieldState battlefieldState,
                              EventDispatcher eventDispatcher, SwingGUI parentUI) {
        this.combatManager = combatManager;
        this.playerTrainer1 = playerTrainer1;
        this.playerTrainer2 = playerTrainer2;
        this.playerInputHandler = playerInputHandler;
        this.turnProcessor = turnProcessor;
        this.faintedPokemonHandler = faintedPokemonHandler;
        this.effectHandler = effectHandler;
        this.activeGameMode = activeGameMode;
        this.battlefieldState = battlefieldState;
        this.eventDispatcher = eventDispatcher;
        this.parentUI = parentUI;
    }

    public void signalStop() {
        this.stopLoop = true;
    }

    /**
     * Ejecuta el bucle principal del combate.
     * @param initialTurn El turno desde el cual comenzar o reanudar.
     * @return true si el bucle completó un ciclo sin ser detenido prematuramente por una condición de fin de juego o señal externa.
     * false si el bucle fue detenido. El ganador se determina en CombatManager.
     */
    public boolean executeCombatLoop(int initialTurn) {
        this.turnNumber = initialTurn > 0 ? initialTurn - 1 : 0;
        this.stopLoop = false; // Asegurar que se resetea al inicio de un nuevo bucle de combate

        // La condición principal del bucle ahora se basa en las señales de parada.
        // CombatManager.combatLoopShouldStop se activa cuando el juego realmente termina.
        // this.stopLoop se activa si BattlePhaseManager necesita detenerse (ej. reinicio).
        while (!this.stopLoop && !combatManager.getCombatLoopShouldStop()) {
            // Manejo de pausa
            while (combatManager.isPaused() && !this.stopLoop && !combatManager.getCombatLoopShouldStop()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("[BattlePhaseManager] Hilo de combate interrumpido durante la pausa.");
                    Thread.currentThread().interrupt();
                    this.stopLoop = true; // Señalizar para salir del bucle exterior también
                    break;
                }
            }
            if (this.stopLoop || combatManager.getCombatLoopShouldStop()) break; // Salir si se detuvo durante la pausa

            this.turnNumber++;
            battlefieldState.setTurnCount(this.turnNumber);
            System.out.println("\n[BattlePhaseManager] === INICIO DEL TURNO " + this.turnNumber + " ===");
            eventDispatcher.dispatchEvent(new TurnStartEvent(this.turnNumber));
            
            // GameMode puede tener lógica específica al inicio del turno (ej. TestGameMode para maxTurns)
            if (activeGameMode != null) { // Comprobación de nulidad para activeGameMode
                // activeGameMode.onTurnStart(); // Esta llamada ahora se hace en TestGameMode a través de CombatEventListener
            }
            
            p1UsedProtectThisTurn = false;
            p2UsedProtectThisTurn = false;

            // Fase de Preparación (Standby)
            if (!standbyPhase()) { // standbyPhase ahora verifica combatManager.getCombatLoopShouldStop()
                System.out.println("[BattlePhaseManager] Fin de batalla detectado o parada solicitada en standbyPhase.");
                break;
            }
            // Verificar nuevamente después de la fase, ya que la fase podría haber activado una condición de parada
            if (this.stopLoop || combatManager.getCombatLoopShouldStop()) break;


            // Fase de Batalla
            if (!battlePhase()) { // battlePhase ahora verifica combatManager.getCombatLoopShouldStop()
                System.out.println("[BattlePhaseManager] Fin de batalla detectado o parada solicitada en battlePhase.");
                break;
            }
            if (this.stopLoop || combatManager.getCombatLoopShouldStop()) break;

            // Fase de Fin de Turno
            if (!endPhase()) { // endPhase ahora verifica combatManager.getCombatLoopShouldStop()
                System.out.println("[BattlePhaseManager] Fin de batalla detectado o parada solicitada en endPhase.");
                break;
            }
            if (this.stopLoop || combatManager.getCombatLoopShouldStop()) break;

            // Lógica de espera para que el usuario continúe después de ver el log
            if (!this.stopLoop && !combatManager.getCombatLoopShouldStop()) {
                final CombatManager cm = this.combatManager;
                final SwingGUI currentParentUI = this.parentUI;

                SwingUtilities.invokeLater(() -> {
                    if (currentParentUI != null && currentParentUI.getCombatViewPanel() != null) {
                        currentParentUI.getCombatViewPanel().showLogPanel();
                    }
                });

                cm.setWaitingForUserToContinueAfterLog(true);
                System.out.println("[BattlePhaseManager] Turno " + this.turnNumber + " procesado. Esperando continuación del usuario...");

                while (cm.isWaitingForUserToContinueAfterLog() && !this.stopLoop && !cm.getCombatLoopShouldStop()) {
                    if (cm.isPaused()) {
                        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); this.signalStop(); break; }
                        continue;
                    }
                    try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); this.signalStop(); break; }
                }
                cm.setWaitingForUserToContinueAfterLog(false);

                if (this.stopLoop || cm.getCombatLoopShouldStop()) {
                     System.out.println("[BattlePhaseManager] Espera interrumpida por señal de parada.");
                     break;
                }
                 System.out.println("[BattlePhaseManager] Usuario continuó. Avanzando desde el turno " + this.turnNumber + ".");
            }
            System.out.println("[BattlePhaseManager] === FIN DEL TURNO " + this.turnNumber + " ===");
        } // Fin del bucle while principal

        if (this.stopLoop || combatManager.getCombatLoopShouldStop()) {
            System.out.println("[BattlePhaseManager] Bucle de combate detenido por señal externa o condición de fin.");
            // No es necesario despachar evento de "combate detenido" aquí, CombatManager lo hará si es apropiado.
            return false; // Indica que el bucle fue detenido
        }
        // Si el bucle termina sin una señal de parada, significa que completó su ciclo de vida previsto
        // (aunque esto es menos probable con la lógica de parada ahora en CombatManager).
        // La determinación del ganador real se hace en CombatManager.
        return true; 
    }


    private boolean standbyPhase() {
        System.out.println("[BattlePhaseManager] --- Fase de Preparación (Standby) ---");
        Pokemon activeP1 = battlefieldState.getActivePokemonPlayer1();
        Pokemon activeP2 = battlefieldState.getActivePokemonPlayer2();

        if (activeP1 != null) {
            activeP1.clearRetroceso();
        }
        if (activeP2 != null) {
            activeP2.clearRetroceso();
        }
        
        // La condición de fin de juego es verificada por CombatManager,
        // que a su vez establece combatLoopShouldStop.
        return !combatManager.getCombatLoopShouldStop() && !this.stopLoop;
    }

    private boolean battlePhase() {
        System.out.println("[BattlePhaseManager] --- Fase de Batalla ---");

        Pokemon p1CurrentActiveForInput = battlefieldState.getActivePokemonPlayer1();
        Pokemon p2CurrentActiveForInput = battlefieldState.getActivePokemonPlayer2();

        boolean p1MustSwitch = (p1CurrentActiveForInput == null || p1CurrentActiveForInput.estaDebilitado());
        PlayerActionChoice actionP1 = playerInputHandler.getPlayerAction(
            playerTrainer1,
            combatManager.getPlayer1Selector(),
            p1CurrentActiveForInput,
            p2CurrentActiveForInput,
            p1MustSwitch
        );
        
        if (actionP1 != null && actionP1.type == CombatManager.PlayerActionType.ATTACK && actionP1.moveIndex != -1 &&
            p1CurrentActiveForInput != null && p1CurrentActiveForInput.getMovimientos() != null && 
            actionP1.moveIndex < p1CurrentActiveForInput.getMovimientos().size() && 
            p1CurrentActiveForInput.getMovimientos().get(actionP1.moveIndex) != null) { // Chequeo adicional de nulidad
            Movements moveP1 = p1CurrentActiveForInput.getMovimientos().get(actionP1.moveIndex);
            if (moveP1.getEfecto() instanceof ProteccionEfecto) {
                p1UsedProtectThisTurn = true;
            }
        }

        if (this.stopLoop || combatManager.getCombatLoopShouldStop()) return false;
        // Si p1 tuvo que cambiar, FaintedPokemonHandler ya manejó el cambio y actualizó battlefieldState.
        // Si el equipo de p1 fue derrotado, combatLoopShouldStop ya estaría true.
        
        // Refrescar referencias en caso de que p1 haya cambiado
        p1CurrentActiveForInput = battlefieldState.getActivePokemonPlayer1();
        
        boolean p2MustSwitch = (p2CurrentActiveForInput == null || p2CurrentActiveForInput.estaDebilitado());
         // Si P2 ya está KO por alguna razón antes de su acción (ej. efecto de fin de turno anterior que KO al nuevo P2)
        if (p2CurrentActiveForInput != null && p2CurrentActiveForInput.estaDebilitado()){
            p2MustSwitch = true;
        }

        PlayerActionChoice actionP2 = playerInputHandler.getPlayerAction(
            playerTrainer2,
            combatManager.getPlayer2Selector(),
            p2CurrentActiveForInput, // Puede ser null o KO si P1 lo debilitó y P2 no tuvo chance de cambiar
            p1CurrentActiveForInput, // El Pokémon activo de P1 después de su acción/cambio
            p2MustSwitch
        );
        
         if (actionP2 != null && actionP2.type == CombatManager.PlayerActionType.ATTACK && actionP2.moveIndex != -1 &&
            p2CurrentActiveForInput != null && !p2CurrentActiveForInput.estaDebilitado() && // Asegurar que P2 no esté KO antes de acceder a movimientos
            p2CurrentActiveForInput.getMovimientos() != null && 
            actionP2.moveIndex < p2CurrentActiveForInput.getMovimientos().size() &&
            p2CurrentActiveForInput.getMovimientos().get(actionP2.moveIndex) != null) { // Chequeo adicional de nulidad
            Movements moveP2 = p2CurrentActiveForInput.getMovimientos().get(actionP2.moveIndex);
            if (moveP2.getEfecto() instanceof ProteccionEfecto) {
                p2UsedProtectThisTurn = true;
            }
        }

        if (this.stopLoop || combatManager.getCombatLoopShouldStop()) return false;
        // Similar a P1, si P2 tuvo que cambiar, FaintedPokemonHandler lo manejó.

        return turnProcessor.processTurnActions(actionP1, playerTrainer1, actionP2, playerTrainer2);
    }

    private boolean endPhase() {
        System.out.println("[BattlePhaseManager] --- Fase de Fin de Turno ---");
        Pokemon p1 = battlefieldState.getActivePokemonPlayer1();
        Pokemon p2 = battlefieldState.getActivePokemonPlayer2();

        if (p1 != null && !p1.estaDebilitado()) {
            effectHandler.aplicarEfectosPersistentesDeFinDeTurno(p1, battlefieldState, eventDispatcher);
            if (p1.estaDebilitado()) {
                // FaintedPokemonHandler devuelve true si el entrenador ha perdido (no puede continuar)
                if (faintedPokemonHandler.handleFaintedPokemonAfterAction(playerTrainer1)) {
                    combatManager.signalCombatLoopToStop(); // Señalizar fin de combate
                    return false; // El combate terminó
                }
            }
        }
        if (this.stopLoop || combatManager.getCombatLoopShouldStop()) return false;

        if (p2 != null && !p2.estaDebilitado()) {
            effectHandler.aplicarEfectosPersistentesDeFinDeTurno(p2, battlefieldState, eventDispatcher);
            if (p2.estaDebilitado()) {
                if (faintedPokemonHandler.handleFaintedPokemonAfterAction(playerTrainer2)) {
                    combatManager.signalCombatLoopToStop(); // Señalizar fin de combate
                    return false; // El combate terminó
                }
            }
        }
        if (this.stopLoop || combatManager.getCombatLoopShouldStop()) return false;

        if (p1 != null && !p1.estaDebilitado()) {
            p1.actualizarDuracionEstado();
            p1.resetProtectionStateForEndOfTurn();
            if (!p1UsedProtectThisTurn) {
                p1.resetConsecutiveProtectUses();
            }
        }
        if (p2 != null && !p2.estaDebilitado()) {
            p2.actualizarDuracionEstado();
            p2.resetProtectionStateForEndOfTurn();
             if (!p2UsedProtectThisTurn) {
                p2.resetConsecutiveProtectUses();
            }
        }

        if (activeGameMode != null) { 
        }

        if (parentUI != null) {
            parentUI.requestAutoSave(playerTrainer1, playerTrainer2, this.turnNumber);
        }
        return !combatManager.getCombatLoopShouldStop() && !this.stopLoop;
    }
}