package PokeBody.Services.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Despachador de eventos de combate.
 * Se encarga de notificar a todos los listeners registrados sobre los eventos que ocurren.
 */
public class EventDispatcher {
    private final List<CombatEventListener> listeners = new ArrayList<>();

    /**
     * Registra un nuevo listener de eventos.
     * @param listener El listener a registrar.
     */
    public void registerListener(CombatEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Elimina un listener de eventos.
     * @param listener El listener a eliminar.
     */
    public void unregisterListener(CombatEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Despacha un evento a todos los listeners registrados.
     * @param event El evento de combate a despachar.
     */
    public void dispatchEvent(CombatEvent event) {
        // Copia la lista para evitar ConcurrentModificationException si un listener se desregistra
        List<CombatEventListener> listenersCopy = new ArrayList<>(listeners);
        for (CombatEventListener listener : listenersCopy) {
            if (event instanceof BattleStartEvent) {
                listener.onBattleStart((BattleStartEvent) event);
            } else if (event instanceof BattleEndEvent) {
                listener.onBattleEnd((BattleEndEvent) event);
            } else if (event instanceof TurnStartEvent) {
                listener.onTurnStart((TurnStartEvent) event);
            } else if (event instanceof MoveUsedEvent) {
                listener.onMoveUsed((MoveUsedEvent) event);
            } else if (event instanceof PokemonFaintedEvent) {
                listener.onPokemonFainted((PokemonFaintedEvent) event);
            } else if (event instanceof PokemonHpChangedEvent) {
                listener.onPokemonHpChanged((PokemonHpChangedEvent) event);
            } else if (event instanceof StatusAppliedEvent) {
                listener.onStatusApplied((StatusAppliedEvent) event);
            } else if (event instanceof MessageEvent) {
                listener.onMessage((MessageEvent) event);
            } else if (event instanceof PokemonChangeEvent) { // Nuevo caso para el evento de cambio de Pokémon
                listener.onPokemonChange((PokemonChangeEvent) event);
            }
        }
    }
}
