package PokeBody.domain;

import java.util.Map;

import PokeBody.Data.PokemonData;

public class HealerPokemon extends Pokemon {

    /**
     * Constructor principal para HealerPokemon, usando PokemonData.
     * Llama al constructor de la clase base Pokemon.
     * @param data Los datos base del Pokémon.
     * @param allMovesMap Mapa de todos los movimientos disponibles.
     * @param nivelActual El nivel del Pokémon.
     */
    public HealerPokemon(PokemonData data, Map<String, Movements> allMovesMap, int nivelActual) {
        super(data, allMovesMap, nivelActual);
        this.setHpActual(this.getHpMax());
    }

    /**
     * Constructor de copia para crear un HealerPokemon a partir de un Pokemon existente.
     * @param basePokemon El Pokémon base del cual copiar las estadísticas y estado.
     */

    public HealerPokemon(Pokemon basePokemon) {
        super(basePokemon); 
        this.setNombre(super.getNombre() + " (Sanador)");
        this.setHpActual(this.getHpMax());
    }
}