package PokeBody.domain.efectos;

import PokeBody.Services.EffectHandler;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto; // Necesario para llamar a intentarAplicarEstadoPrimario
import PokeBody.domain.Pokemon;

public class InfligirEstado extends Efecto {
    private final String estadoAInfligir;

    public InfligirEstado(String nombreEfectoConEstado, double probabilidad, String estadoAInfligir, int duracionEstado) {
        super(nombreEfectoConEstado, probabilidad, duracionEstado); 
        this.estadoAInfligir = estadoAInfligir;
    }
    
    public String getEstadoAInfligir() { 
        return estadoAInfligir;
    }

    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        // Llama al método estático de EffectHandler para aplicar el estado.
        // Esto mantiene la lógica de inmunidades y aplicación de estado centralizada.
        EffectHandler.intentarAplicarEstadoPrimario(objetivo, estadoAInfligir, this.duracionBase, dispatcher, fuente);
    }
}