// Archivo: PokeBody/Services/AI/RandomAISelector.java 
// (o PokeBody.Services si no lo has movido al subpaquete AI)
package PokeBody.Services.AI; // O PokeBody.Services

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * Otra implementación de IA que elige un movimiento al azar.
 * Debería tener una lógica diferenciada de RandomMovementSelector si ambas se mantienen.
 */
public class RandomAISelector implements MovementSelector {
    private final Random random = new Random();

    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
        PlayerActionChoice actionChoice = new PlayerActionChoice(PlayerActionType.ATTACK);

        if (activePokemon == null || activePokemon.estaDebilitado()) {
            actionChoice.moveIndex = -1; 
            System.err.println( (activePokemon != null ? activePokemon.getNombre() : "Pokémon") + 
                                " (RandomAISelector) está debilitado.");
            return actionChoice;
        }
        
        if (activePokemon.getMovimientos() == null || activePokemon.getMovimientos().isEmpty()) {
            System.err.println("RandomAISelector: " + activePokemon.getNombre() + " no tiene movimientos.");
            actionChoice.moveIndex = 0; 
            return actionChoice;
        }

        List<Movements> moves = activePokemon.getMovimientos();
        List<Integer> usableMoveIndices = IntStream.range(0, moves.size())
                .filter(i -> moves.get(i) != null && moves.get(i).puedeUsarse())
                .boxed()
                .collect(Collectors.toList());

        if (usableMoveIndices.isEmpty()) {
            actionChoice.moveIndex = 0; 
        } else {
            int randomIndexInList = random.nextInt(usableMoveIndices.size());
            actionChoice.moveIndex = usableMoveIndices.get(randomIndexInList);
        }
        
        // System.out.println(activePokemon.getNombre() + " (RandomAISelector) elige movimiento índice: " + actionChoice.moveIndex);
        return actionChoice;
    }
}
