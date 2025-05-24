package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class ToxicPersistente extends Efecto {
    public ToxicPersistente() { 
        // La duración inicial se establece en 1 para el primer turno de daño.
        // El contador de turnos de tóxico se maneja en Pokemon.incrementarContadorToxic().
        super("TOXICO_PERSISTENTE", 1.0, 1); 
    }
    
    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        if (objetivo == null || objetivo.estaDebilitado()) return;
        
        // El contador de turnos para Tóxico (almacenado en Pokemon.duracionEstado para este estado)
        // se incrementa antes de calcular el daño en EffectHandler o BattlePhaseManager.
        int contadorToxic = objetivo.getDuracionEstado(); // Este es N (1 para el primer turno de daño, 2 para el segundo, etc.)
        
        // Daño es N/16 del HP máximo.
        aplicarDanoPorcentaje(objetivo, (double)contadorToxic / 16.0, dispatcher, "fue gravemente herido por el veneno");
    }
}
