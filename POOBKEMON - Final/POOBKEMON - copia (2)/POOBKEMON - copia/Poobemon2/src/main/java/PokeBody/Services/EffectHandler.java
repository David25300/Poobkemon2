// Archivo: PokeBody/Services/EffectHandler.java
package PokeBody.Services;

import java.util.List;
import java.util.Random;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.PokemonFaintedEvent;
import PokeBody.Services.events.StatusAppliedEvent; // IMPORTADO
import PokeBody.domain.Efecto;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.Type;

/**
 * Gestiona la aplicación de efectos secundarios y estados persistentes en Pokémon.
 * Interactúa con EventDispatcher y asume que la clase Efecto también lo hace.
 */
public class EffectHandler {
    private final Random random;

    public EffectHandler() {
        this.random = new Random();
    }

    public EffectHandler(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Intenta aplicar un estado primario (VENENO, QUEMADURA, etc.) a un Pokémon objetivo.
     * Emite eventos a través del EventDispatcher.
     *
     * @param objetivo El Pokémon que podría recibir el estado.
     * @param nombreEstadoEl El nombre del estado a aplicar.
     * @param duracion La duración del estado.
     * @param dispatcher El EventDispatcher para emitir eventos.
     * @param source El Pokémon que origina el estado (puede ser null).
     * @return true si el estado fue aplicado exitosamente, false en caso contrario.
     */
    public static boolean intentarAplicarEstadoPrimario(Pokemon objetivo, String nombreEstadoEl, int duracion, EventDispatcher dispatcher, Pokemon source) {
        if (objetivo == null || objetivo.estaDebilitado() || nombreEstadoEl == null) {
            return false;
        }
        String nombreEstado = nombreEstadoEl.toUpperCase();

        if (objetivo.getEstado() != null && !objetivo.getEstado().isEmpty()) {
            if (dispatcher != null) {
                dispatcher.dispatchEvent(new MessageEvent(objetivo.getNombre() + " ya tiene el estado " + objetivo.getEstado() + " y no puede ser afectado por " + nombreEstado + "."));
            }
            return false;
        }

        if (esInmuneAEstado(objetivo, nombreEstado)) {
            if (dispatcher != null) {
                dispatcher.dispatchEvent(new MessageEvent("¡A " + objetivo.getNombre() + " no le afecta " + nombreEstado + "!"));
            }
            return false;
        }

        objetivo.setEstadoActivo(nombreEstado, duracion);
        if ("DRENADORAS".equalsIgnoreCase(nombreEstado) && source != null) {
            objetivo.setFuenteDeDrenadoras(source);
        }

        if (dispatcher != null) {
            dispatcher.dispatchEvent(new StatusAppliedEvent(objetivo, nombreEstado, source));
        }
        
        if ("QUEMADURA".equals(nombreEstado)) {
            if (!objetivo.tieneModificadorTemporal("QUEMADURA_ATAQUE_REDUCIDO")) {
                objetivo.marcarModificadorTemporal("QUEMADURA_ATAQUE_REDUCIDO");
            }
        }
        return true;
    }

    /**
     * Verifica si un Pokémon es inmune a un estado específico basado en su tipo.
     * @param pokemon El Pokémon a verificar.
     * @param nombreEstado El nombre del estado.
     * @return true si es inmune, false en caso contrario.
     */
    public static boolean esInmuneAEstado(Pokemon pokemon, String nombreEstado) {
        if (pokemon == null || nombreEstado == null) return true; 

        String estadoUpper = nombreEstado.toUpperCase();
        List<Type.Tipo> tiposPokemon = pokemon.getTipos();
        if (tiposPokemon == null) return false;

        switch (estadoUpper) {
            case "VENENO":
            case "TOXICO":
                return tiposPokemon.contains(Type.Tipo.VENENO) || tiposPokemon.contains(Type.Tipo.ACERO);
            case "QUEMADURA":
                return tiposPokemon.contains(Type.Tipo.FUEGO);
            case "CONGELADO":
                return tiposPokemon.contains(Type.Tipo.HIELO); 
            case "PARALIZADO":
                // Los Pokémon de tipo Eléctrico son inmunes a la parálisis desde Gen VI.
                // Asumiendo que esta regla aplica.
                return tiposPokemon.contains(Type.Tipo.ELECTRICO);
            default: 
                return false; 
        }
    }

    /**
     * Aplica los efectos de estado persistentes al final del turno para un Pokémon.
     *
     * @param pokemon El Pokémon cuyos efectos persistentes se aplicarán.
     * @param currentState El estado actual del campo de batalla (para obtener el dueño si se debilita).
     * @param dispatcher El EventDispatcher para emitir eventos.
     */
    public void aplicarEfectosPersistentesDeFinDeTurno(Pokemon pokemon, BattlefieldState currentState, EventDispatcher dispatcher) {
        if (pokemon == null || pokemon.estaDebilitado() || pokemon.getEstado() == null || pokemon.getEstado().trim().isEmpty()) {
            return;
        }

        String nombreEstado = pokemon.getEstado();
        Efecto efectoPersistente = Efecto.forEstadoPersistente(nombreEstado);

        if (efectoPersistente != null) {
            if ("TOXICO".equals(nombreEstado.toUpperCase())) {
                pokemon.incrementarContadorToxic();
            }
            
            Pokemon fuenteDelEfecto = null;
            if ("DRENADORAS".equalsIgnoreCase(nombreEstado.toUpperCase())) {
                fuenteDelEfecto = pokemon.getFuenteDeDrenadoras();
            }
            
            // Guardar HP antes de aplicar el efecto persistente
            int hpAntesDelEfecto = pokemon.getHpActual();
            
            efectoPersistente.ejecutar(pokemon, fuenteDelEfecto, dispatcher); // El efecto ya despacha su propio PokemonHpChangedEvent si modifica HP

            // Si el efecto persistente no despachó un evento de cambio de HP (porque la lógica está en EfectoHandler),
            // y el HP cambió, despacharlo aquí.
            // Sin embargo, la lógica actual en Efecto.java (aplicarDanoPorcentaje, curarPorcentaje) ya lo hace.
            // Así que este bloque podría ser redundante si la estructura de Efecto se mantiene.
            // if (pokemon.getHpActual() != hpAntesDelEfecto && dispatcher != null) {
            //    dispatcher.dispatchEvent(new PokemonHpChangedEvent(pokemon, pokemon.getHpActual(), pokemon.getHpMax(), hpAntesDelEfecto));
            // }


            if (pokemon.estaDebilitado()) {
                Trainer owner = currentState.getOwnerOf(pokemon);
                if (dispatcher != null && owner != null) { 
                    dispatcher.dispatchEvent(new PokemonFaintedEvent(pokemon, owner));
                } else if (dispatcher != null) {
                    dispatcher.dispatchEvent(new MessageEvent(pokemon.getNombre() + " se debilitó por el efecto de estado, pero no se pudo determinar el dueño."));
                }
            }
        }
    }


    /**
     * Intenta aplicar el efecto secundario de un movimiento al objetivo.
     *
     * @param movimiento El movimiento con el posible efecto secundario.
     * @param usuario El Pokémon que usa el movimiento.
     * @param objetivo El Pokémon objetivo del movimiento.
     * @param dispatcher El EventDispatcher para emitir eventos.
     * @return true si el efecto secundario se activó y aplicó, false en caso contrario.
     */
    public boolean aplicarEfectoSecundarioDeMovimiento(Movements movimiento, Pokemon usuario, Pokemon objetivo, EventDispatcher dispatcher) {
        if (movimiento == null || (objetivo != null && objetivo.estaDebilitado())) {
            return false;
        }

        Efecto efectoDelMovimiento = movimiento.getEfecto();
        if (efectoDelMovimiento != null) {
            // Guardar HP del objetivo ANTES de aplicar el efecto secundario, si el efecto puede cambiar HP
            int hpObjetivoAntes = (objetivo != null) ? objetivo.getHpActual() : 0;
            int hpUsuarioAntes = (usuario != null) ? usuario.getHpActual() : 0;

            boolean efectoAplicado = efectoDelMovimiento.intentarAplicar(objetivo, usuario, dispatcher);

            // Si el efecto aplicado fue de tipo InfligirEstado, y se aplicó,
            // el StatusAppliedEvent ya debería haber sido emitido desde Efecto.InfligirEstado.ejecutar
            // (a través de la llamada a EffectHandler.intentarAplicarEstadoPrimario).

            // Verificar si el HP del objetivo cambió y despachar evento si es necesario
            // (si el propio efecto no lo hizo ya)
            if (efectoAplicado && objetivo != null && objetivo.getHpActual() != hpObjetivoAntes && dispatcher != null) {
                 // Esto podría ser redundante si el efecto específico (ej. Drenadoras desde un movimiento) ya despacha su propio evento de HP.
                 // Se necesita cuidado para no duplicar eventos.
                 // Por ahora, se asume que los efectos directos de HP como absorber vida se manejan en ActionExecutor.
                 // Los efectos de estado que causan daño (veneno, quemadura) se manejan en aplicarEfectosPersistentes.
                 // Los cambios de stat no afectan HP directamente.
            }
            // Similar para el usuario si el efecto es sobre el usuario y cambia HP.

            return efectoAplicado;
        }
        return false;
    }
}
