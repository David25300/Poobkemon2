// Archivo: PokeBody/Services/AI/AttackingAISelector.java
package PokeBody.Services.AI;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.DamageCalculator;
import PokeBody.domain.MovementSelector; // Para calcular daño y efectividad
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class AttackingAISelector implements MovementSelector {
    private final Random random = new Random();
    private final DamageCalculator damageCalculator;

    public AttackingAISelector(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
        PlayerActionChoice action = new PlayerActionChoice(PlayerActionType.ATTACK);

        if (activePokemon == null || activePokemon.estaDebilitado() || activePokemon.getMovimientos() == null || activePokemon.getMovimientos().isEmpty()) {
            // Si no puede atacar (debilitado o sin movimientos), intenta cambiar.
            // Esta lógica de cambio podría ser más sofisticada.
            return selectBestSwitchOrFirstAvailable(activeTrainer, activePokemon, opponentPokemon);
        }

        List<Movements> usableMoves = activePokemon.getMovimientos().stream()
                .filter(m -> m != null && m.puedeUsarse())
                .collect(Collectors.toList());

        if (usableMoves.isEmpty()) {
            // No hay movimientos con PP, debería usar Lucha/Struggle
            // CombatManager o ActionExecutor manejarán la lógica de "Lucha"
            // Devolvemos el primer movimiento (índice 0) como placeholder para Lucha
            action.moveIndex = 0; 
            System.out.println(activePokemon.getNombre() + " (IA Atacante) no tiene PP, usará Forcejeo (mov 0).");
            return action;
        }

        // Priorizar movimientos:
        // 1. Súper efectivos con buen poder.
        // 2. STAB con buen poder.
        // 3. Movimientos que aumentan el ataque/ataque especial del usuario.
        // 4. Movimientos que bajan la defensa/defensa especial del oponente.
        // 5. Movimientos con poder base alto.

        Movements bestMove = usableMoves.stream()
            .max(Comparator.comparingDouble(move -> scoreMove(move, activePokemon, opponentPokemon)))
            .orElse(usableMoves.get(random.nextInt(usableMoves.size()))); // Fallback a uno aleatorio si todos tienen score 0

        action.moveIndex = activePokemon.getMovimientos().indexOf(bestMove);
        
        // System.out.println(activePokemon.getNombre() + " (IA Atacante) elige: " + bestMove.getNombre());
        return action;
    }

    private double scoreMove(Movements move, Pokemon attacker, Pokemon defender) {
        if (move.getCategoria() == Movements.TipoAtaque.ESTADO) {
            // Evaluar movimientos de estado ofensivos
            if (move.getBoostStat() != null && move.getBoostAmount() != 0) {
                if ( (move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.ATTACK || move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.SP_ATTACK) && move.getBoostAmount() > 0 /*&& esAlUsuario (necesitaría flag)*/ ) {
                    return 60; // Movimiento que sube el propio ataque
                }
                if ( (move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.DEFENSE || move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.SP_DEFENSE) && move.getBoostAmount() < 0 /*&& !esAlUsuario*/ ) {
                    return 55; // Movimiento que baja la defensa del rival
                }
            }
            return 0; // Otros movimientos de estado tienen bajo score para IA atacante
        }

        // Para movimientos ofensivos
        double power = move.getPotencia();
        double effectiveness = damageCalculator.obtenerMultiplicadorEfectividad(attacker, defender, move);
        boolean stab = attacker.tieneTipo(move.getTipo());

        double score = power;
        if (stab) score *= 1.5;
        score *= effectiveness; // Multiplicador de efectividad (2x, 4x, 0.5x, etc.)

        // Pequeño bonus si es súper efectivo
        if (effectiveness >= 2) score += 20;
        
        // Considerar si puede ser K.O.
        // int potentialDamage = damageCalculator.calcularDanioReal(attacker, defender, move, false); // Sin crítico para estimación
        // if (potentialDamage >= defender.getHpActual()) {
        //     score += 1000; // Prioridad alta para K.O.
        // }

        return score;
    }
    
    // Método de fallback si el Pokémon activo está KO o no puede atacar.
    protected PlayerActionChoice selectBestSwitchOrFirstAvailable(Trainer activeTrainer, Pokemon currentPokemon, Pokemon opponentPokemon) {
        List<Pokemon> team = activeTrainer.getteam();
        int bestSwitchIndex = -1;
        double bestScore = -Double.MAX_VALUE;

        for (int i = 0; i < team.size(); i++) {
            Pokemon candidate = team.get(i);
            if (candidate != null && !candidate.estaDebilitado() && candidate != currentPokemon) {
                // Aquí se podría añadir una lógica más compleja para "mejor cambio"
                // Por ahora, el primero disponible que no sea el actual.
                if (bestSwitchIndex == -1) bestSwitchIndex = i; // Tomar el primero disponible
                
                // Ejemplo de evaluación simple para cambio (podría ser más complejo)
                // double candidateScore = evaluatePokemonMatchup(candidate, opponentPokemon);
                // if (candidateScore > bestScore) {
                //    bestScore = candidateScore;
                //    bestSwitchIndex = i;
                // }
            }
        }

        if (bestSwitchIndex != -1) {
            PlayerActionChoice switchAction = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
            switchAction.switchToPokemonIndex = bestSwitchIndex;
            // System.out.println(activeTrainer.getName() + " (IA Atacante) necesita cambiar, elige a " + team.get(bestSwitchIndex).getNombre());
            return switchAction;
        } else {
            // No hay Pokémon para cambiar o algo salió mal, la IA no puede hacer nada.
            // CombatManager debería detectar esto como una pérdida si no hay acciones válidas.
            // Devolver un ataque con el primer movimiento del Pokémon actual (aunque esté KO)
            // para que CombatManager maneje el flujo.
            PlayerActionChoice fallbackAttack = new PlayerActionChoice(PlayerActionType.ATTACK);
            fallbackAttack.moveIndex = 0; 
            return fallbackAttack;
        }
    }
}

