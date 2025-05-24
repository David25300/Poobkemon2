package PokeBody.Services.AI;

import java.util.List;
import java.util.Random;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.DamageCalculator;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.Type;

/**
 * AI selector that prioritizes switching Pokémon if the matchup is unfavorable
 * or if a better option is available. If no switch is made, it delegates
 * the action selection to a backup selector (e.g., AttackingAISelector).
 */
public class ChangingAISelector implements MovementSelector {
    private final Random random = new Random(); // No se usa actualmente, pero se mantiene por si acaso
    private final DamageCalculator damageCalculator;
    private final MovementSelector backupSelector; // Selector a usar si no se cambia

    /**
     * Constructor for ChangingAISelector.
     * @param damageCalculator Calculator for damage estimations.
     * @param backupSelector The MovementSelector to use if no switch is performed.
     */
    public ChangingAISelector(DamageCalculator damageCalculator, MovementSelector backupSelector) {
        if (damageCalculator == null || backupSelector == null) {
            throw new IllegalArgumentException("DamageCalculator and backupSelector cannot be null for ChangingAISelector");
        }
        this.damageCalculator = damageCalculator;
        this.backupSelector = backupSelector;
    }

    @Override
    public PlayerActionChoice selectAction(Trainer activeTrainer, Pokemon activePokemon, Pokemon opponentPokemon) {

        // --- Caso 1: El Pokémon activo está debilitado ---
        if (activePokemon == null || activePokemon.estaDebilitado()) {
            // Debe cambiar obligatoriamente si hay opciones
            System.out.println(activeTrainer.getName() + " (IA Cambiante): Pokémon activo debilitado, buscando cambio...");
            return findBestSwitch(activeTrainer, activePokemon, opponentPokemon, true); // forceSwitch = true
        }

        // --- Caso 2: Evaluar si un cambio es beneficioso ---
        double currentMatchupScore = evaluateMatchup(activePokemon, opponentPokemon);
        int bestSwitchIndex = -1;
        Pokemon bestSwitchCandidate = null;
        double bestSwitchScore = -Double.MAX_VALUE; // Empezar con un score muy bajo para encontrar el mejor

        List<Pokemon> team = activeTrainer.getteam();
        boolean canSwitch = false; // ¿Hay algún Pokémon al que se pueda cambiar?

        // Buscar el mejor candidato para cambiar
        for (int i = 0; i < team.size(); i++) {
            Pokemon candidate = team.get(i);
            // Considerar solo si el candidato es diferente, no está debilitado
            if (candidate != null && !candidate.estaDebilitado() && candidate != activePokemon) {
                canSwitch = true; // Se encontró al menos una opción de cambio
                double candidateScore = evaluateMatchup(candidate, opponentPokemon);
                if (candidateScore > bestSwitchScore) {
                    bestSwitchScore = candidateScore;
                    bestSwitchIndex = i;
                    bestSwitchCandidate = candidate;
                }
            }
        }

        // --- Decisión: Cambiar o delegar al backupSelector ---
        boolean shouldSwitch = false;
        if (canSwitch && bestSwitchCandidate != null) {
            // Criterios para decidir cambiar:
            // 1. ¿El matchup actual es muy malo Y el candidato es significativamente mejor o al menos no malo?
            boolean urgentSwitch = (currentMatchupScore < -30 && bestSwitchScore > -20); // Umbrales de ejemplo
            // 2. ¿El candidato es MUCHO mejor que el actual? (Cambio estratégico)
            boolean significantImprovement = (bestSwitchScore > currentMatchupScore + 40); // Umbral alto para evitar cambios innecesarios

            if (urgentSwitch || significantImprovement) {
                shouldSwitch = true;
            }
        }

        // Si se decide cambiar y se encontró un índice válido
        if (shouldSwitch && bestSwitchIndex != -1) {
            PlayerActionChoice switchAction = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
            switchAction.switchToPokemonIndex = bestSwitchIndex;
            System.out.println(activeTrainer.getName() + " (IA Cambiante) decide cambiar a " + bestSwitchCandidate.getNombre());
            return switchAction;
        } else {
            // Si no se debe cambiar O no hay a quién cambiar (canSwitch es false)
            if (!canSwitch) {
                 System.out.println(activeTrainer.getName() + " (IA Cambiante): No hay Pokémon a los que cambiar.");
            } else {
                 System.out.println(activeTrainer.getName() + " (IA Cambiante): No ve un cambio beneficioso.");
            }
            // Delegar la decisión de ataque/estado al selector de respaldo
            System.out.println(activeTrainer.getName() + " (IA Cambiante): Delega la acción al backup selector (" + backupSelector.getClass().getSimpleName() + ").");
            return backupSelector.selectAction(activeTrainer, activePokemon, opponentPokemon);
        }
    }

