package PokeBody.Services.events;

import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class PokemonFaintedEvent extends CombatEvent {
    private final Pokemon faintedPokemon;
    private final Trainer owner;

    public PokemonFaintedEvent(Pokemon faintedPokemon, Trainer owner) {
        super();
        this.faintedPokemon = faintedPokemon;
        this.owner = owner;
    }

    public Pokemon getFaintedPokemon() { return faintedPokemon; }
    public Trainer getOwner() { return owner; }
}