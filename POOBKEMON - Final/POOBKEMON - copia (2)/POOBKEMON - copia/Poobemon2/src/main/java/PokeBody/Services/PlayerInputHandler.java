package PokeBody.Services;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import Face.SwingGUI;
import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class PlayerInputHandler {
    private ExecutorService inputExecutor; // No final para poder reiniciarlo
    private final int inputTimeoutSeconds;
    private final EventDispatcher eventDispatcher;
    private final SwingGUI parentUI;
    private final CombatManager combatManager; // Referencia a CombatManager para el estado de pausa

    public PlayerInputHandler(ExecutorService inputExecutor, int inputTimeoutSeconds, EventDispatcher eventDispatcher, SwingGUI parentUI, CombatManager combatManager) {
        if (inputExecutor == null || eventDispatcher == null || parentUI == null || combatManager == null) {
            throw new IllegalArgumentException("Dependencias de PlayerInputHandler no pueden ser nulas.");
        }
        this.inputExecutor = inputExecutor;
        this.inputTimeoutSeconds = inputTimeoutSeconds;
        this.eventDispatcher = eventDispatcher;
        this.parentUI = parentUI;
        this.combatManager = combatManager;
    }

    public void setInputExecutor(ExecutorService executor) { // Para actualizar después de reinicio
        this.inputExecutor = executor;
    }


    public PlayerActionChoice getPlayerAction(Trainer trainer, MovementSelector selector, Pokemon activePokemon, Pokemon opponentPokemon, boolean isMandatorySwitch) {
        PlayerActionChoice action = null; // Inicializar a null
        boolean isHumanPlayer = selector instanceof MovementSelector.GUIQueueMovementSelector;
        String trainerDisplayName = trainer.getName() + (isHumanPlayer ? " (Jugador)" : " (IA)");

        System.out.println("[PlayerInputHandler] Solicitando acción de: " + trainerDisplayName +
                           ", Pokémon activo: " + (activePokemon != null ? activePokemon.getNombre() : "NINGUNO") +
                           ", Cambio Obligatorio: " + isMandatorySwitch);

        // Bucle para manejar la pausa mientras se espera la entrada del jugador
        while (action == null && !combatManager.getCombatLoopShouldStop()) {
            if (combatManager.isPaused()) {
                // Si está pausado, el hilo de PlayerInputHandler (que es el hilo de combate)
                // simplemente espera. El temporizador de UI ya está detenido.
                try {
                    Thread.sleep(200); // Esperar un poco para no consumir CPU
                    continue; // Volver a verificar el estado de pausa
                } catch (InterruptedException e) {
                    System.out.println("[PlayerInputHandler] Hilo interrumpido durante la pausa mientras esperaba acción.");
                    Thread.currentThread().interrupt();
                    return createDefaultPassAction(); // O manejar de otra forma la interrupción
                }
            }

            // --- Lógica original de obtención de acción ---
            if (isMandatorySwitch) {
                eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " debe cambiar Pokémon."));
                System.out.println("[PlayerInputHandler] " + trainer.getName() + " debe cambiar (obligatorio).");

                if (isHumanPlayer) {
                    final Trainer trainerFinal = trainer;
                    final Pokemon currentPokemonFinal = activePokemon;
                    SwingUtilities.invokeLater(() -> {
                        parentUI.showPokemonSwitchUI(trainerFinal, currentPokemonFinal, true);
                    });
                    System.out.println("[PlayerInputHandler] Esperando acción de cambio OBLIGATORIO de GUI para " + trainerDisplayName);
                    Future<PlayerActionChoice> futureAction = inputExecutor.submit(((MovementSelector.GUIQueueMovementSelector) selector)::waitForPlayerAction);
                    try {
                        action = futureAction.get(inputTimeoutSeconds * 2, TimeUnit.SECONDS); // Mayor timeout
                        System.out.println("[PlayerInputHandler] Acción de cambio OBLIGATORIO recibida de GUI para " + trainerDisplayName + ": " + action);
                        if (!isValidSwitchChoice(action, trainer, activePokemon, true)) {
                            System.err.println("[PlayerInputHandler] Jugador no seleccionó un cambio válido para Pokémon KO o acción fue null/inválida. Forzando primer disponible.");
                            action = createForcedSwitchAction(trainer);
                        }
                    } catch (TimeoutException e) {
                        eventDispatcher.dispatchEvent(new MessageEvent(trainerDisplayName + " tardó demasiado en cambiar. Seleccionando primer Pokémon disponible."));
                        System.out.println("[PlayerInputHandler] Timeout esperando cambio OBLIGATORIO de GUI para " + trainerDisplayName + ". Forzando.");
                        action = createForcedSwitchAction(trainer);
                        futureAction.cancel(true);
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("[PlayerInputHandler] Error esperando cambio OBLIGATORIO de GUI para " + trainer.getName() + ": " + e.getMessage());
                        action = createForcedSwitchAction(trainer);
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    }
                } else {
                    action = selector.selectAction(trainer, activePokemon, opponentPokemon);
                    if (!isValidSwitchChoice(action, trainer, activePokemon, true)) {
                         System.out.println("[PlayerInputHandler] ADVERTENCIA: IA Selector de " + trainer.getName() +
                                           " no eligió un cambio válido (obligatorio) o devolvió null. Forzando búsqueda de cambio.");
                         action = createForcedSwitchAction(trainer);
                    }
                }
                System.out.println("[PlayerInputHandler] Acción de cambio OBLIGATORIO para " + trainer.getName() + ": " + action);
                return action; // Salir del bucle de pausa después de obtener la acción obligatoria
            }

            if (activePokemon == null || activePokemon.estaDebilitado()){
                System.err.println("[PlayerInputHandler] ERROR INESPERADO: Pokémon activo KO pero isMandatorySwitch es false para " + trainer.getName());
                action = createForcedSwitchAction(trainer);
                return action; // Salir del bucle de pausa
            }

            if (isHumanPlayer) {
                MovementSelector.GUIQueueMovementSelector currentGuiSelector = (MovementSelector.GUIQueueMovementSelector) selector;
                if (parentUI.getCombatViewPanel() != null) {
                    parentUI.getCombatViewPanel().setActivePlayerTurnForInput(trainer == parentUI.getCurrentPlayer1TrainerState());
                }
                eventDispatcher.dispatchEvent(new MessageEvent("Esperando acción de " + trainerDisplayName + "..."));
                System.out.println("[PlayerInputHandler] Esperando acción de GUI para " + trainerDisplayName);
                Future<PlayerActionChoice> futureAction = inputExecutor.submit(currentGuiSelector::waitForPlayerAction);
                try {
                    action = futureAction.get(inputTimeoutSeconds, TimeUnit.SECONDS);
                    System.out.println("[PlayerInputHandler] Acción recibida de GUI para " + trainerDisplayName + ": " + action);
                    if (action == null) {
                        System.err.println("[PlayerInputHandler] Acción recibida de GUI fue NULL para " + trainerDisplayName + ". Usando acción por defecto.");
                        action = createDefaultAttackAction(activePokemon);
                    } else if (action.type == PlayerActionType.ATTACK && !isAttackActionValid(activePokemon, action.moveIndex)) {
                        eventDispatcher.dispatchEvent(new MessageEvent("Movimiento inválido o sin PP para " + activePokemon.getNombre() + ". Usando Forcejeo."));
                        System.out.println("[PlayerInputHandler] Movimiento inválido de GUI, usando Forcejeo para " + activePokemon.getNombre());
                        action.moveIndex = findViableMoveIndex(activePokemon);
                    } else if (action.type == PlayerActionType.SWITCH_POKEMON && !isValidSwitchChoice(action, trainer, activePokemon, false)) {
                        eventDispatcher.dispatchEvent(new MessageEvent("Intento de cambio inválido por " + trainer.getName() + ". Se pierde el turno."));
                        System.out.println("[PlayerInputHandler] Intento de cambio voluntario inválido por GUI. Se pierde turno.");
                        action = createDefaultPassAction();
                    }
                } catch (TimeoutException e) {
                    eventDispatcher.dispatchEvent(new MessageEvent(trainerDisplayName + " tardó demasiado. Usando acción por defecto."));
                    System.out.println("[PlayerInputHandler] Timeout para " + trainerDisplayName + ". Usando acción por defecto.");
                    action = createDefaultAttackAction(activePokemon);
                    futureAction.cancel(true);
                } catch (InterruptedException | ExecutionException e) {
                    eventDispatcher.dispatchEvent(new MessageEvent("Error esperando acción de " + trainerDisplayName + ". Usando acción por defecto."));
                    System.err.println("[PlayerInputHandler] Error para " + trainer.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    action = createDefaultAttackAction(activePokemon);
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            } else { // Es una IA
                eventDispatcher.dispatchEvent(new MessageEvent(trainerDisplayName + " está decidiendo..."));
                System.out.println("[PlayerInputHandler] Solicitando acción de IA para " + trainerDisplayName);
                action = selector.selectAction(trainer, activePokemon, opponentPokemon);
                System.out.println("[PlayerInputHandler] Acción recibida de IA para " + trainerDisplayName + ": " + action);
                if (action == null) {
                    System.err.println("[PlayerInputHandler] Acción recibida de IA fue NULL para " + trainerDisplayName + ". Usando acción por defecto.");
                    action = createDefaultAttackAction(activePokemon);
                } else if (action.type == PlayerActionType.ATTACK && !isAttackActionValid(activePokemon, action.moveIndex)) {
                     eventDispatcher.dispatchEvent(new MessageEvent("IA intentó un movimiento inválido para " + activePokemon.getNombre() + ". Usando Forcejeo."));
                     System.out.println("[PlayerInputHandler] Movimiento inválido de IA, usando Forcejeo para " + activePokemon.getNombre());
                     action.moveIndex = findViableMoveIndex(activePokemon);
                } else if (action.type == PlayerActionType.SWITCH_POKEMON && !isValidSwitchChoice(action, trainer, activePokemon, false)) {
                     eventDispatcher.dispatchEvent(new MessageEvent("IA intentó un cambio inválido. Atacando en su lugar."));
                     System.out.println("[PlayerInputHandler] Intento de cambio voluntario inválido por IA. Atacando en su lugar.");
                     action = createDefaultAttackAction(activePokemon);
                }
            }
             if (combatManager.getCombatLoopShouldStop()) action = createDefaultPassAction(); // Si se detiene, pasar
        } // Fin del bucle while de pausa

        return action != null ? action : createDefaultPassAction(); // Devolver acción o pasar si se interrumpió
    }

    private boolean isValidSwitchChoice(PlayerActionChoice action, Trainer trainer, Pokemon currentActive, boolean isMandatory) {
        if (action == null || action.type != PlayerActionType.SWITCH_POKEMON || action.switchToPokemonIndex == -1) {
            return false;
        }
        if (action.switchToPokemonIndex < 0 || action.switchToPokemonIndex >= trainer.getteam().size()) {
            return false;
        }
        Pokemon targetSwitch = trainer.getteam().get(action.switchToPokemonIndex);
        if (targetSwitch == null || targetSwitch.estaDebilitado()) {
            return false;
        }
        if (!isMandatory && targetSwitch == currentActive) {
            return false;
        }
        return true;
    }

    private PlayerActionChoice createForcedSwitchAction(Trainer trainer) {
        PlayerActionChoice action = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
        action.switchToPokemonIndex = findFirstAvailableSwitchIndex(trainer);
        if (action.switchToPokemonIndex == -1) {
            System.err.println("[PlayerInputHandler] No hay Pokémon para cambio forzado para " + trainer.getName() + ".");
            return createDefaultPassAction();
        }
        return action;
    }


    private PlayerActionChoice createDefaultAttackAction(Pokemon pokemon) {
        PlayerActionChoice defaultChoice = new PlayerActionChoice(PlayerActionType.ATTACK);
        defaultChoice.moveIndex = findViableMoveIndex(pokemon);
        System.out.println("[PlayerInputHandler createDefaultAttackAction] Creando acción de ataque por defecto para " +
                           (pokemon != null ? pokemon.getNombre() : "null") + ". Índice de movimiento: " + defaultChoice.moveIndex);
        return defaultChoice;
    }

    private PlayerActionChoice createDefaultPassAction() {
        PlayerActionChoice pass = new PlayerActionChoice(PlayerActionType.ATTACK);
        pass.moveIndex = -1;
        System.out.println("[PlayerInputHandler] Creando acción de PASE por defecto (índice de movimiento -1).");
        return pass;
    }

    private int findFirstAvailableSwitchIndex(Trainer trainer) {
        if (trainer == null || trainer.getteam() == null) return -1;
        List<Pokemon> team = trainer.getteam();
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i) != null && !team.get(i).estaDebilitado()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isAttackActionValid(Pokemon attacker, int moveIndex) {
        if (attacker == null || attacker.getMovimientos() == null || attacker.getMovimientos().isEmpty()) {
            return false;
        }
        if (moveIndex < 0 || moveIndex >= attacker.getMovimientos().size()) {
            return false;
        }
        Movements move = attacker.getMovimientos().get(moveIndex);
        return move != null && move.puedeUsarse();
    }

    private int findViableMoveIndex(Pokemon pokemon) {
        if (pokemon == null || pokemon.getMovimientos() == null || pokemon.getMovimientos().isEmpty()) return 0;
        for (int i = 0; i < pokemon.getMovimientos().size(); i++) {
            Movements move = pokemon.getMovimientos().get(i);
            if (move != null && move.puedeUsarse()) return i;
        }
        return 0;
    }
}