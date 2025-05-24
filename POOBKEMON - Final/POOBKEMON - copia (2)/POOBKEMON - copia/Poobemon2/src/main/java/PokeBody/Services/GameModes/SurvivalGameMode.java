package PokeBody.Services.GameModes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import Face.SwingGUI;
import PokeBody.Data.DataLoader;
import PokeBody.Data.PokemonData;
import PokeBody.Services.BattlefieldState;
import PokeBody.Services.CombatManager;
import PokeBody.Services.GameSetupManager;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent; // Necesario para acceder a datos globales como allMovesMap
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class SurvivalGameMode implements GameMode {
    private Trainer entrenadorJugador;
    // oponenteActual ya no es necesario como campo de instancia si cada ronda crea uno nuevo
    // private Trainer oponenteActual; 
    private CombatManager gestorCombate; // Se asigna en initialize
    private EventDispatcher despachadorEventos;
    private BattlefieldState estadoCampoBatalla; // Se asigna en initialize
    private SwingGUI parentUI; 

    private int rondasCompletadas;
    private static final int EQUIPO_SUPERVIVENCIA_TAMANO = 6;
    private static final int NIVEL_POKEMON_SUPERVIVENCIA = 100;
    private static final int NUM_MOVIMIENTOS_POKEMON = 4;
    private static final int PORCENTAJE_CURACION_POR_RONDA = 25;
    private static final String ESCENARIO_SUPERVIVENCIA = "Arena Supervivencia"; // O el nombre que prefieras

    public SurvivalGameMode(SwingGUI parentUI) {
        if (parentUI == null) {
            throw new IllegalArgumentException("ParentUI no puede ser null para SurvivalGameMode.");
        }
        this.parentUI = parentUI;
        this.rondasCompletadas = 0;
    }

    @Override
    public void initialize(Trainer playerTrainer, CombatManager combatManager, EventDispatcher eventDispatcher, BattlefieldState battlefieldState) {
        this.entrenadorJugador = playerTrainer; 
        this.gestorCombate = combatManager; // El CombatManager de la ronda actual
        this.despachadorEventos = eventDispatcher;
        this.estadoCampoBatalla = battlefieldState; 
        
        if (this.despachadorEventos != null) {
             // El mensaje de inicio de modo se podría mover a SwingGUI antes de la primera llamada a proceedToCombat
        } else {
            System.err.println("SurvivalGameMode.initialize: despachadorEventos es null.");
        }
    }

    @Override
    public Trainer getInitialOpponent(Trainer playerTrainerShell) {
        this.rondasCompletadas = 0;
        this.entrenadorJugador = playerTrainerShell; 

        Map<String, Movements> todosMovimientosDisponibles;
        List<PokemonData> todosDatosPokemon;
        try {
            parentUI.loadBaseGameData();
            todosMovimientosDisponibles = parentUI.getAllAvailableMoves();
            todosDatosPokemon = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);

            if (todosDatosPokemon == null || todosDatosPokemon.isEmpty() || todosMovimientosDisponibles == null || todosMovimientosDisponibles.isEmpty()) {
                throw new IllegalStateException("Datos de Pokémon o movimientos no disponibles para iniciar Supervivencia.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (despachadorEventos != null) despachadorEventos.dispatchEvent(new MessageEvent("Error crítico al cargar datos para Supervivencia: " + e.getMessage()));
            return null; 
        }

        // El equipo del jugador ya se crea en SwingGUI.startGame para el modo Supervivencia.
        // Aquí solo nos aseguramos de que tenga el tamaño correcto y Pokémon válidos si es necesario.
        // Sin embargo, la lógica actual en SwingGUI ya crea un equipo de 6 para el jugador.

        if (this.entrenadorJugador.getteam() == null || this.entrenadorJugador.getteam().size() != EQUIPO_SUPERVIVENCIA_TAMANO) {
            System.err.println("SurvivalGameMode.getInitialOpponent: El equipo del jugador no tiene el tamaño correcto (" + 
                               (this.entrenadorJugador.getteam() != null ? this.entrenadorJugador.getteam().size() : "null") + 
                               "). Se esperaba " + EQUIPO_SUPERVIVENCIA_TAMANO);
            // Podríamos intentar recrearlo aquí, pero es mejor asegurar que SwingGUI lo haga bien.
            // List<Pokemon> nuevoEquipoJugador = crearEquipoAleatorioSupervivencia(EQUIPO_SUPERVIVENCIA_TAMANO, todosDatosPokemon, todosMovimientosDisponibles);
            // this.entrenadorJugador = new Trainer(this.entrenadorJugador.getName(), nuevoEquipoJugador, new ArrayList<>());
        }


        if (despachadorEventos != null && this.entrenadorJugador.getteam() != null) {
            despachadorEventos.dispatchEvent(new MessageEvent(this.entrenadorJugador.getName() + " comienza Supervivencia con: " + 
                this.entrenadorJugador.getteam().stream().map(Pokemon::getNombre).collect(Collectors.joining(", "))));
        }
        
        return configurarSiguienteOponenteIA(todosDatosPokemon, todosMovimientosDisponibles);
    }

    private List<Pokemon> crearEquipoAleatorioSupervivencia(int tamanoEquipo, List<PokemonData> todosDatosPokemon, Map<String, Movements> todosMovimientos) {
        List<Pokemon> equipo = new ArrayList<>();
        List<PokemonData> disponiblesParaElegir = new ArrayList<>(todosDatosPokemon);
        Random random = new Random();

        if (disponiblesParaElegir.size() < tamanoEquipo && tamanoEquipo > 0) {
            System.err.println("Advertencia: No hay suficientes Pokémon únicos (" + disponiblesParaElegir.size() + 
                               ") para crear un equipo de " + tamanoEquipo + ". Se usarán repetidos si es necesario o se creará un equipo más pequeño.");
            // Podrías permitir repeticiones o ajustar el tamaño del equipo si no hay suficientes únicos.
            // Por ahora, si no hay suficientes, el bucle se detendrá antes.
        }


        for (int i = 0; i < tamanoEquipo; i++) {
            if (disponiblesParaElegir.isEmpty()) {
                 // Si queremos permitir repeticiones para alcanzar el tamaño del equipo:
                 if (todosDatosPokemon.isEmpty()) break; // No hay nada que elegir
                 disponiblesParaElegir.addAll(todosDatosPokemon); // Rellenar de nuevo para permitir repetidos
            }
            PokemonData datosPokemonElegido = disponiblesParaElegir.remove(random.nextInt(disponiblesParaElegir.size()));
            
            Pokemon nuevoPokemon = new Pokemon(datosPokemonElegido, todosMovimientos, NIVEL_POKEMON_SUPERVIVENCIA);
            
            // Asignar 4 movimientos aleatorios/predefinidos
            // Los movimientos "predefinidos" se toman de la lista 'movimientoNombres' en pokemons.json
            List<String> nombresMovimientosBase = datosPokemonElegido.getMovimientoNombres();
            List<Movements> movimientosAprendidos = new ArrayList<>();

            if (nombresMovimientosBase != null && !nombresMovimientosBase.isEmpty()) {
                List<String> copiaNombres = new ArrayList<>(nombresMovimientosBase);
                Collections.shuffle(copiaNombres); // Aleatorizar el orden de los movimientos base
                for (String nombreMov : copiaNombres) {
                    if (movimientosAprendidos.size() >= NUM_MOVIMIENTOS_POKEMON) break;
                    Movements plantillaMov = todosMovimientos.get(nombreMov);
                    if (plantillaMov != null) {
                        movimientosAprendidos.add(new Movements(plantillaMov)); // Crear nueva instancia
                    }
                }
            }
            // Si después de los predefinidos aún faltan movimientos, completar aleatoriamente
            while (movimientosAprendidos.size() < NUM_MOVIMIENTOS_POKEMON && !todosMovimientos.isEmpty()) {
                List<Movements> todosLosMovimientosLista = new ArrayList<>(todosMovimientos.values());
                Movements movAleatorio = todosLosMovimientosLista.get(random.nextInt(todosLosMovimientosLista.size()));
                // Evitar duplicados simples
                if (movimientosAprendidos.stream().noneMatch(m -> m.getNombre().equals(movAleatorio.getNombre()))) {
                    movimientosAprendidos.add(new Movements(movAleatorio));
                }
            }
            nuevoPokemon.setMovimientos(movimientosAprendidos);
            nuevoPokemon.setHpActual(nuevoPokemon.getHpMax()); 

            equipo.add(nuevoPokemon);
        }
        return equipo;
    }

    private Trainer configurarSiguienteOponenteIA(List<PokemonData> todosDatosPokemon, Map<String, Movements> todosMovimientosDisponiblesMap) {
        // El oponente en supervivencia también tiene un equipo de 6 Pokémon
        List<Pokemon> equipoIA = crearEquipoAleatorioSupervivencia(EQUIPO_SUPERVIVENCIA_TAMANO, todosDatosPokemon, todosMovimientosDisponiblesMap);
        
        if (equipoIA.isEmpty()) {
            System.err.println("Error: No se pudo crear el equipo para el oponente IA.");
            return null; // No se puede crear un oponente sin equipo
        }
        
        return new Trainer("Oponente Supervivencia R" + (rondasCompletadas + 1), equipoIA, new ArrayList<>());
    }

    public void iniciarSiguienteBatalla() {
        if (this.despachadorEventos == null || this.estadoCampoBatalla == null || this.entrenadorJugador == null || this.parentUI == null) {
            System.err.println("SurvivalGameMode.iniciarSiguienteBatalla: Dependencias críticas son null.");
            // Si el gestorCombate es null aquí, es un problema mayor, pero la lógica principal
            // es que SwingGUI creará uno nuevo.
            if (this.gestorCombate != null) gestorCombate.signalCombatLoopToStop();
            return;
        }

        rondasCompletadas++;
        despachadorEventos.dispatchEvent(new MessageEvent("¡Ronda " + rondasCompletadas + " superada! Preparando siguiente desafío..."));

        for (Pokemon p : this.entrenadorJugador.getteam()) {
            if (!p.estaDebilitado()) {
                int cantidadCuracion = (int) (p.getHpMax() * (PORCENTAJE_CURACION_POR_RONDA / 100.0));
                p.curar(cantidadCuracion);
                despachadorEventos.dispatchEvent(new MessageEvent(p.getNombre() + " ha recuperado " + cantidadCuracion + " PS."));
            }
        }
        
        // Verificar si el jugador puede continuar (tiene Pokémon no debilitados)
        boolean jugadorPuedeContinuar = this.entrenadorJugador.getteam().stream().anyMatch(p -> p != null && !p.estaDebilitado());
        if (!jugadorPuedeContinuar) {
            despachadorEventos.dispatchEvent(new MessageEvent("¡No te quedan Pokémon! Fin del Modo Supervivencia."));
            // El CombatManager actual ya habrá terminado. SwingGUI manejará el fin del juego.
            parentUI.handleCombatEnd(null); // Indicar que el jugador no ganó
            return;
        }
        
        // Asegurar que el Pokémon activo del jugador sea uno no debilitado
        Pokemon pokemonActivoJugador = this.estadoCampoBatalla.getActivePokemonPlayer1();
        if (pokemonActivoJugador == null || pokemonActivoJugador.estaDebilitado()) {
            Pokemon siguientePokemon = this.entrenadorJugador.getteam().stream()
                                       .filter(p -> p != null && !p.estaDebilitado())
                                       .findFirst().orElse(null);
            if (siguientePokemon != null) {
                // SwingGUI necesitará saber esto para el próximo proceedToCombat
                this.entrenadorJugador.getteam().remove(siguientePokemon); // Simular que se mueve al frente
                this.entrenadorJugador.getteam().add(0, siguientePokemon);
                // No se actualiza directamente battlefieldState aquí, SwingGUI lo hará al crear el nuevo CombatManager
                despachadorEventos.dispatchEvent(new MessageEvent("Automáticamente envías a " + siguientePokemon.getNombre() + "."));
            } else { // Esto no debería pasar si jugadorPuedeContinuar es true
                despachadorEventos.dispatchEvent(new MessageEvent("¡Error interno! No se encontró Pokémon activo para el jugador."));
                parentUI.handleCombatEnd(null);
                return;
            }
        }


        Map<String, Movements> todosMovimientosDisponibles;
        List<PokemonData> todosDatosPokemon;
        try {
            parentUI.loadBaseGameData(); 
            todosMovimientosDisponibles = parentUI.getAllAvailableMoves();
            todosDatosPokemon = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);
             if (todosDatosPokemon == null || todosDatosPokemon.isEmpty() || todosMovimientosDisponibles == null || todosMovimientosDisponibles.isEmpty()) {
                throw new IllegalStateException("Datos de Pokémon o movimientos no disponibles para el siguiente oponente.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            despachadorEventos.dispatchEvent(new MessageEvent("Error crítico al cargar datos para el siguiente oponente: " + e.getMessage()));
            parentUI.handleCombatEnd(null); // Terminar el modo si no se pueden cargar datos
            return;
        }

        Trainer nuevoOponente = configurarSiguienteOponenteIA(todosDatosPokemon, todosMovimientosDisponibles);
        if (nuevoOponente == null || nuevoOponente.getteam().isEmpty()) {
            despachadorEventos.dispatchEvent(new MessageEvent("Error al configurar el siguiente oponente. Terminando Supervivencia."));
            parentUI.handleCombatEnd(this.entrenadorJugador); // El jugador gana si no hay más oponentes
            return;
        }
        
        // Solicitar a SwingGUI que inicie la siguiente ronda de combate
        parentUI.continueSurvivalModeCombat(this.entrenadorJugador, nuevoOponente, ESCENARIO_SUPERVIVENCIA);
    }
 
    @Override
    public void onPlayerWinBattle(Trainer player, Trainer defeatedOpponent) {
        if (this.despachadorEventos == null) return;
        // Si el jugador gana la ronda, se prepara la siguiente
        iniciarSiguienteBatalla();
    }

    @Override
    public void onPlayerLoseBattle(Trainer defeatedPlayer, Trainer winnerOpponent) {
        if (this.despachadorEventos == null || this.parentUI == null) return;
        despachadorEventos.dispatchEvent(new MessageEvent("Has sido derrotado en la ronda " + (rondasCompletadas + 1) + ". Fin del modo supervivencia. Rondas completadas: " + rondasCompletadas));
        // SwingGUI se encargará de finalizar el juego a través de handleCombatEnd
        parentUI.handleCombatEnd(winnerOpponent); // El oponente es el ganador del modo
    }

    @Override
    public void onPlayerDraw(Trainer player, Trainer opponent) {
        if (this.despachadorEventos == null || this.parentUI == null) return;
        despachadorEventos.dispatchEvent(new MessageEvent("La ronda " + (rondasCompletadas + 1) + " terminó en empate. Fin del modo supervivencia. Rondas completadas: " + rondasCompletadas));
        parentUI.handleCombatEnd(null); // Empate en el modo
    }

    @Override
    public String getModeName() {
        return "Supervivencia";
    }

    @Override
    public void onReturnToMenu() {
        this.rondasCompletadas = 0;
        // this.oponenteActual = null; // oponenteActual ya no es un campo de instancia persistente entre rondas
    }
}
