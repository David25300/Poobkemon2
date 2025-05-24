package PokeBody.domain;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit; // Importar TimeUnit para poll

import PokeBody.Services.CombatManager.PlayerActionChoice; // Importar PlayerActionChoice
import PokeBody.Services.CombatManager.PlayerActionType; // Importar PlayerActionType

/**
 * Interface for selecting a Pokémon's action (move, switch, item).
 * Includes an implementation for GUI interaction using a queue.
 */
public interface MovementSelector {

    /**
     * Selects an action for the active Pokémon.
     *
     * @param activeTrainer   The Trainer making the decision.
     * @param activePokemon   The Trainer's active Pokémon.
     * @param opponentPokemon The opponent's active Pokémon.
     * @return The chosen PlayerActionChoice.
     */
    PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon);

    /**
     * Implementation of MovementSelector that uses a BlockingQueue to receive
     * action choices from a GUI component.
     * UPDATED: Now uses PlayerActionChoice instead of just move index.
     */
    class GUIQueueMovementSelector implements MovementSelector {
        // CORRECCIÓN: Cambiar el tipo de la cola a PlayerActionChoice
        private final BlockingQueue<PlayerActionChoice> actionQueue = new ArrayBlockingQueue<>(1);
        private static final long QUEUE_TIMEOUT_MS = 50; // Tiempo corto para verificar la cola sin bloquear indefinidamente

        public void submitPlayerAction(PlayerActionChoice choice) {
            actionQueue.clear(); // Clear previous pending actions if any
            boolean offered = actionQueue.offer(choice); // Use offer to avoid blocking if queue is full (shouldn't happen with size 1 and clear)
            if (!offered) {
                System.err.println("GUIQueueMovementSelector: No se pudo añadir la acción a la cola (¿llena?).");
            } else {
                 System.out.println("GUIQueueMovementSelector: Acción recibida en la cola: " + choice.type);
            }
        }
        public PlayerActionChoice waitForPlayerAction() throws InterruptedException {
            System.out.println("GUIQueueMovementSelector: Esperando acción del jugador...");
            PlayerActionChoice choice = actionQueue.take(); // Bloquea hasta que haya una acción
            System.out.println("GUIQueueMovementSelector: Acción obtenida: " + choice.type);
            return choice;
       }

        /**
         * Retrieves the PlayerActionChoice submitted by the GUI.
         * Called by CombatManager when it needs the player's input.
         * Includes a short timeout to prevent indefinite blocking if the GUI fails to submit.
         *
         * @param activePokemon   The current active Pokémon (used for fallback).
         * @param opponentPokemon The current opponent Pokémon (unused here but part of the signature).
         * @return The PlayerActionChoice from the queue, or a default fallback action on timeout/interrupt.
         */
        public PlayerActionChoice getPlayerAction(Pokemon activePokemon, Pokemon opponentPokemon) {
            try {
                // Usar poll con timeout para evitar bloqueo indefinido
                PlayerActionChoice choice = actionQueue.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (choice != null) {
                    return choice;
                } else {
                    // Timeout: No action received from GUI in time. Return a default action (e.g., first move).
                    System.err.println("GUIQueueMovementSelector: Timeout esperando acción de la GUI. Seleccionando acción por defecto.");
                    return createDefaultAttackAction(activePokemon);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("GUIQueueMovementSelector interrumpido. Seleccionando acción por defecto.");
                return createDefaultAttackAction(activePokemon);
            }
        }

        /**
         * Creates a default attack action (usually the first usable move or Struggle).
         * Used as a fallback if the GUI doesn't respond.
         *
         * @param pokemon The Pokémon that needs to act.
         * @return A PlayerActionChoice for attacking.
         */
        private PlayerActionChoice createDefaultAttackAction(Pokemon pokemon) {
            PlayerActionChoice defaultChoice = new PlayerActionChoice(PlayerActionType.ATTACK);
            defaultChoice.moveIndex = findFirstViableMoveIndex(pokemon); // Find first usable move or Struggle (index 0)
            return defaultChoice;
        }

        /**
         * Helper to find the index of the first usable move, or 0 (Struggle) if none.
         *
         * @param pokemon The Pokémon whose moves to check.
         * @return The index of the first usable move, or 0.
         */
        private int findFirstViableMoveIndex(Pokemon pokemon) {
            if (pokemon == null || pokemon.getMovimientos() == null || pokemon.getMovimientos().isEmpty()) {
                return 0; // Struggle
            }
            for (int i = 0; i < pokemon.getMovimientos().size(); i++) {
                Movements move = pokemon.getMovimientos().get(i);
                if (move != null && move.puedeUsarse()) {
                    return i;
                }
            }
            return 0; // Struggle if no moves have PP
        }


        /**
         * Implementation of the interface method.
         * This will be called by CombatManager. It waits for the GUI to submit an action
         * via the queue using the getPlayerAction method.
         */
        @Override
        public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
            // CombatManager llamará a getPlayerAction directamente cuando necesite la entrada de la GUI.
            // Este método aquí sirve principalmente para cumplir la interfaz, pero la lógica real
            // de espera y obtención de la acción está en getPlayerAction.
            // Devolvemos una acción por defecto aquí, ya que CombatManager usará getPlayerAction.
            System.out.println("GUIQueueMovementSelector.selectAction llamado (esperando getPlayerAction desde CombatManager).");
            return getPlayerAction(activePokemon, opponentPokemon); // Espera la acción de la cola
        }
    }
}