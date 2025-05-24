package PokeBody.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa a un entrenador con su equipo de Pokémon y los ítems
 * que porta en su bolsa.
 */
public class Trainer implements Serializable {
    private final String name;
    private final List<Pokemon> team;
    private final List<Item> items;

    /**
     * Crea un entrenador con nombre, equipo de Pokémon e ítems.
     * @param name Nombre del entrenador
     * @param team Lista de Pokémon (hasta 6)
     * @param items Lista de ítems disponibles
     */
    public Trainer(String name, List<Pokemon> team, List<Item> items) {
        this.name = name;
        if (team.size() > 6) {
            throw new IllegalArgumentException("Un entrenador sólo puede llevar hasta 6 Pokémon");
        }
        this.team = new ArrayList<>(team);
        this.items = new ArrayList<>(items);
    }

    public String getName() {
        return name;
    }

    /**
     * @return Equipo de Pokémon del entrenador (inmutable)
     */
    public List<Pokemon> getTeam() {
        return List.copyOf(team);
    }

    /**
     * @return Lista de ítems disponibles
     */
    public List<Item> getItems() {
        return List.copyOf(items);
    }

    /**
     * Añade un ítem al entrenador
     */
    public void addItem(Item item) {
        items.add(item);
    }

    /**
     * Elimina un ítem del entrenador si existe
     * @return true si se eliminó
     */
    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    /**
     * Sustituye un Pokémon en el equipo por índice
     * @param index posición (0-based)
     * @param pokemon nuevo Pokémon
     */
    public void setPokemon(int index, Pokemon pokemon) {
        if (index < 0 || index >= team.size()) {
            throw new IndexOutOfBoundsException("Índice de equipo inválido");
        }
        team.set(index, pokemon);
    }

    /**
     * Añade un Pokémon al equipo si hay espacio
     */
    public void addPokemon(Pokemon pokemon) {
        if (team.size() >= 6) {
            throw new IllegalStateException("El equipo ya tiene 6 Pokémon");
        }
        team.add(pokemon);
    }

    public List<Pokemon> getteam() {
        return team;
        
    }
    /**
     * @return String representativo para debugging
     */
    @Override
    public String toString() {
        return "Trainer{name='" + name + "', team=" + team + ", items=" + items + '}';
    }
}
