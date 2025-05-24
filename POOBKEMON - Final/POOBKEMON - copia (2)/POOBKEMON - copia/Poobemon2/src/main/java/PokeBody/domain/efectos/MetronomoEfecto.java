package PokeBody.domain.efectos;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Efecto;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;

public class MetronomoEfecto extends Efecto {
    private static final Random RND_METRONOME_EFFECT = new Random(); 
    private Movements chosenMoveByMetronome = null; 

    public MetronomoEfecto() {
        super("METRONOMO", 1.0, 0);
    }

    /**
     * Selecciona un movimiento aleatorio del juego y lo almacena.
     * Este método es llamado por ActionExecutor.
     * @param fuente El Pokémon que usa Metrónomo.
     * @param dispatcher El despachador de eventos.
     * @param allGameMoves Mapa de todos los movimientos disponibles en el juego.
     */
    public void seleccionarMovimientoAleatorio(Pokemon fuente, EventDispatcher dispatcher, Map<String, Movements> allGameMoves) {
        this.chosenMoveByMetronome = null; 
        if (fuente == null || fuente.estaDebilitado()) {
            if (dispatcher != null && fuente != null) dispatcher.dispatchEvent(new MessageEvent(fuente.getNombre() + " está debilitado y no puede usar Metrónomo."));
            return;
        }
        if (allGameMoves == null || allGameMoves.isEmpty()) {
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("Error: No hay movimientos disponibles para Metrónomo."));
            System.err.println("MetronomoEfecto: allGameMoves es nulo o vacío.");
            this.chosenMoveByMetronome = Movements.FUERZAGEO; // Fallback a Forcejeo
            return;
        }

        // Lista de nombres de movimientos a excluir
        List<String> excludedMoveNames = List.of(
            "Metrónomo", "Forcejeo", "Transformación", "Esquema", "Ayuda", "Canto Mortal", 
            "Contraataque", "Manto Espejo", "Movimiento Espejo", // Manto Espejo también
            "Protección", "Detección", "Aguante", "Amago", "Anticipo", "Yo Primero", 
            "Robo", "Truco", "Cede Paso", "Manos Juntas", "Imitación", "Conversión", "Conversión2",
            "Voto Fuego", "Voto Planta", "Voto Agua", 
            "Autodestrucción", "Explosión", // A menudo excluidos
            // Movimientos de dos turnos (pueden ser problemáticos si no se maneja el estado de carga)
            "Vuelo", "Excavar", "Buceo", "Caída Libre", "Golpe Fantasma", "Ataque Aéreo", 
            "Rayo Solar", "Geocontrol", "Rayo Meteórico", "Cabezazo Zen", // Cabezazo Zen no es de dos turnos
            // Movimientos Z y Max (si los tuvieras)
            "Gigavoltio Destructor", "Hidrovórtice Abisal" // Ejemplos
            // Otros movimientos que no tienen sentido o rompen el juego si son llamados por Metrónomo
        );

        List<Movements> elegibleMoves = allGameMoves.values().stream()
            .filter(move -> move != null && 
                             !excludedMoveNames.contains(move.getNombre()) &&
                             !move.getNombre().startsWith("Voto ") && // Excluir todos los Voto
                             !(move.getPotencia() == 0 && move.getEfecto() == null && move.getBoostAmount() == 0 && move.getBoostStat() == null) // Evitar movimientos de estado completamente vacíos
            )
            .collect(Collectors.toList());


        if (elegibleMoves.isEmpty()) {
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("¡Metrónomo no pudo seleccionar ningún movimiento!"));
            this.chosenMoveByMetronome = Movements.FUERZAGEO; 
        } else {
            Movements templateMove = elegibleMoves.get(RND_METRONOME_EFFECT.nextInt(elegibleMoves.size()));
            this.chosenMoveByMetronome = new Movements(templateMove); 
            // El PP del movimiento elegido por Metrónomo no se consume del original del Pokémon,
            // Metrónomo mismo consume su PP.
            // El PP del movimiento elegido se considera "lleno" para este uso.
            this.chosenMoveByMetronome.setPpActual(this.chosenMoveByMetronome.getPpMax()); 
        }
    }
    
    public Movements getChosenMoveByMetronome() {
        return this.chosenMoveByMetronome;
    }
    
    public void clearChosenMove() {
        this.chosenMoveByMetronome = null;
    }

    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        // Este método es llamado por ActionExecutor si el efecto se aplica como un efecto secundario normal.
        // Para Metrónomo, ActionExecutor debe llamar a seleccionarMovimientoAleatorio y luego getChosenMoveByMetronome.
        // Este método puede quedar vacío o loguear un mensaje.
         if (fuente != null && !fuente.estaDebilitado() && dispatcher != null) {
            // El mensaje de "usó Metrónomo" se emite desde ActionExecutor antes de llamar a seleccionarMovimientoAleatorio.
         }
    }
}
