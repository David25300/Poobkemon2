package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon;

public class AbsorberVidaEfecto extends Efecto {
    private final double porcentajeDanoAbsorbido;

    public AbsorberVidaEfecto(String nombreEfecto, double porcentaje) {
        super(nombreEfecto, 1.0, 0); // Probabilidad 1.0 porque si el movimiento golpea, el efecto de absorber ocurre
        this.porcentajeDanoAbsorbido = porcentaje;
    }

    @Override
    public void ejecutar(Pokemon objetivoDelMovimiento, Pokemon fuenteDelMovimiento, EventDispatcher dispatcher) {
        // La lógica de curación basada en el daño infligido
        // se maneja en ActionExecutor.ejecutarAccionesDeAtaque,
        // después de calcular el daño.
        // Este efecto sirve más como una "marca" para que ActionExecutor sepa que debe aplicar la curación.
        // No hay una acción directa aquí, ya que depende del daño causado por el movimiento.
    }

    public double getPorcentajeDanoAbsorbido() { 
        return porcentajeDanoAbsorbido;
    }
}