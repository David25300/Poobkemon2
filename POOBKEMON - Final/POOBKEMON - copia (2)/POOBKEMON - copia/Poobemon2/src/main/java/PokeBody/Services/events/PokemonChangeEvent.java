package PokeBody.Services.events;

import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * Evento que se dispara cuando un entrenador cambia su Pokémon activo.
 */
public class PokemonChangeEvent extends CombatEvent {
    private final Trainer trainer; // El entrenador que realiza el cambio
    private final Pokemon oldPokemon; // El Pokémon que sale del combate
    private final Pokemon newPokemon; // El Pokémon que entra al combate

    /**
     * Constructor para PokemonChangeEvent.
     * @param trainer El entrenador que realiza el cambio.
     * @param oldPokemon El Pokémon que es retirado. Puede ser null si es el primer Pokémon enviado.
     * @param newPokemon El Pokémon que es enviado al combate.
     */
    public PokemonChangeEvent(Trainer trainer, Pokemon oldPokemon, Pokemon newPokemon) {
        this.trainer = trainer;
        this.oldPokemon = oldPokemon;
        this.newPokemon = newPokemon;
    }

    /**
     * Obtiene el entrenador que realizó el cambio.
     * @return El entrenador.
     */
    public Trainer getTrainer() {
        return trainer;
    }

    /**
     * Obtiene el Pokémon que fue retirado.
     * @return El Pokémon retirado, o null si no había Pokémon activo antes.
     */
    public Pokemon getOldPokemon() {
        return oldPokemon;
    }

    /**
     * Obtiene el Pokémon que entra al combate.
     * @return El nuevo Pokémon activo.
     */
    public Pokemon getNewPokemon() {
        return newPokemon;
    }

    @Override
    public String toString() {
        return trainer.getName() + " cambió a " + (oldPokemon != null ? oldPokemon.getNombre() : "N/A") +
               " por " + (newPokemon != null ? newPokemon.getNombre() : "N/A");
    }
}
