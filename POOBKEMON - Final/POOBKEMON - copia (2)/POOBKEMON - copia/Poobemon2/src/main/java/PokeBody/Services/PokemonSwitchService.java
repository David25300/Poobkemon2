package PokeBody.Services;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.PokemonChangeEvent;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

/**
 * Handles the logic for switching Pokémon during a battle.
 * It interacts with BattlefieldState, TeamManager, and EventDispatcher
 * to ensure all necessary updates and notifications occur.
 */
public class PokemonSwitchService {
    private final BattlefieldState battlefieldState;
    private final TeamManager teamManager;
    private final EventDispatcher eventDispatcher;

    /**
     * Constructor for PokemonSwitchService.
     * @param battlefieldState The current state of the battlefield.
     * @param teamManager The manager for player and opponent teams.
     * @param eventDispatcher The dispatcher for combat events.
     */
    public PokemonSwitchService(BattlefieldState battlefieldState, TeamManager teamManager, EventDispatcher eventDispatcher) {
        if (battlefieldState == null) {
            throw new IllegalArgumentException("BattlefieldState cannot be null for PokemonSwitchService.");
        }
        if (teamManager == null) {
            throw new IllegalArgumentException("TeamManager cannot be null for PokemonSwitchService.");
        }
        if (eventDispatcher == null) {
            throw new IllegalArgumentException("EventDispatcher cannot be null for PokemonSwitchService.");
        }
        this.battlefieldState = battlefieldState;
        this.teamManager = teamManager;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Executes the action of changing the active Pokémon for a trainer.
     *
     * @param trainer The trainer making the switch.
     * @param switchToPokemonIndex The index of the Pokémon in the trainer's team to switch to.
     * @param isMandatorySwitch Indicates if the switch is forced (e.g., due to a fainted Pokémon).
     * This can influence messages or specific rules, though core validation remains.
     * @return true if the switch was successful, false otherwise.
     */
    public boolean executeSwitch(Trainer trainer, int switchToPokemonIndex, boolean isMandatorySwitch) {
        if (trainer == null) {
            eventDispatcher.dispatchEvent(new MessageEvent("Error: Entrenador no válido para el cambio."));
            System.err.println("[PokemonSwitchService] executeSwitch: Trainer es null.");
            return false;
        }

        TeamManager.Equipo equipoEnum = (trainer == battlefieldState.getTrainerPlayer1()) ? TeamManager.Equipo.JUGADOR : TeamManager.Equipo.RIVAL;
        Pokemon oldPokemon = battlefieldState.getActivePokemonForTrainer(trainer);

        // Validar el índice del Pokémon a cambiar
        if (switchToPokemonIndex < 0 || switchToPokemonIndex >= trainer.getteam().size()) {
            eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó un cambio inválido (índice fuera de rango)."));
            System.err.println("[PokemonSwitchService] executeSwitch: Índice de cambio inválido: " + switchToPokemonIndex + " para " + trainer.getName());
            return false;
        }

        Pokemon newPokemon = trainer.getteam().get(switchToPokemonIndex);

        if (newPokemon == null) {
            eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó cambiar a una ranura vacía."));
            System.err.println("[PokemonSwitchService] executeSwitch: Intento de cambio a slot null por " + trainer.getName());
            return false;
        }

        if (newPokemon.estaDebilitado()) {
            eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " no puede cambiar a " + newPokemon.getNombre() + ", ¡está debilitado!"));
            System.err.println("[PokemonSwitchService] executeSwitch: Intento de cambio a Pokémon KO: " + newPokemon.getNombre() + " por " + trainer.getName());
            return false;
        }

        // Si no es un cambio obligatorio y el Pokémon seleccionado ya está en batalla
        if (!isMandatorySwitch && oldPokemon == newPokemon) {
            eventDispatcher.dispatchEvent(new MessageEvent(newPokemon.getNombre() + " ya está en combate."));
            System.out.println("[PokemonSwitchService] executeSwitch: " + trainer.getName() + " intentó cambiar a " + newPokemon.getNombre() + " que ya está activo (no obligatorio).");
            return false;
        }
        
        // Lógica para retirar al Pokémon anterior
        if (oldPokemon != null && !oldPokemon.estaDebilitado()) {
            eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " retira a " + oldPokemon.getNombre() + "."));
            oldPokemon.resetearBoosts(); // Resetea boosts y otros estados volátiles
            // Aquí se podrían añadir más lógicas como desactivar efectos específicos al salir.
        } else if (oldPokemon != null && oldPokemon.estaDebilitado() && !isMandatorySwitch) {
            // Esto podría ocurrir si un Pokémon se debilita por un efecto de fin de turno
            // y el jugador elige cambiarlo ANTES de que el juego lo fuerce.
            // O si el cambio es voluntario pero el actual se debilitó justo antes.
            System.out.println("[PokemonSwitchService] " + oldPokemon.getNombre() + " de " + trainer.getName() + " ya estaba debilitado al intentar un cambio no mandatorio.");
        }


        // Usar TeamManager para actualizar el Pokémon activo en la lista del equipo
        boolean switchedInTeamManager = teamManager.cambiarPokemonAIndice(equipoEnum, switchToPokemonIndex);

        if (switchedInTeamManager) {
            // Actualizar BattlefieldState con el nuevo Pokémon activo
            // TeamManager ya actualizó su índice interno, obtenemos el nuevo activo desde ahí.
            Pokemon newActiveFromTeamManager = teamManager.getPokemonActivo(equipoEnum);
            battlefieldState.setActivePokemonForTrainer(trainer, newActiveFromTeamManager);

            eventDispatcher.dispatchEvent(new PokemonChangeEvent(trainer, oldPokemon, newActiveFromTeamManager));
            eventDispatcher.dispatchEvent(new MessageEvent("¡Adelante, " + newActiveFromTeamManager.getNombre() + "!"));
            System.out.println("[PokemonSwitchService] executeSwitch: " + trainer.getName() + " cambió exitosamente a " + newActiveFromTeamManager.getNombre());
            return true;
        } else {
            // Esto podría pasar si teamManager.cambiarPokemonAIndice tiene validaciones adicionales que fallan
            // o si el índice, a pesar de las validaciones iniciales, resulta problemático para TeamManager.
            eventDispatcher.dispatchEvent(new MessageEvent("¡El cambio de " + trainer.getName() + " a " + newPokemon.getNombre() + " falló inesperadamente!"));
            System.err.println("[PokemonSwitchService] executeSwitch: teamManager.cambiarPokemonAIndice devolvió false para " + trainer.getName() + " al cambiar a " + newPokemon.getNombre());
            return false;
        }
    }
}