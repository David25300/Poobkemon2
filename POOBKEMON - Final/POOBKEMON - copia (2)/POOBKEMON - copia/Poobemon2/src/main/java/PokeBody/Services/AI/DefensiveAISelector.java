// Archivo: PokeBody/Services/AI/DefensiveAISelector.java
package PokeBody.Services.AI;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import PokeBody.Services.CombatManager.PlayerActionChoice; 
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.DamageCalculator;
import PokeBody.Services.EffectHandler; // Necesario para esInmuneAEstado
import PokeBody.domain.Efecto; // Clase base abstracta
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.efectos.InfligirEstado;
import PokeBody.domain.efectos.ModificarStatEfecto;


public class DefensiveAISelector implements MovementSelector {
    private final Random random = new Random();
    private final DamageCalculator damageCalculator;

    public DefensiveAISelector(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {
        PlayerActionChoice action = new PlayerActionChoice(PlayerActionType.ATTACK);

        if (activePokemon == null || activePokemon.estaDebilitado() || activePokemon.getMovimientos() == null || activePokemon.getMovimientos().isEmpty()) {
            return selectBestSwitchOrFirstAvailable(activeTrainer, activePokemon, opponentPokemon);
        }

        List<Movements> usableMoves = activePokemon.getMovimientos().stream()
                .filter(m -> m != null && m.puedeUsarse())
                .collect(Collectors.toList());

        if (usableMoves.isEmpty()) {
            action.moveIndex = 0; // Lucha/Struggle
            System.out.println(activePokemon.getNombre() + " (IA Defensiva) no tiene PP, usará Forcejeo (mov 0).");
            return action;
        }

        Movements bestDefensiveMove = usableMoves.stream()
            .max(Comparator.comparingDouble(move -> scoreDefensiveMove(move, activePokemon, opponentPokemon)))
            .orElse(null);

        if (bestDefensiveMove != null && scoreDefensiveMove(bestDefensiveMove, activePokemon, opponentPokemon) > 20) { 
            action.moveIndex = activePokemon.getMovimientos().indexOf(bestDefensiveMove);
        } else {
            Movements fallbackMove = usableMoves.stream()
                .filter(m -> m.getCategoria() != Movements.TipoAtaque.ESTADO) 
                .max(Comparator.comparingDouble(move -> {
                    double eff = damageCalculator.obtenerMultiplicadorEfectividad(activePokemon, opponentPokemon, move);
                    if (eff == 0) return -1000; 
                    return eff * move.getPotencia();
                }))
                .orElse(usableMoves.get(random.nextInt(usableMoves.size()))); 
            action.moveIndex = activePokemon.getMovimientos().indexOf(fallbackMove);
        }
        
        // System.out.println(activePokemon.getNombre() + " (IA Defensiva) elige: " + activePokemon.getMovimientos().get(action.moveIndex).getNombre());
        return action;
    }

    private double scoreDefensiveMove(Movements move, Pokemon self, Pokemon opponent) {
        double score = 0;

        if (move.getCategoria() == Movements.TipoAtaque.ESTADO) {
            Efecto efecto = move.getEfecto(); 
            if (efecto instanceof ModificarStatEfecto) { 
                ModificarStatEfecto modStat = (ModificarStatEfecto) efecto;
                
                // Asumiendo que ModificarStatEfecto tiene un método esAlUsuario()
                // y getNiveles(), getStatAfectada()
                // Esta lógica necesita que ModificarStatEfecto defina cómo saber a quién afecta.
                // Por ahora, mantendremos la lógica simplificada basada en el signo de getNiveles()
                // y los campos boostStat/boostAmount del movimiento si el efecto no es explícito.

                if (modStat.getNiveles() > 0 && modStat.esAlUsuario()) { // Sube stats propias
                    if (modStat.getStatAfectada() == PokeBody.domain.StatBoosts.Stat.DEFENSE || 
                        modStat.getStatAfectada() == PokeBody.domain.StatBoosts.Stat.SP_DEFENSE) {
                        score += 80;
                        if (self.getStatBoostLevel(modStat.getStatAfectada()) >= 6) score -= 70;
                    }
                } else if (modStat.getNiveles() < 0 && !modStat.esAlUsuario()) { // Baja stats del oponente
                     if (modStat.getStatAfectada() == PokeBody.domain.StatBoosts.Stat.ATTACK || 
                         modStat.getStatAfectada() == PokeBody.domain.StatBoosts.Stat.SP_ATTACK) {
                        score += 70;
                         if (opponent.getStatBoostLevel(modStat.getStatAfectada()) <= -6) score -= 60;
                    }
                }
            }
            
            // Considerar también los boosts directos definidos en Movements.java si son independientes del Efecto
            if (move.getBoostStat() != null && move.getBoostAmount() > 0) { // Asume que es para el usuario (self)
                if (move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.DEFENSE || move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.SP_DEFENSE) {
                    score += 80; // Prioridad alta a subir defensas
                    if (self.getStatBoostLevel(move.getBoostStat()) >= 6) score -= 70; // Menos útil si ya está al máximo
                }
            }
            if (move.getBoostStat() != null && move.getBoostAmount() < 0) { // Asume que es para el oponente
                 if ( (move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.ATTACK || move.getBoostStat() == PokeBody.domain.StatBoosts.Stat.SP_ATTACK) ) {
                    score += 70; // Prioridad alta a bajar ataque del oponente
                     if (opponent.getStatBoostLevel(move.getBoostStat()) <= -6) score -= 60; 
                }
            }

            if (efecto instanceof InfligirEstado) { 
                String estadoAInfligir = ((InfligirEstado) efecto).getEstadoAInfligir().toUpperCase();
                if (opponent.getEstado() == null) { 
                    if (estadoAInfligir.equals("PARALIZADO") && !EffectHandler.esInmuneAEstado(opponent, "PARALIZADO")) score += 65;
                    if (estadoAInfligir.equals("DORMIDO") && !EffectHandler.esInmuneAEstado(opponent, "DORMIDO")) score += 75;
                    if (estadoAInfligir.equals("QUEMADURA") && !EffectHandler.esInmuneAEstado(opponent, "QUEMADURA")) score += 60;
                    if (estadoAInfligir.equals("VENENO") && !EffectHandler.esInmuneAEstado(opponent, "VENENO")) score += 50;
                    if (estadoAInfligir.equals("TOXICO") && !EffectHandler.esInmuneAEstado(opponent, "TOXICO")) score += 70;
                }
            }
            // Lógica para Protección y Recuperación
            if (efecto instanceof PokeBody.domain.efectos.ProteccionEfecto) {
                // Evaluar si es buen momento para protegerse (ej. si el oponente es más rápido y puede hacer KO)
                score += 70; // Puntuación base por protegerse
                if (self.getConsecutiveProtectUses() > 0) score -= self.getConsecutiveProtectUses() * 20; // Penalizar usos consecutivos
            }
            // if (efecto instanceof PokeBody.domain.efectos.RecuperarHpEfecto) { // Si tuvieras esta clase
            //    if (self.getHpActual() < self.getHpMax() * 0.6) score += 85; // Más útil si está bajo de vida
            // }
 
        } else { // Movimientos de ataque
            if (self.tieneTipo(move.getTipo())) score += 10; 
            double effectivenessOnOpponent = damageCalculator.obtenerMultiplicadorEfectividad(self, opponent, move);
            if (effectivenessOnOpponent < 1.0 && effectivenessOnOpponent > 0) score += 15; 
            if (effectivenessOnOpponent == 0) score -= 100; // Muy malo si es inmune
            score += move.getPotencia() * 0.1; 
        }
        return score;
    }

    private PlayerActionChoice selectBestSwitchOrFirstAvailable(Trainer activeTrainer, Pokemon currentPokemon, Pokemon opponentPokemon) {
        List<Pokemon> team = activeTrainer.getteam();
        int bestSwitchIndex = -1;

        for (int i = 0; i < team.size(); i++) {
            Pokemon candidate = team.get(i);
            if (candidate != null && !candidate.estaDebilitado()) {
                if (currentPokemon == null || candidate.getId() != currentPokemon.getId() || currentPokemon.estaDebilitado()) { // Comparar por ID para evitar problemas con instancias
                    bestSwitchIndex = i;
                    break; 
                }
            }
        }

        if (bestSwitchIndex != -1) {
            PlayerActionChoice switchAction = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
            switchAction.switchToPokemonIndex = bestSwitchIndex;
            return switchAction;
        } else {
            PlayerActionChoice fallbackAttack = new PlayerActionChoice(PlayerActionType.ATTACK);
            fallbackAttack.moveIndex = 0; 
            return fallbackAttack;
        }
    }
}
