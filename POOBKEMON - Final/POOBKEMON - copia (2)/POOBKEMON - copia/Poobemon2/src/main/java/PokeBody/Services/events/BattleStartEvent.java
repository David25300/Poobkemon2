package PokeBody.Services.events;

import PokeBody.domain.Trainer;

public class BattleStartEvent extends CombatEvent {
    private final Trainer player1;
    private final Trainer player2;

    public BattleStartEvent(Trainer player1, Trainer player2) {
        super();
        this.player1 = player1;
        this.player2 = player2;
    }

    public Trainer getPlayer1() { return player1; }
    public Trainer getPlayer2() { return player2; }
}