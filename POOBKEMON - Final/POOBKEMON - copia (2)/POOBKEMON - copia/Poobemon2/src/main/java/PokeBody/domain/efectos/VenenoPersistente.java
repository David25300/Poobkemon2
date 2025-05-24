package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class VenenoPersistente extends Efecto {
    public VenenoPersistente() { 
        super("VENENO_PERSISTENTE", 1.0, Integer.MAX_VALUE); 
    }
    
    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        aplicarDanoPorcentaje(objetivo, 1.0/8.0, dispatcher, "fue herido por el veneno");
    }
}
