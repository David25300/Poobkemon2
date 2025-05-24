package PokeBody.Services.events;

public class TurnStartEvent extends CombatEvent {
    private final int turnNumber; // Podrías añadir un contador de turnos

    public TurnStartEvent(int turnNumber) {
        super();
        this.turnNumber = turnNumber;
    }

    public int getTurnNumber() { return turnNumber; }
}
