package PokeBody.Services.events;

public class MessageEvent extends CombatEvent {
    private final String message;

    public MessageEvent(String message) {
        super();
        this.message = message;
    }

    public String getMessage() { return message; }
}