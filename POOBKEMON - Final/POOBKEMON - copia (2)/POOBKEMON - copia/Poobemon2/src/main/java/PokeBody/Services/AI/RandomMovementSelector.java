// Archivo: PokeBody/Services/AI/RandomMovementSelector.java
// Asegúrate de que esté en el paquete correcto, por ejemplo, PokeBody.Services.AI
package PokeBody.Services.AI; // O PokeBody.Services si lo prefieres así

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // Necesario para la nueva firma

import PokeBody.Services.CombatManager.PlayerActionChoice; // Necesario
import PokeBody.Services.CombatManager.PlayerActionType; // Necesario
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * Un MovementSelector simple para la IA que elige un movimiento usable al azar.
 * Ahora implementa la interfaz MovementSelector con selectAction.
 */
public class RandomMovementSelector implements MovementSelector {

    private final Random random = new Random();

    /**
     * Selecciona una acción para el Pokémon activo del entrenador.
     * Esta implementación siempre elegirá atacar con un movimiento usable al azar.
     *
     * @param activeTrainer   El Entrenador que está tomando la decisión (IA).
     * @param activePokemon   El Pokémon activo del entrenador.
     * @param opponentPokemon El Pokémon activo del oponente.
     * @return Un PlayerActionChoice de tipo ATTACK con el índice del movimiento seleccionado.
     * Si no hay movimientos usables, el índice será para "Lucha" (Forcejeo), usualmente 0.
     */
    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
        PlayerActionChoice actionChoice = new PlayerActionChoice(PlayerActionType.ATTACK);

        if (activePokemon == null || activePokemon.estaDebilitado()) {
            // Si el Pokémon está debilitado, la IA debería intentar cambiar.
            // Esta lógica de cambio podría ser más sofisticada o manejada por una IA de nivel superior.
            // Por ahora, si está KO, no puede atacar. CombatManager deberá manejar el cambio forzado.
            // Devolvemos un ataque con índice -1 para indicar que no se pudo seleccionar un movimiento.
            actionChoice.moveIndex = -1; // Indicar acción no válida o necesidad de cambio
            System.err.println( (activePokemon != null ? activePokemon.getNombre() : "Pokémon") + 
                                " (IA Aleatoria) está debilitado y no puede seleccionar movimiento.");
            return actionChoice;
        }
        
        if (activePokemon.getMovimientos() == null || activePokemon.getMovimientos().isEmpty()) {
            System.err.println("RandomMovementSelector: " + activePokemon.getNombre() + " no tiene movimientos.");
            actionChoice.moveIndex = 0; // Asumir Lucha/Struggle
            return actionChoice;
        }

        List<Movements> moves = activePokemon.getMovimientos();

        // Encontrar índices de movimientos que pueden usarse (PP > 0)
        List<Integer> usableMoveIndices = IntStream.range(0, moves.size())
                .filter(i -> moves.get(i) != null && moves.get(i).puedeUsarse())
                .boxed()
                .collect(Collectors.toList());

        if (usableMoveIndices.isEmpty()) {
            // No quedan movimientos usables, se seleccionará Lucha/Struggle
            // System.out.println("RandomMovementSelector: No hay movimientos usables con PP para " + activePokemon.getNombre() + ". Usando Lucha (movimiento 0 por defecto).");
            actionChoice.moveIndex = 0; // Asumir que el índice 0 es Lucha o un placeholder
        } else {
            // Seleccionar un índice al azar de la lista de movimientos usables
            int randomIndexInList = random.nextInt(usableMoveIndices.size());
            actionChoice.moveIndex = usableMoveIndices.get(randomIndexInList);
        }
        
        // System.out.println(activePokemon.getNombre() + " (IA Aleatoria) elige movimiento índice: " + actionChoice.moveIndex);
        return actionChoice;
    }
}
