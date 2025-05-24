package PokeBody.domain.efectos;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.domain.Efecto;
import PokeBody.domain.Pokemon; // Asegúrate que StatBoosts esté importado o accesible
import PokeBody.domain.StatBoosts.Stat;

public class ModificarStatEfecto extends Efecto {
    private final Stat statAfectada;
    private final int niveles; // Cantidad de niveles que sube o baja (positivo para subir, negativo para bajar)
    private final boolean esAlUsuario; // true si el efecto es sobre el usuario del movimiento, false si es sobre el objetivo

    public ModificarStatEfecto(String nombreEfecto, double probabilidad, Stat stat, int niveles, boolean esAlUsuario) {
        super(nombreEfecto, probabilidad, 0); // Duración 0 porque el cambio de stat es inmediato
        this.statAfectada = stat;
        this.niveles = niveles;
        this.esAlUsuario = esAlUsuario;
    }

    @Override
    public void ejecutar(Pokemon objetivoDelMovimiento, Pokemon fuenteDelMovimiento, EventDispatcher dispatcher) {
        Pokemon targetPokemonParaStatChange = esAlUsuario ? fuenteDelMovimiento : objetivoDelMovimiento;
        
        if (targetPokemonParaStatChange != null && !targetPokemonParaStatChange.estaDebilitado()) {
            // Usar el método modificarStat de la clase base Efecto
            // El tercer argumento 'niveles' ya tiene el signo correcto.
            modificarStat(targetPokemonParaStatChange, statAfectada, niveles, dispatcher, (targetPokemonParaStatChange == fuenteDelMovimiento));
        }
    }
    public Stat getStatAfectada() {
        return statAfectada;
    }

    public int getNiveles() {
        return niveles;
    }

    public boolean esAlUsuario() {
        return esAlUsuario;
    }
}