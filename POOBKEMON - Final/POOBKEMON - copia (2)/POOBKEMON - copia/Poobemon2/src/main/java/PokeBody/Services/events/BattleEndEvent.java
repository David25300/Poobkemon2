package PokeBody.Services.events;

import PokeBody.domain.Trainer;

public class BattleEndEvent extends CombatEvent {
    private final Trainer winner;
    private final Trainer loser;

    public BattleEndEvent(Trainer winner, Trainer loser) {
        super();
        this.winner = winner;
        this.loser = loser;
    }

    public Trainer getWinner() { return winner; }
    public Trainer getLoser() { return loser; }
}