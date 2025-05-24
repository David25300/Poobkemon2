package PokeBody.Services.GameModes;

import PokeBody.Services.BattlefieldState;
import PokeBody.Services.CombatManager;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Trainer;

public interface GameMode {
    void initialize(Trainer playerTrainer, CombatManager combatManager, EventDispatcher eventDispatcher, BattlefieldState battlefieldState);

    /**
     * Called when the player wins a battle within this game mode.
     * @param player The winning player trainer.
     * @param defeatedOpponent The defeated opponent trainer.
     */
    void onPlayerWinBattle(Trainer player, Trainer defeatedOpponent);

    /**
     * Called when the player loses a battle within this game mode.
     * @param defeatedPlayer The defeated player trainer.
     * @param winnerOpponent The winning opponent trainer.
     */
    void onPlayerLoseBattle(Trainer defeatedPlayer, Trainer winnerOpponent);

    /**
     * Called when a battle within this game mode results in a draw.
     * @param player The player trainer.
     * @param opponent The opponent trainer.
     */
    void onPlayerDraw(Trainer player, Trainer opponent);

    String getModeName();

    /**
     * Gets the initial AI opponent for this game mode.
     * The playerTrainer is provided to potentially scale the opponent or make decisions based on the player's team.
     * @param playerTrainer The player's trainer.
     * @return The initial AI opponent.
     */
    Trainer getInitialOpponent(Trainer playerTrainer);

    /**
     * Optional: Called when the game setup manager is returning to the main menu.
     * Allows the game mode to perform any cleanup if necessary.
     */
    default void onReturnToMenu() {
        // Default implementation does nothing.
    }
}
