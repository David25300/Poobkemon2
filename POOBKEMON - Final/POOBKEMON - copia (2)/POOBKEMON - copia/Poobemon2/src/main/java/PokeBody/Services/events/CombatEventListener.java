package PokeBody.Services.events;

public interface CombatEventListener {
    void onBattleStart(BattleStartEvent event);
    void onBattleEnd(BattleEndEvent event);
    void onTurnStart(TurnStartEvent event);
    void onMoveUsed(MoveUsedEvent event);
    void onPokemonFainted(PokemonFaintedEvent event);
    void onPokemonHpChanged(PokemonHpChangedEvent event);
    void onStatusApplied(StatusAppliedEvent event);
    void onMessage(MessageEvent event);

    /**
     * Se llama cuando un Pokémon es cambiado durante el combate.
     * La implementación por defecto no hace nada, para que las clases existentes
     * no se vean obligadas a implementarlo si no lo necesitan.
     * @param event El evento de cambio de Pokémon.
     */
    default void onPokemonChange(PokemonChangeEvent event) {
    }
}
