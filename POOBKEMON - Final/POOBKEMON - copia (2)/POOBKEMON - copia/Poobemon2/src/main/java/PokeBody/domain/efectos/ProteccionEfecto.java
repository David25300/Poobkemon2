package PokeBody.domain.efectos;

import java.util.Random;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class ProteccionEfecto extends Efecto {
    private static final Random RND_PROTECT_EFFECT = new Random();

    public ProteccionEfecto() {
        super("PROTECCION", 1.0, 1); 
    }

    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        if (fuente == null || fuente.estaDebilitado()) {
            if (dispatcher != null && fuente != null) dispatcher.dispatchEvent(new MessageEvent(fuente.getNombre() + " está debilitado y no puede protegerse."));
            return;
        }

        double successChance = 1.0;
        // La probabilidad de éxito disminuye con usos consecutivos.
        // Ejemplo: 1er uso: 100%, 2do: 50%, 3ro: 25%, etc.
        if (fuente.getConsecutiveProtectUses() > 0) {
            successChance = Math.pow(0.5, fuente.getConsecutiveProtectUses());
        }
        
        if (RND_PROTECT_EFFECT.nextDouble() < successChance) {
            fuente.setProtectedThisTurn(true);
            fuente.incrementConsecutiveProtectUses(); // Método debe existir en Pokemon.java
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("¡" + fuente.getNombre() + " se está protegiendo!"));
        } else {
            fuente.setProtectedThisTurn(false); 
            fuente.resetConsecutiveProtectUses(); // Método debe existir en Pokemon.java
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("¡Pero " + fuente.getNombre() + " falló al intentar protegerse!"));
        }
    }
}
 