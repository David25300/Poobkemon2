package PokeBody.Services;

import java.util.ArrayList;
import java.util.List;

import PokeBody.domain.Movements; // For checking team status
import PokeBody.domain.Pokemon;

/**
 * Manages the Pokémon teams during combat.
 * Handles active Pokémon, switching, and checks team status (fainted, defeated).
 */
public class TeamManager {

    /** Enumerator to identify teams */
    public enum Equipo {
        JUGADOR, RIVAL
    }

    private final List<Pokemon> equipoJugador;
    private final List<Pokemon> equipoRival;
    private int indiceActivoJugador;
    private int indiceActivoRival;
    // Redundant fields, use indices to get active Pokemon directly from lists
    // private Pokemon activoJugador;
    // private Pokemon activoRival;

    /** Maximum number of Pokémon per team */
    private static final int MAX_POKEMON_EQUIPO = 6;

    /**
     * Constructor initializing the team manager with the specified teams.
     * Sets the first non-fainted Pokémon as active for each team.
     *
     * @param equipoJugador Player's Pokémon list.
     * @param equipoRival   Rival's Pokémon list.
     * @throws IllegalArgumentException if any team is null, empty, or exceeds the size limit.
     */
    public TeamManager(List<Pokemon> equipoJugador, List<Pokemon> equipoRival) {
        validarEquipo(equipoJugador, "Jugador");
        validarEquipo(equipoRival, "Rival");

        // Use defensive copies to prevent external modification
        this.equipoJugador = new ArrayList<>(equipoJugador);
        this.equipoRival = new ArrayList<>(equipoRival);

        // Find the first available Pokémon index for each team
        this.indiceActivoJugador = findNextAvailablePokemonIndex(this.equipoJugador, -1);
        this.indiceActivoRival = findNextAvailablePokemonIndex(this.equipoRival, -1);

        // Validate that at least one Pokémon is available on each team initially
        if (this.indiceActivoJugador == -1) {
            throw new IllegalStateException("El equipo del Jugador no tiene Pokémon disponibles al inicio.");
        }
        if (this.indiceActivoRival == -1) {
             throw new IllegalStateException("El equipo del Rival no tiene Pokémon disponibles al inicio.");
        }

        // Initialize active Pokemon references (optional, can use getPokemonActivo directly)
        // this.activoJugador = this.equipoJugador.get(this.indiceActivoJugador);
        // this.activoRival = this.equipoRival.get(this.indiceActivoRival);
    }

