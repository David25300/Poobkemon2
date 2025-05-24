package PokeBody.Services.GameModes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Face.SwingGUI;
import PokeBody.Data.DataLoader;
import PokeBody.Data.PokemonData;
import PokeBody.Services.BattlefieldState;
import PokeBody.Services.CombatManager;
import PokeBody.Services.GameSetupManager;
import PokeBody.Services.events.BattleEndEvent;
import PokeBody.Services.events.BattleStartEvent;
import PokeBody.Services.events.CombatEventListener;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.MoveUsedEvent;
import PokeBody.Services.events.PokemonChangeEvent;
import PokeBody.Services.events.PokemonFaintedEvent;
import PokeBody.Services.events.PokemonHpChangedEvent;
import PokeBody.Services.events.StatusAppliedEvent;
import PokeBody.Services.events.TurnStartEvent;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * Implementación del modo de juego "Test" o "Prueba".
 * La batalla termina después de un número fijo de turnos o si un entrenador es derrotado.
 */
public class TestGameMode implements GameMode, CombatEventListener {

    private SwingGUI parentUI;
    private CombatManager combatManager;
    private EventDispatcher eventDispatcher;
    // private BattlefieldState battlefieldState; // No parece usarse directamente después de initialize
    private Trainer playerTrainer; // El entrenador del jugador humano

    private int maxTurns;
    private int currentTurnNumberInTestMode = 0; // Contador de turnos específico para este modo
    private static final int DEFAULT_TEST_MAX_TURNS = 10;
    private static final int TEAM_SIZE_TEST = 3;
    
    // Tipos de combatiente (pueden ser configurables si se desea más flexibilidad)
    // private SwingGUI.CombatantType p1TypeCurrent = SwingGUI.CombatantType.PLAYER;
    // private SwingGUI.CombatantType p2TypeCurrent = SwingGUI.CombatantType.AI;


    public TestGameMode(SwingGUI parentUI) {
        if (parentUI == null) {
            throw new IllegalArgumentException("ParentUI no puede ser null para TestGameMode.");
        }
        this.parentUI = parentUI;
        this.maxTurns = DEFAULT_TEST_MAX_TURNS;
    }
    
    public void setMaxTurns(int turns) {
        this.maxTurns = Math.max(1, turns);
    }

    @Override
    public void initialize(Trainer playerTrainer, CombatManager combatManager, EventDispatcher eventDispatcher, BattlefieldState battlefieldState) {
        this.playerTrainer = playerTrainer; // Guardar el entrenador del jugador
        this.combatManager = combatManager;
        this.eventDispatcher = eventDispatcher;
        // this.battlefieldState = battlefieldState; // Guardar si se necesita para otras lógicas
        this.currentTurnNumberInTestMode = 0; // Resetear contador de turnos del modo

        if (this.eventDispatcher != null) {
            this.eventDispatcher.registerListener(this); // Registrarse para escuchar eventos de turno
            this.eventDispatcher.dispatchEvent(new MessageEvent("Modo Prueba iniciado. Máximo " + maxTurns + " turnos."));
        } else {
            System.err.println("TestGameMode.initialize: eventDispatcher es null.");
        }
    }

    @Override
    public Trainer getInitialOpponent(Trainer playerTrainer) {
        // playerTrainer es el del jugador humano, ya lo tenemos en this.playerTrainer si se pasó en initialize
        // o podemos usar el argumento directamente.
        System.out.println("TestGameMode: Configurando oponente inicial para el Modo Prueba...");
        try {
            parentUI.loadBaseGameData(); // Asegura que los datos base estén cargados
            List<Pokemon> allPokemons = parentUI.getAllAvailablePokemons();
            Map<String, Movements> allMovesMap = parentUI.getAllAvailableMoves();

            if (allPokemons == null || allPokemons.isEmpty()) {
                throw new IllegalStateException("No hay Pokémon disponibles para el modo prueba.");
            }
            if (allMovesMap == null || allMovesMap.isEmpty()) {
                throw new IllegalStateException("No hay movimientos disponibles para el modo prueba.");
            }

            List<Pokemon> opponentTeam = createRandomTeamForTest(allPokemons, allMovesMap, TEAM_SIZE_TEST);
            return new Trainer("IA de Pruebas", opponentTeam, new ArrayList<>()); // Sin ítems para la IA de prueba

        } catch (Exception e) {
            parentUI.showErrorAndReturnToMenu("Error al crear oponente para Modo Prueba: " + e.getMessage());
            e.printStackTrace();
            return null; // Indicar fallo
        }
    }
    
