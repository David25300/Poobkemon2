package PokeBody.Services.events;

import PokeBody.domain.Pokemon;

public class StatusAppliedEvent extends CombatEvent {
    private final Pokemon target;
    private final String statusName;
    private final Pokemon source; // Quién aplicó el estado (opcional)

    public StatusAppliedEvent(Pokemon target, String statusName, Pokemon source) {
        super();
        this.target = target;
        this.statusName = statusName;
        this.source = source;
    }

    public Pokemon getTarget() { return target; }
    public String getStatusName() { return statusName; }
    public Pokemon getSource() { return source; }
}