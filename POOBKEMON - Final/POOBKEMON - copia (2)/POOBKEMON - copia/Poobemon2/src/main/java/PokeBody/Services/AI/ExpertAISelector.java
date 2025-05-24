// Archivo: PokeBody/Services/AI/ExpertAISelector.java
package PokeBody.Services.AI;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType; // Importar PlayerActionType
import PokeBody.Services.DamageCalculator;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * An AI selector that attempts to make more strategic decisions by first
 * considering if switching is advantageous (using ChangingAISelector) and
 * then deciding on an attack (using AttackingAISelector) if no switch occurs.
 */
public class ExpertAISelector implements MovementSelector {
    private final ChangingAISelector changingLogic;
    // AttackingLogic no es estrictamente necesario como campo aquí,
    // ya que ChangingAISelector lo usa internamente como backup.
    // private final AttackingAISelector attackingLogic;

    /**
     * Constructor for ExpertAISelector.
     * It initializes the changing and attacking logic components.
     * @param damageCalculator The DamageCalculator instance needed by sub-selectors.
     */
    public ExpertAISelector(DamageCalculator damageCalculator) {
        // Ensure DamageCalculator is not null
        if (damageCalculator == null) {
            throw new IllegalArgumentException("DamageCalculator cannot be null for ExpertAISelector");
        }
        // The AttackingAI will be the backup if ChangingAI decides not to switch.
        AttackingAISelector attackBackup = new AttackingAISelector(damageCalculator);
        // ChangingAI needs a backup selector to delegate to if it doesn't switch.
        this.changingLogic = new ChangingAISelector(damageCalculator, attackBackup);
    }

    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
        // --- Expert AI Logic ---

        // 1. Handle the case where the active Pokémon is fainted (must switch)
        // ChangingAISelector ya maneja esto internamente y devolverá una acción de cambio si es necesario y posible.
        if (activePokemon == null || activePokemon.estaDebilitado()) {
            System.out.println(activeTrainer.getName() + " (IA Experta): Pokémon activo KO, llamando a changingLogic para forzar cambio.");
            return changingLogic.selectAction(activeTrainer, activePokemon, opponentPokemon);
        }

        // 2. Let ChangingAISelector evaluate the situation.
        // It will either decide to switch or delegate to its backup (AttackingAISelector).
        System.out.println(activeTrainer.getName() + " (IA Experta): Evaluando situación con changingLogic...");
        PlayerActionChoice decision = changingLogic.selectAction(activeTrainer, activePokemon, opponentPokemon);

        // 3. Return the decision made by ChangingAISelector.
        // If it decided to switch, the decision type will be SWITCH_POKEMON.
        // If it decided not to switch, it will have already called the backup (AttackingAISelector)
        // and the decision type will be ATTACK.
        if (decision.type == PlayerActionType.SWITCH_POKEMON) {
             System.out.println(activeTrainer.getName() + " (IA Experta): changingLogic decidió cambiar.");
        } else if (decision.type == PlayerActionType.ATTACK) {
             System.out.println(activeTrainer.getName() + " (IA Experta): changingLogic delegó al backup (AttackingAI), se atacará.");
        } else {
             // Handle other potential action types if necessary (e.g., USE_ITEM if AI could use items)
             System.out.println(activeTrainer.getName() + " (IA Experta): changingLogic devolvió tipo de acción inesperado: " + decision.type);
        }

        return decision;
    }
}