    private List<Pokemon> createRandomTeamForTest(List<Pokemon> sourcePool, Map<String, Movements> allMovesMap, int teamSize) {
        List<Pokemon> randomTeam = new ArrayList<>();
        List<PokemonData> allPokemonData; // Necesitamos PokemonData para construir nuevos Pokemon
        try {
            allPokemonData = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);
        } catch (Exception e) {
            System.err.println("TestGameMode: Error cargando PokemonData para crear equipo: " + e.getMessage());
            return randomTeam; // Devuelve equipo vacío si no se pueden cargar los datos base
        }

        if (allPokemonData.isEmpty()) {
            System.err.println("TestGameMode: No hay PokemonData disponibles para crear equipo.");
            return randomTeam;
        }

        List<PokemonData> availableToPick = new ArrayList<>(allPokemonData);
        Random random = new Random();

        for (int i = 0; i < teamSize; i++) {
            if (availableToPick.isEmpty()) break;
            PokemonData pickedData = availableToPick.remove(random.nextInt(availableToPick.size()));
            // Crear una nueva instancia de Pokemon con el nivel deseado y el mapa de movimientos
            Pokemon teamMember = new Pokemon(
                pickedData,
                allMovesMap, // Pasar el mapa de todos los movimientos
                50 // Nivel de prueba, por ejemplo
            );
            randomTeam.add(teamMember);
        }
        return randomTeam;
    }

    @Override
    public void onPlayerWinBattle(Trainer player, Trainer defeatedOpponent) {
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent(player.getName() + " ganó la batalla de prueba contra " + defeatedOpponent.getName() + "!"));
            // En modo prueba, ganar una batalla podría simplemente terminar la prueba.
            if (this.combatManager != null) {
                this.combatManager.signalCombatLoopToStop();
            }
        }
    }

    @Override
    public void onPlayerLoseBattle(Trainer defeatedPlayer, Trainer winnerOpponent) {
         if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent(defeatedPlayer.getName() + " perdió la batalla de prueba contra " + winnerOpponent.getName() + "!"));
            if (this.combatManager != null) {
                this.combatManager.signalCombatLoopToStop();
            }
        }
    }

    @Override
    public void onPlayerDraw(Trainer player, Trainer opponent) {
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent("La batalla de prueba entre " + player.getName() + " y " + opponent.getName() + " terminó en empate."));
            if (this.combatManager != null) {
                this.combatManager.signalCombatLoopToStop();
            }
        }
    }

    @Override
    public String getModeName() {
        return "Prueba";
    }

    @Override
    public void onReturnToMenu() {
        // Limpiar estado si es necesario
        this.currentTurnNumberInTestMode = 0;
        if (this.eventDispatcher != null) {
            this.eventDispatcher.unregisterListener(this); // Dejar de escuchar eventos
        }
    }

    // Implementación de CombatEventListener
    @Override
    public void onBattleStart(BattleStartEvent event) {
        // El modo de prueba podría querer hacer algo específico al inicio de la batalla
        this.currentTurnNumberInTestMode = 0; // Asegurarse de resetear al inicio de una nueva batalla
    }

    @Override
    public void onBattleEnd(BattleEndEvent event) {
        // El modo de prueba podría querer hacer algo específico al final de la batalla
        if (this.eventDispatcher != null) {
            this.eventDispatcher.unregisterListener(this); // Dejar de escuchar eventos
        }
    }

    @Override
    public void onTurnStart(TurnStartEvent event) {
        this.currentTurnNumberInTestMode = event.getTurnNumber();
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent("Modo Prueba - Turno: " + this.currentTurnNumberInTestMode + "/" + maxTurns));
        }
        if (this.currentTurnNumberInTestMode >= maxTurns) {
            if (eventDispatcher != null) {
                eventDispatcher.dispatchEvent(new MessageEvent("Límite de " + maxTurns + " turnos alcanzado en Modo Prueba. Finalizando combate."));
            }
            if (this.combatManager != null) {
                this.combatManager.signalCombatLoopToStop();
            }
        }
    }

    @Override
    public void onMoveUsed(MoveUsedEvent event) { /* No necesita acción específica aquí */ }

    @Override
    public void onPokemonFainted(PokemonFaintedEvent event) { /* No necesita acción específica aquí */ }

    @Override
    public void onPokemonHpChanged(PokemonHpChangedEvent event) { /* No necesita acción específica aquí */ }

    @Override
    public void onStatusApplied(StatusAppliedEvent event) { /* No necesita acción específica aquí */ }

    @Override
    public void onMessage(MessageEvent event) { /* No necesita acción específica aquí */ }

    @Override
    public void onPokemonChange(PokemonChangeEvent event) { /* No necesita acción específica aquí */ }

}