    /**
     * Validates that a team meets the established rules (not null, not empty, size limit).
     *
     * @param equipo The team list to validate.
     * @param teamName The name of the team for error messages.
     * @throws IllegalArgumentException if the team does not meet the rules.
     */
    private void validarEquipo(List<Pokemon> equipo, String teamName) {
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo de " + teamName + " no puede ser nulo.");
        }
        if (equipo.isEmpty()) {
            throw new IllegalArgumentException("El equipo de " + teamName + " no puede estar vacío.");
        }
        if (equipo.size() > MAX_POKEMON_EQUIPO) {
            throw new IllegalArgumentException("El equipo de " + teamName + " excede el límite de " + MAX_POKEMON_EQUIPO + " Pokémon.");
        }
        // Optional: Check for null Pokemon within the list
        for (int i = 0; i < equipo.size(); i++) {
            if (equipo.get(i) == null) {
                 throw new IllegalArgumentException("El equipo de " + teamName + " contiene un Pokémon nulo en el índice " + i + ".");
            }
        }
    }

    /**
     * Gets the currently active Pokémon for the specified team.
     *
     * @param equipo The team identifier (JUGADOR or RIVAL).
     * @return The active Pokémon, or null if the team is somehow invalid (should not happen with constructor validation).
     */
    public Pokemon getPokemonActivo(Equipo equipo) {
        List<Pokemon> equipoActual = (equipo == Equipo.JUGADOR) ? equipoJugador : equipoRival;
        int indiceActual = (equipo == Equipo.JUGADOR) ? indiceActivoJugador : indiceActivoRival;

        // Check if index is valid (should be guaranteed by logic, but good practice)
        if (indiceActual >= 0 && indiceActual < equipoActual.size()) {
            return equipoActual.get(indiceActual);
        } else {
            // This indicates a serious logic error if reached
            System.err.println("Error: Índice de Pokémon activo inválido para " + equipo + ": " + indiceActual);
            return null;
        }
    }

    // Redundant getters removed: getPokemonActivoJugador(), getPokemonActivoRival()
    // Use getPokemonActivo(Equipo.JUGADOR) and getPokemonActivo(Equipo.RIVAL) instead.

    /**
     * Handles the situation where one or both active Pokémon faint at the end of a turn.
     * Prompts for or automatically selects the next Pokémon if necessary.
     * Note: The actual switching logic (player choice, AI choice) should be handled
     * by the CombatManager or UI interaction based on the results here.
     *
     * @return true if a switch is required for either player, false otherwise.
     */
    public boolean manejarPokemonDebilitados() {
        boolean switchNeededJugador = false;
        boolean switchNeededRival = false;

        Pokemon activoJugador = getPokemonActivo(Equipo.JUGADOR);
        if (activoJugador != null && activoJugador.estaDebilitado()) {
            // Player's active Pokemon fainted. Check if replacements are available.
            if (hayPokemonDisponibles(Equipo.JUGADOR)) {
                 switchNeededJugador = true;
                 // The actual switch action (choosing the next Pokemon) happens outside this method.
                 System.out.println(activoJugador.getNombre() + " del Jugador se ha debilitado! Se necesita cambio.");
            } else {
                 // Player has no more Pokemon, game might end.
                 System.out.println(activoJugador.getNombre() + " del Jugador se ha debilitado! No quedan más Pokémon.");
            }
        }

        Pokemon activoRival = getPokemonActivo(Equipo.RIVAL);
        if (activoRival != null && activoRival.estaDebilitado()) {
             // Rival's active Pokemon fainted. Check if replacements are available.
             if (hayPokemonDisponibles(Equipo.RIVAL)) {
                 switchNeededRival = true;
                 // The actual switch action (AI/Player 2 choice) happens outside this method.
                 System.out.println(activoRival.getNombre() + " del Rival se ha debilitado! Se necesita cambio.");
            } else {
                 // Rival has no more Pokemon, game might end.
                 System.out.println(activoRival.getNombre() + " del Rival se ha debilitado! No quedan más Pokémon.");
            }
        }
        return switchNeededJugador || switchNeededRival;
    }


    /**
     * Checks if a specific Pokémon is fainted (HP <= 0).
     *
     * @param pokemon The Pokémon to check.
     * @return true if fainted, false otherwise. Returns true if pokemon is null.
     */
    // Method `estaDebilitado(Pokemon)` is redundant as Pokemon class has it. Keep for clarity if needed.
    // public boolean estaDebilitado(Pokemon pokemon) {
    //     return pokemon == null || pokemon.estaDebilitado();
    // }


    /**
     * Finds the index of the next available (non-fainted) Pokémon in a team,
     * starting the search after the current index. Returns -1 if none are found.
     *
     * @param equipoActual The list of Pokémon for the team.
     * @param indiceActual The index of the currently fainted Pokémon (-1 if starting fresh).
     * @return The index of the next available Pokémon, or -1 if none exist.
     */
    private int findNextAvailablePokemonIndex(List<Pokemon> equipoActual, int indiceActual) {
        if (equipoActual == null || equipoActual.isEmpty()) {
            return -1;
        }
        int teamSize = equipoActual.size();
        // Start checking from the next index, wrapping around
        for (int i = 1; i <= teamSize; i++) {
            int checkIndex = (indiceActual + i) % teamSize;
            Pokemon p = equipoActual.get(checkIndex);
            if (p != null && !p.estaDebilitado()) {
                return checkIndex; // Found the next available Pokemon
            }
        }
        return -1; // No available Pokemon found
    }


    /**
     * Switches the active Pokémon for a team to the one at the specified index.
     * Validates the index and checks if the target Pokémon is fainted.
     * Resets boosts and volatile statuses of the outgoing Pokémon.
     *
     * @param equipo The team identifier (JUGADOR or RIVAL).
     * @param indice The index (0-based) of the Pokémon to switch in.
     * @return true if the switch was successful, false otherwise.
     */
    public boolean cambiarPokemonAIndice(Equipo equipo, int indice) {
        List<Pokemon> equipoActual = (equipo == Equipo.JUGADOR) ? equipoJugador : equipoRival;
        int indiceActivoActual = (equipo == Equipo.JUGADOR) ? indiceActivoJugador : indiceActivoRival;

        // Validate index
        if (indice < 0 || indice >= equipoActual.size()) {
            System.err.println("Error de cambio: Índice " + indice + " fuera de rango para equipo " + equipo);
            return false;
        }

        // Cannot switch to the same Pokémon
        if (indice == indiceActivoActual) {
             System.out.println("Intento de cambio al mismo Pokémon (" + indice + ") para equipo " + equipo);
            return false; // Or maybe allow it depending on game rules? Usually not.
        }

        Pokemon pokemonEntrante = equipoActual.get(indice);

        // Cannot switch to a fainted Pokémon
        if (pokemonEntrante == null || pokemonEntrante.estaDebilitado()) {
             System.out.println("Error de cambio: Pokémon en índice " + indice + " está debilitado o es nulo para equipo " + equipo);
            return false;
        }

        // --- Perform the switch ---
        Pokemon pokemonSaliente = getPokemonActivo(equipo);

        // Reset boosts of the outgoing Pokemon (if it's not fainted)
        if (pokemonSaliente != null && !pokemonSaliente.estaDebilitado()) {
             pokemonSaliente.resetearBoosts();
             // TODO: Reset volatile statuses like confusion, flinch, etc.
             System.out.println(pokemonSaliente.getNombre() + " regresa.");
        }

        // Update the active index
        if (equipo == Equipo.JUGADOR) {
            indiceActivoJugador = indice;
        } else {
            indiceActivoRival = indice;
        }

        // Optional: Reset boosts/volatile status for the incoming Pokemon? Usually not needed unless specified.
        System.out.println("¡Adelante, " + pokemonEntrante.getNombre() + "!");

        return true;
    }

    /**
     * Checks if the combat has ended (one team has no usable Pokémon left).
     *
     * @return true if the combat is over, false otherwise.
     */
    public boolean estaCombateTerminado() {
        return estaEquipoDerrotado(Equipo.JUGADOR) || estaEquipoDerrotado(Equipo.RIVAL);
    }

    /**
     * Checks if a team is completely defeated (all Pokémon fainted).
     *
     * @param equipo The team identifier (JUGADOR or RIVAL).
     * @return true if all Pokémon on the team are fainted, false otherwise.
     */
    public boolean estaEquipoDerrotado(Equipo equipo) {
        return !hayPokemonDisponibles(equipo);
    }

    /**
     * Checks if there is at least one non-fainted Pokémon on the specified team.
     *
     * @param equipo The team identifier (JUGADOR or RIVAL).
     * @return true if at least one Pokémon can still fight, false otherwise.
     */
    public boolean hayPokemonDisponibles(Equipo equipo) {
        List<Pokemon> equipoActual = (equipo == Equipo.JUGADOR) ? equipoJugador : equipoRival;
        if (equipoActual == null) return false; // Should not happen
        for (Pokemon pokemon : equipoActual) {
            if (pokemon != null && !pokemon.estaDebilitado()) {
                return true; // Found at least one available Pokemon
            }
        }
        return false; // All Pokemon are fainted or list is empty/invalid
    }


    /**
     * Resets the state of Pokémon in both teams at the beginning of a combat.
     * Heals HP, restores PP, clears status, and resets boosts.
     * Also ensures the initial active Pokémon are set correctly.
     */
    public void resetearEquiposParaNuevoCombate() {
        System.out.println("Reseteando equipos para nuevo combate...");
        for (Pokemon pokemon : equipoJugador) {
            if (pokemon != null) {
                pokemon.setHpActual(pokemon.getHpMax()); // Full heal
                pokemon.clearStatusEffects();            // Clear status
                pokemon.resetearBoosts();                // Reset boosts
                if(pokemon.getMovimientos() != null) {   // Restore PP
                    for(Movements move : pokemon.getMovimientos()) {
                        if (move != null) move.restaurarPP();
                    }
                }
            }
        }

        for (Pokemon pokemon : equipoRival) {
             if (pokemon != null) {
                pokemon.setHpActual(pokemon.getHpMax());
                // Corrected method call: clearStatusEffects()
                pokemon.clearStatusEffects();
                pokemon.resetearBoosts();
                 if(pokemon.getMovimientos() != null) {
                    for(Movements move : pokemon.getMovimientos()) {
                        if (move != null) move.restaurarPP();
                    }
                }
            }
        }

        // Reset active indices to the first available Pokemon
        indiceActivoJugador = findNextAvailablePokemonIndex(equipoJugador, -1);
        indiceActivoRival = findNextAvailablePokemonIndex(equipoRival, -1);

         // Validate again after reset
        if (this.indiceActivoJugador == -1) {
            throw new IllegalStateException("El equipo del Jugador no tiene Pokémon disponibles después del reseteo.");
        }
        if (this.indiceActivoRival == -1) {
             throw new IllegalStateException("El equipo del Rival no tiene Pokémon disponibles después del reseteo.");
        }

        System.out.println("Equipos reseteados. Jugador activo: " + getPokemonActivo(Equipo.JUGADOR).getNombre() + ", Rival activo: " + getPokemonActivo(Equipo.RIVAL).getNombre());
    }

    // --- Getters for Team Lists (Defensive Copies) ---

    /**
     * Gets the player's team list.
     *
     * @return A defensive copy of the player's team list.
     */
    public List<Pokemon> getEquipoJugador() {
        return new ArrayList<>(equipoJugador);
    }

    /**
     * Gets the rival's team list.
     *
     * @return A defensive copy of the rival's team list.
     */
    public List<Pokemon> getEquipoRival() {
        return new ArrayList<>(equipoRival);
    }

    /**
     * Gets the full team list for the specified team identifier.
     * Use with caution, returns the internal list reference.
     * Prefer getEquipoJugador() or getEquipoRival() for defensive copies.
     *
     * @param equipo The team identifier (JUGADOR or RIVAL).
     * @return The internal list of Pokémon for that team.
     */
    public List<Pokemon> getEquipoInterno(Equipo equipo) { // Renamed for clarity
        return (equipo == Equipo.JUGADOR)
            ? equipoJugador
            : equipoRival;
    }
}
