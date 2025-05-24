package PokeBody.Services.GameModes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Face.SwingGUI; // Para el oponente de respaldo
import PokeBody.Data.DataLoader;
import PokeBody.Data.PokemonData;
import PokeBody.Services.BattlefieldState;
import PokeBody.Services.CombatManager;
import PokeBody.Services.GameSetupManager;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class NormalGameMode implements GameMode {

    private SwingGUI parentUI;
    private EventDispatcher eventDispatcher;
    // private CombatManager combatManager; // Se recibe en initialize
    // private BattlefieldState battlefieldState; // Se recibe en initialize

    private String normalGameSubMode; // "PVP", "PVE", "AVA"

    public NormalGameMode(SwingGUI parentUI) {
        if (parentUI == null) {
            throw new IllegalArgumentException("ParentUI no puede ser null para NormalGameMode.");
        }
        this.parentUI = parentUI;
    }

    /**
     * Método específico de NormalGameMode para iniciar el flujo de configuración
     * que lleva a la selección de equipos y escenario.
     */
    public void setupAndStart() {
        this.normalGameSubMode = null; // Se determinará más adelante
        try {
            parentUI.loadBaseGameData();
            parentUI.getTeamSelectionPanel().loadAvailablePokemonsAndItems(
                parentUI.getAllAvailablePokemons(),
                parentUI.getAllAvailableItems()
            );
            parentUI.getCardLayout().show(parentUI.getMainPanel(), "TEAM_SELECT");
        } catch (Exception e) {
            parentUI.showErrorAndReturnToMenu("Error al configurar el Modo Normal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(Trainer playerTrainer, CombatManager combatManager, EventDispatcher eventDispatcher, BattlefieldState battlefieldState) {
        // this.combatManager = combatManager; // CombatManager es manejado por SwingGUI
        this.eventDispatcher = eventDispatcher;
        // this.battlefieldState = battlefieldState; // Guardar si es necesario para lógica específica del modo
        if (this.eventDispatcher != null) {
            this.eventDispatcher.dispatchEvent(new MessageEvent("Modo Normal inicializado."));
        }
    }

    @Override
    public Trainer getInitialOpponent(Trainer playerTrainer) {
        // En Modo Normal, la selección del oponente es más compleja (puede ser otro jugador o una IA específica).
        // Este método es un requisito de la interfaz. Si se llama inesperadamente,
        // se podría devolver un oponente genérico o nulo.
        // El flujo principal de NormalGameMode usa setupAndStart() para la selección.
        System.out.println("NormalGameMode.getInitialOpponent() llamado. Esto es inusual para Modo Normal, que usa seleccion de equipo.");
        // Devolver un oponente IA genérico como fallback si es necesario.
        try {
            parentUI.loadBaseGameData();
            List<PokemonData> allPokemonData = DataLoader.loadPokemonsData(GameSetupManager.POKEMONS_RESOURCE_PATH);
            Map<String, Movements> allMovesMap = parentUI.getAllAvailableMoves();

            if (allPokemonData != null && !allPokemonData.isEmpty() && allMovesMap != null && !allMovesMap.isEmpty()) {
                Random random = new Random();
                PokemonData opponentData = allPokemonData.get(random.nextInt(allPokemonData.size()));
                List<Pokemon> opponentTeam = new ArrayList<>();
                opponentTeam.add(new Pokemon(opponentData, allMovesMap, 50)); // Nivel 50 por defecto
                return new Trainer("Oponente Aleatorio (Normal)", opponentTeam, new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error al crear oponente de respaldo en NormalGameMode: " + e.getMessage());
        }
        return null; // O un entrenador con un equipo vacío para indicar un problema.
    }

    @Override
    public void onPlayerWinBattle(Trainer player, Trainer defeatedOpponent) {
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent("¡" + player.getName() + " ha ganado la batalla en Modo Normal contra " + defeatedOpponent.getName() + "!"));
        }
        // La lógica de fin de combate (borrar guardado, volver a menú) la maneja SwingGUI.
    }

    @Override
    public void onPlayerLoseBattle(Trainer defeatedPlayer, Trainer winnerOpponent) {
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent("¡" + defeatedPlayer.getName() + " ha perdido la batalla en Modo Normal contra " + winnerOpponent.getName() + "!"));
        }
    }

    @Override
    public void onPlayerDraw(Trainer player, Trainer opponent) {
        if (eventDispatcher != null) {
            eventDispatcher.dispatchEvent(new MessageEvent("¡La batalla en Modo Normal entre " + player.getName() + " y " + opponent.getName() + " terminó en empate!"));
        }
    }

    @Override
    public String getModeName() {
        return "Normal";
    }

    @Override
    public void onReturnToMenu() {
        // Lógica de limpieza si es necesario al volver al menú
        this.normalGameSubMode = null;
    }

    // --- Métodos específicos de NormalGameMode ---
    public String getNormalGameSubMode() {
        return normalGameSubMode;
    }

    public void setNormalGameSubMode(SwingGUI.CombatantType p1Type, SwingGUI.CombatantType p2Type) {
        if (p1Type == SwingGUI.CombatantType.PLAYER && p2Type == SwingGUI.CombatantType.PLAYER) {
            this.normalGameSubMode = "PVP";
        } else if (p1Type == SwingGUI.CombatantType.PLAYER && p2Type == SwingGUI.CombatantType.AI) {
            this.normalGameSubMode = "PVE";
        } else if (p1Type == SwingGUI.CombatantType.AI && p2Type == SwingGUI.CombatantType.AI) {
            this.normalGameSubMode = "AVA";
        } else {
            this.normalGameSubMode = "Desconocido";
        }
    }
}
