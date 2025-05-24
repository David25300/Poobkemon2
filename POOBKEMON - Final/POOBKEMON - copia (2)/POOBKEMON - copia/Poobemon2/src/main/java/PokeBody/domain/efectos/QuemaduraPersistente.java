package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class QuemaduraPersistente extends Efecto {
    public QuemaduraPersistente() { 
        super("QUEMADURA_PERSISTENTE", 1.0, Integer.MAX_VALUE); 
    }
    
    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        // La quemadura estándar causa 1/16 del HP máximo como daño al final del turno.
        // En generaciones anteriores era 1/8, pero desde Gen VII es 1/16.
        // Vamos a usar 1/16.
        aplicarDanoPorcentaje(objetivo, 1.0/16.0, dispatcher, "fue herido por su quemadura");
    }
}
