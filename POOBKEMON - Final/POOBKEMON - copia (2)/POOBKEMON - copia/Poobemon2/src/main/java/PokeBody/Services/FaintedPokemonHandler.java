package PokeBody.Services;

import java.util.List;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.CombatManager.PlayerActionType;
import PokeBody.Services.GameModes.GameMode;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent; // Importado para mensajes
import PokeBody.Services.events.PokemonFaintedEvent;
import PokeBody.domain.MovementSelector;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;


public class FaintedPokemonHandler {
    private final BattlefieldState battlefieldState;
    private final TeamManager teamManager;
    private final PlayerInputHandler playerInputHandler;
    private final PokemonSwitchService pokemonSwitchService;
    private final EventDispatcher eventDispatcher;
    // private final GameMode activeGameMode; // No se usa directamente para checkEndCondition aquí
    private final CombatManager combatManager;

    public FaintedPokemonHandler(BattlefieldState battlefieldState, TeamManager teamManager,
                                 PlayerInputHandler playerInputHandler, PokemonSwitchService pokemonSwitchService,
                                 EventDispatcher eventDispatcher, GameMode activeGameMode, // Se mantiene por si otros métodos lo necesitan
                                 CombatManager combatManager) {
        this.battlefieldState = battlefieldState;
        this.teamManager = teamManager;
        this.playerInputHandler = playerInputHandler;
        this.pokemonSwitchService = pokemonSwitchService;
        this.eventDispatcher = eventDispatcher;
        // this.activeGameMode = activeGameMode;
        this.combatManager = combatManager;
    }

    /**
     * Maneja la situación si el Pokémon activo del entrenador especificado está debilitado.
     * Se llama después de que una acción (ataque, efecto de estado) podría haber causado un KO.
     * Fuerza un cambio si es posible.
     * @param trainer El entrenador cuyo Pokémon activo se verificará.
     * @return true si el entrenador se ha quedado sin Pokémon válidos para continuar (ha perdido), false en caso contrario.
     */
    public boolean handleFaintedPokemonAfterAction(Trainer trainer) {
        if (trainer == null) {
            System.err.println("[FaintedPokemonHandler] El entrenador proporcionado es null.");
            return false; // No se puede procesar sin un entrenador
        }

        Pokemon activePokemon = battlefieldState.getActivePokemonForTrainer(trainer);
        
        // Si el Pokémon activo del entrenador está realmente debilitado
        if (activePokemon != null && activePokemon.estaDebilitado()) {
            eventDispatcher.dispatchEvent(new PokemonFaintedEvent(activePokemon, trainer));
            System.out.println("[FaintedPokemonHandler] " + activePokemon.getNombre() + " de " + trainer.getName() + " se debilitó.");

            // Verificar si el entrenador tiene más Pokémon disponibles
            TeamManager.Equipo equipoDelEntrenador = (trainer == battlefieldState.getTrainerPlayer1()) ? TeamManager.Equipo.JUGADOR : TeamManager.Equipo.RIVAL;
            if (teamManager.hayPokemonDisponibles(equipoDelEntrenador)) {
                System.out.println("[FaintedPokemonHandler] " + trainer.getName() + " tiene Pokémon disponibles. Forzando cambio.");
                
                MovementSelector selectorDelEntrenador = (trainer == battlefieldState.getTrainerPlayer1()) ?
                                                       combatManager.getPlayer1Selector() :
                                                       combatManager.getPlayer2Selector();
                
                Pokemon opponentPokemon = (trainer == battlefieldState.getTrainerPlayer1()) ?
                                          battlefieldState.getActivePokemonPlayer2() :
                                          battlefieldState.getActivePokemonPlayer1();

                PlayerActionChoice switchChoice = playerInputHandler.getPlayerAction(
                    trainer,
                    selectorDelEntrenador,
                    activePokemon, // El Pokémon KO
                    opponentPokemon, 
                    true // isMandatorySwitch = true
                );

                boolean switchSuccess = false;
                if (switchChoice != null && switchChoice.type == PlayerActionType.SWITCH_POKEMON && switchChoice.switchToPokemonIndex != -1) {
                    if (switchChoice.switchToPokemonIndex >= 0 && switchChoice.switchToPokemonIndex < trainer.getteam().size()) {
                        Pokemon targetSwitchPokemon = trainer.getteam().get(switchChoice.switchToPokemonIndex);
                        if (targetSwitchPokemon != null && !targetSwitchPokemon.estaDebilitado()) {
                           switchSuccess = pokemonSwitchService.executeSwitch(trainer, switchChoice.switchToPokemonIndex, true);
                        } else {
                            System.err.println("[FaintedPokemonHandler] Intento de cambio a Pokémon KO o slot inválido por elección: " + 
                                               (targetSwitchPokemon != null ? targetSwitchPokemon.getNombre() : "SLOT VACIO/NULL"));
                            eventDispatcher.dispatchEvent(new MessageEvent("Error: " + trainer.getName() + " intentó cambiar a un Pokémon no válido."));
                        }
                    } else {
                        System.err.println("[FaintedPokemonHandler] Índice de cambio (" + switchChoice.switchToPokemonIndex + ") fuera de rango para " + trainer.getName());
                        eventDispatcher.dispatchEvent(new MessageEvent("Error: " + trainer.getName() + " intentó un cambio con índice inválido."));
                    }
                }

                if (!switchSuccess) {
                    System.err.println("[FaintedPokemonHandler] El cambio elegido/automático por " + trainer.getName() + " falló o no fue un cambio válido. Intentando primer disponible.");
                    eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " debe seleccionar un Pokémon válido."));
                    
                    int firstAvailableIdx = findFirstAvailableSwitchIndex(trainer);
                    if (firstAvailableIdx != -1) {
                        switchSuccess = pokemonSwitchService.executeSwitch(trainer, firstAvailableIdx, true);
                    }
                    
                    if (!switchSuccess) {
                        System.out.println("[FaintedPokemonHandler] " + trainer.getName() + " no pudo realizar un cambio válido (después de fallo de elección/forzado). Equipo derrotado.");
                        eventDispatcher.dispatchEvent(new MessageEvent("¡" + trainer.getName() + " no tiene más Pokémon para luchar!"));
                        return true; // Indica que el entrenador ha perdido
                    }
                }
                // Si el cambio fue exitoso, el entrenador no ha perdido todavía.
                return false; 
            } else {
                // No hay Pokémon disponibles, el entrenador ha perdido.
                System.out.println("[FaintedPokemonHandler] " + trainer.getName() + " no tiene más Pokémon disponibles. Equipo derrotado.");
                eventDispatcher.dispatchEvent(new MessageEvent("¡" + trainer.getName() + " no tiene más Pokémon para luchar!"));
                return true; // Indica que el entrenador ha perdido
            }
        }
        // El Pokémon activo del entrenador no está (o ya no está) debilitado, o no había Pokémon activo.
        return false; 
    }

    private int findFirstAvailableSwitchIndex(Trainer trainer) {
        if (trainer == null || trainer.getteam() == null) return -1;
        List<Pokemon> team = trainer.getteam();
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i) != null && !team.get(i).estaDebilitado()) {
                return i;
            }
        }
        return -1; // No se encontró ningún Pokémon disponible
    }
}