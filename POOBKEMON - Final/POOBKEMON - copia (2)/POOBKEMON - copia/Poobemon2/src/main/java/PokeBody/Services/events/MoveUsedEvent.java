package PokeBody.Services.events;

import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;

public class MoveUsedEvent extends CombatEvent {
    private final Pokemon attacker;
    private final Pokemon target; // Podría ser una lista para movimientos multi-objetivo
    private final Movements move;
    private final int damageDealt;
    private final boolean wasCritical;
    private final String additionalMessage; // Para efectividad, fallo, etc.

    public MoveUsedEvent(Pokemon attacker, Pokemon target, Movements move, int damageDealt, boolean wasCritical, String additionalMessage) {
        super();
        this.attacker = attacker;
        this.target = target;
        this.move = move;
        this.damageDealt = damageDealt;
        this.wasCritical = wasCritical;
        this.additionalMessage = additionalMessage;
    }

    public Pokemon getAttacker() { return attacker; }
    public Pokemon getTarget() { return target; }
    public Movements getMove() { return move; }
    public int getDamageDealt() { return damageDealt; }
    public boolean wasCritical() { return wasCritical; }
    public String getAdditionalMessage() { return additionalMessage; }
}
