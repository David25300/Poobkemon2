package PokeBody.Services.events;

/**
 * Clase base abstracta para todos los eventos de combate.
 */
public abstract class CombatEvent {
    private final long timestamp;

    public CombatEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Podrías añadir un tipo de evento si es necesario
    // public abstract String getEventType();
}