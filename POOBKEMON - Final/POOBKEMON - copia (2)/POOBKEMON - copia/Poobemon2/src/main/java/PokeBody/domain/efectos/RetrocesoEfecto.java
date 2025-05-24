package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;


public class RetrocesoEfecto extends Efecto {
    public RetrocesoEfecto(double probabilidad) {
        super("RETROCESO", probabilidad, 1); // Dura solo el turno en que se aplica
    }

    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        // El efecto de retroceso se aplica al 'objetivo' del movimiento que causa retroceso.
        if (objetivo != null && !objetivo.estaDebilitado() && !objetivo.isRetrocediendo()) {
            objetivo.setRetrocediendo(true);
            // El mensaje "¡[Pokemon] retrocedió!" se suele mostrar cuando el Pokémon intenta actuar
            // y no puede debido al retroceso. ActionExecutor.canPokemonAct se encargará de eso.
            // Aquí podríamos emitir un mensaje de que "está amedrentado" o similar si se desea.
            // dispatcher.dispatchEvent(new MessageEvent("¡" + objetivo.getNombre() + " podría retroceder!"));
        }
    }
}
