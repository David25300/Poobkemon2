package PokeBody.Services.events;

import PokeBody.domain.Pokemon;

/**
 * Event dispatched when a Pokémon's HP changes.
 */
public class PokemonHpChangedEvent extends CombatEvent {
    private final Pokemon targetPokemon;
    private final int newHp;
    private final int maxHp;
    private final int previousHp; // Optional: useful for knowing how much HP changed

    /**
     * Constructs a new PokemonHpChangedEvent.
     * @param targetPokemon The Pokémon whose HP changed.
     * @param newHp The new current HP of the Pokémon.
     * @param maxHp The maximum HP of the Pokémon.
     * @param previousHp The HP of the Pokémon before the change.
     */
    public PokemonHpChangedEvent(Pokemon targetPokemon, int newHp, int maxHp, int previousHp) {
        super();
        if (targetPokemon == null) {
            throw new IllegalArgumentException("Target Pokémon cannot be null for PokemonHpChangedEvent.");
        }
        this.targetPokemon = targetPokemon;
        this.newHp = newHp;
        this.maxHp = maxHp;
        this.previousHp = previousHp;
    }

    /**
     * Gets the Pokémon whose HP changed.
     * @return The target Pokémon.
     */
    public Pokemon getTargetPokemon() {
        return targetPokemon;
    }

    /**
     * Gets the new current HP of the Pokémon.
     * @return The new HP.
     */
    public int getNewHp() {
        return newHp;
    }

    /**
     * Gets the maximum HP of the Pokémon.
     * @return The maximum HP.
     */
    public int getMaxHp() {
        return maxHp;
    }

    /**
     * Gets the HP of the Pokémon before this change.
     * @return The previous HP.
     */
    public int getPreviousHp() {
        return previousHp;
    }

    /**
     * Calculates the amount of HP changed (positive for healing, negative for damage).
     * @return The difference between new HP and previous HP.
     */
    public int getHpChangeAmount() {
        return newHp - previousHp;
    }
}
