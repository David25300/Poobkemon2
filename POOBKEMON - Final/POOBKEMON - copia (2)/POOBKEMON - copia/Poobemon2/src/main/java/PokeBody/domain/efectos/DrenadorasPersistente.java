package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.PokemonHpChangedEvent;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class DrenadorasPersistente extends Efecto {
    public DrenadorasPersistente() { 
        super("DRENADORAS_PERSISTENTE", 1.0, Integer.MAX_VALUE); 
    }
    
    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        if (objetivo == null || objetivo.estaDebilitado()) return;

        // Dañar al objetivo
        int danoCausadoAlObjetivo = Math.max(1, (int)(objetivo.getHpMax() * (1.0/8.0)));
        int hpObjetivoAntes = objetivo.getHpActual();
        objetivo.recibirDanio(danoCausadoAlObjetivo);
        
        if (dispatcher != null) {
            dispatcher.dispatchEvent(new MessageEvent(objetivo.getNombre() + " perdió " + danoCausadoAlObjetivo + " PS por las Drenadoras."));
            dispatcher.dispatchEvent(new PokemonHpChangedEvent(objetivo, objetivo.getHpActual(), objetivo.getHpMax(), hpObjetivoAntes));
        }

        // Curar a la fuente original de las Drenadoras
        Pokemon usuarioOriginalDrenadoras = objetivo.getFuenteDeDrenadoras(); 
        if (usuarioOriginalDrenadoras != null && !usuarioOriginalDrenadoras.estaDebilitado() && usuarioOriginalDrenadoras.getHpActual() < usuarioOriginalDrenadoras.getHpMax()) {
            int hpFuenteAntes = usuarioOriginalDrenadoras.getHpActual();
            int curaRealizada = Math.min(danoCausadoAlObjetivo, usuarioOriginalDrenadoras.getHpMax() - hpFuenteAntes); // No curar más allá del HP máximo
            usuarioOriginalDrenadoras.curar(curaRealizada);
            if (dispatcher != null && curaRealizada > 0) {
                dispatcher.dispatchEvent(new MessageEvent(usuarioOriginalDrenadoras.getNombre() + " recuperó " + curaRealizada + " PS."));
                dispatcher.dispatchEvent(new PokemonHpChangedEvent(usuarioOriginalDrenadoras, usuarioOriginalDrenadoras.getHpActual(), usuarioOriginalDrenadoras.getHpMax(), hpFuenteAntes));
            }
        }
    }
}
