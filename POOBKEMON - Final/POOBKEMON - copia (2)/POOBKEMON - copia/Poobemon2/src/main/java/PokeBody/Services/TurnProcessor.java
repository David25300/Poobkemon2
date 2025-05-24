package PokeBody.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.TurnOrderResolver.TurnAction; 
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class TurnProcessor {
    private final ActionExecutor actionExecutor;
    private final PokemonSwitchService pokemonSwitchService;
    private final ItemUsageService itemUsageService; 
    private final BattlefieldState battlefieldState;
    private final EventDispatcher eventDispatcher;
    private final FaintedPokemonHandler faintedPokemonHandler; 

    private static final int ACTION_PRIORITY_SWITCH = 7;
    private static final int ACTION_PRIORITY_QUICK_ITEM = 6; 
    private static final int ACTION_PRIORITY_STANDARD_ITEM = 5;
    private static final int ACTION_PRIORITY_RUN = 7; 

    public TurnProcessor(ActionExecutor actionExecutor, PokemonSwitchService pokemonSwitchService,
                         ItemUsageService itemUsageService, BattlefieldState battlefieldState,
                         EventDispatcher eventDispatcher, FaintedPokemonHandler faintedPokemonHandler) {
        this.actionExecutor = actionExecutor;
        this.pokemonSwitchService = pokemonSwitchService;
        this.itemUsageService = itemUsageService;
        this.battlefieldState = battlefieldState;
        this.eventDispatcher = eventDispatcher;
        this.faintedPokemonHandler = faintedPokemonHandler;
    }

    public boolean processTurnActions(PlayerActionChoice actionP1, Trainer trainerP1, PlayerActionChoice actionP2, Trainer trainerP2) {
        List<PrioritizedAction> allActions = new ArrayList<>();

        Pokemon p1Active = battlefieldState.getActivePokemonPlayer1();
        Pokemon p2Active = battlefieldState.getActivePokemonPlayer2();

        if (actionP1 != null && actionP1.type != null && p1Active != null && !p1Active.estaDebilitado()) {
            allActions.add(new PrioritizedAction(trainerP1, actionP1, p1Active));
        }
        if (actionP2 != null && actionP2.type != null && p2Active != null && !p2Active.estaDebilitado()) {
            allActions.add(new PrioritizedAction(trainerP2, actionP2, p2Active));
        }

        Collections.sort(allActions);
        System.out.println("[TurnProcessor] Acciones priorizadas: " + allActions);

        for (PrioritizedAction pAction : allActions) {
            if (battlefieldState.getOwnerOf(pAction.actorPokemonOriginal) == null || 
                (battlefieldState.getActivePokemonForTrainer(pAction.trainer) != pAction.actorPokemonOriginal && pAction.actorPokemonOriginal.estaDebilitado()) || 
                 battlefieldState.getActivePokemonForTrainer(pAction.trainer) == null || 
                 battlefieldState.getActivePokemonForTrainer(pAction.trainer).estaDebilitado() ) { 
                
                if (pAction.actorPokemonOriginal != null && pAction.actorPokemonOriginal.estaDebilitado()){
                     System.out.println("[TurnProcessor] " + pAction.actorPokemonOriginal.getNombre() + " de " + pAction.trainer.getName() +
                                   " está debilitado y no puede ejecutar su acción: " + pAction.choice.type);
                } else if (battlefieldState.getActivePokemonForTrainer(pAction.trainer) == null || battlefieldState.getActivePokemonForTrainer(pAction.trainer).estaDebilitado()){
                     System.out.println("[TurnProcessor] El Pokémon activo de " + pAction.trainer.getName() +
                                   " está debilitado o es nulo. " + (pAction.actorPokemonOriginal != null ? pAction.actorPokemonOriginal.getNombre() : "El Pokémon original") + " no puede actuar.");
                }
                continue; 
            }

            Pokemon currentActor = battlefieldState.getActivePokemonForTrainer(pAction.trainer);
            if (pAction.actorPokemonOriginal != currentActor || currentActor == null || currentActor.estaDebilitado()) {
                 if (pAction.actorPokemonOriginal != null) {
                    eventDispatcher.dispatchEvent(new MessageEvent(pAction.actorPokemonOriginal.getNombre() + " no puede realizar su acción (cambiado o debilitado)."));
                 }
                continue;
            }

            System.out.println("[TurnProcessor] Ejecutando acción para " + currentActor.getNombre() + ": " + pAction.choice.type);

            // boolean actionExecuted = false; // No se usa el valor de esta variable
            switch (pAction.choice.type) {
                case SWITCH_POKEMON:
                    pokemonSwitchService.executeSwitch(pAction.trainer, pAction.choice.switchToPokemonIndex, false); 
                    break;
                case USE_ITEM:
                    itemUsageService.processItemAction(pAction.trainer, pAction.choice);
                    break;
                case ATTACK:
                    Pokemon target = (pAction.trainer == trainerP1) ?
                                     battlefieldState.getActivePokemonPlayer2() :
                                     battlefieldState.getActivePokemonPlayer1();
                    if (target != null && !target.estaDebilitado()) {
                        // Crear una lista de un solo elemento para ejecutarAccionesDeAtaque
                        List<TurnAction> singleAttackActionList = Collections.singletonList(
                            new TurnAction(currentActor, target, pAction.choice.moveIndex, pAction.choice.targetIndices)
                        );
                        actionExecutor.ejecutarAccionesDeAtaque(singleAttackActionList, battlefieldState);
                        // actionExecuted = true; // No es necesario si no se usa
                    } else {
                         eventDispatcher.dispatchEvent(new MessageEvent(currentActor.getNombre() + " no tiene un objetivo válido para atacar."));
                    }
                    break;
                case RUN:
                    eventDispatcher.dispatchEvent(new MessageEvent(pAction.trainer.getName() + " intenta huir... ¡No se puede huir de un combate de entrenador!"));
                    // actionExecuted = true; // No es necesario si no se usa
                    break;
                default:
                     System.err.println("[TurnProcessor] Tipo de acción desconocida: " + pAction.choice.type);
                     break;
            }

            if (faintedPokemonHandler.handleFaintedPokemonAfterAction(trainerP1)) { 
                return false; 
            }
            if (faintedPokemonHandler.handleFaintedPokemonAfterAction(trainerP2)) { 
                return false; 
            }
        }
        return true; 
    }

    private static class PrioritizedAction implements Comparable<PrioritizedAction> {
        Trainer trainer;
        PlayerActionChoice choice;
        Pokemon actorPokemonOriginal; 
        int calculatedPriority;

        PrioritizedAction(Trainer trainer, PlayerActionChoice choice, Pokemon actorPokemonOriginal) {
            this.trainer = trainer;
            this.choice = choice;
            this.actorPokemonOriginal = actorPokemonOriginal;
            this.calculatedPriority = calculateActionEffectivePriority(choice, actorPokemonOriginal);
        }

        private static int calculateActionEffectivePriority(PlayerActionChoice choice, Pokemon actor) {
            if (choice == null || choice.type == null) return Integer.MIN_VALUE;
            switch (choice.type) {
                case SWITCH_POKEMON: return ACTION_PRIORITY_SWITCH;
                case USE_ITEM:
                    return ACTION_PRIORITY_STANDARD_ITEM;
                case ATTACK:
                    if (actor != null && choice.moveIndex >= 0 &&
                        actor.getMovimientos() != null && !actor.getMovimientos().isEmpty() && // Añadida verificación de lista no vacía
                        choice.moveIndex < actor.getMovimientos().size()) {
                        Movements move = actor.getMovimientos().get(choice.moveIndex);
                        if (move != null) return move.getPrioridad();
                    }
                    return 0; 
                case RUN: return ACTION_PRIORITY_RUN;
                default: return Integer.MIN_VALUE; 
            }
        }

        @Override
        public int compareTo(PrioritizedAction other) {
            int priorityCompare = Integer.compare(other.calculatedPriority, this.calculatedPriority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            if (this.actorPokemonOriginal != null && other.actorPokemonOriginal != null) {
                // CORRECCIÓN AQUÍ: Llamar a getVelocidadModificada()
                return Integer.compare(other.actorPokemonOriginal.getVelocidadModificada(), this.actorPokemonOriginal.getVelocidadModificada());
            }
            if (this.actorPokemonOriginal == null && other.actorPokemonOriginal != null) return 1; 
            if (this.actorPokemonOriginal != null && other.actorPokemonOriginal == null) return -1; 
            return 0; 
        }

        @Override
        public String toString() {
            return "PrioritizedAction{" +
                   "trainer=" + (trainer != null ? trainer.getName() : "null") +
                   ", choice=" + choice +
                   ", actorOriginal=" + (actorPokemonOriginal != null ? actorPokemonOriginal.getNombre() : "null") +
                   ", priority=" + calculatedPriority +
                   '}';
        }
    }
}