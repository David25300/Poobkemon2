package PokeBody.Services;

import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;


/**
 * Encapsula el estado actual del campo de batalla.
 * Incluye los Pokémon activos, el entrenador activo, y potencialmente
 * efectos de campo, clima, etc.
 */
public class BattlefieldState {
    private Pokemon activePokemonPlayer1;
    private Pokemon activePokemonPlayer2;
    private Trainer trainerPlayer1;
    private Trainer trainerPlayer2;
    private int turnCount;
    // Podrías añadir más estados como:
    // private Weather currentWeather;
    // private Map<Equipo, List<FieldHazard>> hazards; // hazards como Stealth Rock, Spikes
    // private Map<String, Integer> fieldEffectsDuration; // ej. "Trick Room": 3

    /**
     * Constructor para BattlefieldState.
     * @param player1 El entrenador del jugador 1.
     * @param player2 El entrenador del jugador 2.
     * @param activeP1 El Pokémon activo del jugador 1.
     * @param activeP2 El Pokémon activo del jugador 2.
     */
    public BattlefieldState(Trainer player1, Trainer player2, Pokemon activeP1, Pokemon activeP2) {
        this.trainerPlayer1 = player1;
        this.trainerPlayer2 = player2;
        this.activePokemonPlayer1 = activeP1;
        this.activePokemonPlayer2 = activeP2;
        this.turnCount = 0;
        // this.hazards = new HashMap<>();
        // this.hazards.put(Equipo.JUGADOR, new ArrayList<>());
        // this.hazards.put(Equipo.RIVAL, new ArrayList<>());
        // this.fieldEffectsDuration = new HashMap<>();
    }

    // --- Getters ---
    public Pokemon getActivePokemonPlayer1() {
        return activePokemonPlayer1;
    }

    public Pokemon getActivePokemonPlayer2() {
        return activePokemonPlayer2;
    }

    public Pokemon getActivePokemonForTrainer(Trainer trainer) {
        if (trainer == trainerPlayer1) return activePokemonPlayer1;
        if (trainer == trainerPlayer2) return activePokemonPlayer2;
        return null;
    }

    public Pokemon getOpponentOf(Pokemon pokemon) {
        if (pokemon == activePokemonPlayer1) return activePokemonPlayer2;
        if (pokemon == activePokemonPlayer2) return activePokemonPlayer1;
        return null;
    }
     public Trainer getOwnerOf(Pokemon pokemon) {
        if (pokemon == activePokemonPlayer1) return trainerPlayer1;
        if (pokemon == activePokemonPlayer2) return trainerPlayer2;
        // Podrías buscar en los equipos si no es uno de los activos
        if (trainerPlayer1.getteam().contains(pokemon)) return trainerPlayer1;
        if (trainerPlayer2.getteam().contains(pokemon)) return trainerPlayer2;
        return null;
    }

    public Trainer getTrainerPlayer1() {
        return trainerPlayer1;
    }

    public Trainer getTrainerPlayer2() {
        return trainerPlayer2;
    }

    public int getTurnCount() {
        return turnCount;
    }

    // --- Setters y Modificadores ---
    public void setActivePokemonPlayer1(Pokemon pokemon) {
        this.activePokemonPlayer1 = pokemon;
    }

    public void setActivePokemonPlayer2(Pokemon pokemon) {
        this.activePokemonPlayer2 = pokemon;
    }
    public void setActivePokemonForTrainer(Trainer trainer, Pokemon pokemon) {
        if (trainer == trainerPlayer1) {
            this.activePokemonPlayer1 = pokemon;
        } else if (trainer == trainerPlayer2) {
            this.activePokemonPlayer2 = pokemon;
        }
    }
    public void incrementTurnCount() {
        this.turnCount++;
    }

    public void resetTurnCount() {
        this.turnCount = 0;
    }
    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }
}