    /**
     * Evalúa qué tan bueno es el matchup de 'self' contra 'opponent'.
     * Un score más alto es mejor para 'self'. Considera tipos y velocidad.
     */
    private double evaluateMatchup(Pokemon self, Pokemon opponent) {
        if (self == null || opponent == null || self.estaDebilitado()) return -Double.MAX_VALUE;

        double score = 0;

        // 1. Ventaja Ofensiva (Potencial de daño a 'opponent')
        double maxOffensiveEffectiveness = 0;
        if (self.getMovimientos() != null) {
            for (Movements move : self.getMovimientos()) {
                if (move != null && move.puedeUsarse() && move.getCategoria() != Movements.TipoAtaque.ESTADO) {
                    double eff = damageCalculator.obtenerMultiplicadorEfectividad(self, opponent, move);
                    maxOffensiveEffectiveness = Math.max(maxOffensiveEffectiveness, eff);
                }
            }
        }
        if (maxOffensiveEffectiveness >= 4) score += 60;
        else if (maxOffensiveEffectiveness >= 2) score += 40;
        else if (maxOffensiveEffectiveness == 0) score -= 100; // Muy malo si el oponente es inmune a todo

        // 2. Ventaja Defensiva (Resistencia a los tipos de 'opponent')
        // Simplificación: Asume que el oponente usará movimientos de sus tipos
        double maxOpponentEffOnSelf = 0;
        if (opponent.getTipos() != null) {
            for (Type.Tipo tipoOponente : opponent.getTipos()) {
                 // Simula un movimiento del tipo del oponente contra 'self'
                 double eff = Type.obtenerMultiplicador(tipoOponente, self.getTipos().get(0), self.getTipos().size() > 1 ? self.getTipos().get(1) : null);
                 maxOpponentEffOnSelf = Math.max(maxOpponentEffOnSelf, eff);
            }
        }
        if (maxOpponentEffOnSelf >= 4) score -= 80; // Muy malo ser débil x4
        else if (maxOpponentEffOnSelf >= 2) score -= 50; // Malo ser débil x2
        else if (maxOpponentEffOnSelf == 0) score += 60; // Excelente ser inmune
        else if (maxOpponentEffOnSelf <= 0.5) score += 30; // Bueno resistir

        // 3. Ventaja de Velocidad
        if (self.getVelocidadModificada() > opponent.getVelocidadModificada()) {
            score += 15; // Bonus por ser más rápido
        } else if (self.getVelocidadModificada() < opponent.getVelocidadModificada()) {
            score -= 5; // Pequeña penalización por ser más lento
        }

        // 4. Salud Actual (bonus si está sano, penalización si está bajo)
        double healthRatio = (double) self.getHpActual() / self.getHpMax();
        if (healthRatio > 0.7) score += 10;
        else if (healthRatio < 0.3) score -= 25; // Más urgente cambiar si está bajo de vida

        return score;
    }


    /**
     * Encuentra el mejor Pokémon disponible para cambiar, o el primero si es forzado.
     * Usado principalmente cuando el Pokémon activo está debilitado.
     *
     * @param activeTrainer El entrenador que necesita cambiar.
     * @param currentPokemon El Pokémon actual (puede ser null o debilitado).
     * @param opponentPokemon El Pokémon oponente.
     * @param forceSwitch Si true, debe elegir un cambio si es posible.
     * @return Una PlayerActionChoice de tipo SWITCH_POKEMON si se encuentra un cambio,
     * o una acción de ATTACK (Forcejeo) por defecto si no hay opciones.
     */
    private PlayerActionChoice findBestSwitch(Trainer activeTrainer, Pokemon currentPokemon, Pokemon opponentPokemon, boolean forceSwitch) {
        List<Pokemon> team = activeTrainer.getteam();
        int bestSwitchIndex = -1;
        Pokemon bestCandidate = null;
        double highestScore = -Double.MAX_VALUE;
        boolean foundViableSwitch = false;

        for (int i = 0; i < team.size(); i++) {
            Pokemon candidate = team.get(i);
            // Considerar solo si no está debilitado y es diferente al actual (a menos que el actual esté KO)
            if (candidate != null && !candidate.estaDebilitado() && (currentPokemon == null || candidate != currentPokemon || currentPokemon.estaDebilitado())) {
                foundViableSwitch = true;
                double score = evaluateMatchup(candidate, opponentPokemon);
                if (score > highestScore) {
                    highestScore = score;
                    bestSwitchIndex = i;
                    bestCandidate = candidate;
                }
            }
        }

        // Si se encontró un candidato viable (y se debe cambiar o se encontró uno)
        if (foundViableSwitch && bestCandidate != null) {
            PlayerActionChoice switchAction = new PlayerActionChoice(PlayerActionType.SWITCH_POKEMON);
            switchAction.switchToPokemonIndex = bestSwitchIndex;
            System.out.println(activeTrainer.getName() + " (IA Cambiante - Forzado/Mejor): Cambiando a " + bestCandidate.getNombre());
            return switchAction;
        } else {
            // No hay switches válidos (todo el equipo debilitado o solo queda el actual y no está KO)
            System.out.println(activeTrainer.getName() + " (IA Cambiante - Forzado/Mejor): No se encontró cambio viable.");
            // Si no se puede cambiar, debe atacar (Forcejeo si no hay movimientos)
            // Devolver una acción de ataque por defecto, CombatManager o el backupSelector se encargarán.
            // Es más seguro delegar al backupSelector si el Pokémon actual aún puede actuar.
            if (currentPokemon != null && !currentPokemon.estaDebilitado()) {
                 System.out.println("Delegando al backup selector porque no hay cambio viable pero el actual puede actuar.");
                 return backupSelector.selectAction(activeTrainer, currentPokemon, opponentPokemon);
            } else {
                 // Si el actual está KO y no hay a quién cambiar, el juego debería terminar,
                 // pero devolvemos Forcejeo como fallback extremo.
                PlayerActionChoice fallback = new PlayerActionChoice(PlayerActionType.ATTACK);
                fallback.moveIndex = 0; // Índice para Forcejeo/Struggle
                System.out.println("Devolviendo Forcejeo como fallback extremo.");
                return fallback;
            }
        }
    }
}