// Archivo: PokeBody/Services/ItemUsageService.java
package PokeBody.Services;

import java.util.function.Consumer;

import PokeBody.Services.CombatManager.PlayerActionChoice;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.Services.events.PokemonHpChangedEvent; // Para ítems que curan
import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.items.AtaqueEspX;
import PokeBody.domain.items.AtaqueX;
import PokeBody.domain.items.DefensaEspX;
import PokeBody.domain.items.DefensaX;
import PokeBody.domain.items.Revive; // Para la lógica específica de Revive
import PokeBody.domain.items.VelocidadX;


public class ItemUsageService {
    private final BattlefieldState battlefieldState;
    private final EventDispatcher eventDispatcher;

    public ItemUsageService(BattlefieldState battlefieldState, EventDispatcher eventDispatcher) {
        this.battlefieldState = battlefieldState;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Procesa la acción de usar un ítem.
     * @param trainer El entrenador que usa el ítem.
     * @param choice La elección de acción que contiene el ítem y el objetivo.
     * @return true si el ítem se usó (o se intentó usar), false si hubo un error previo.
     */
    public boolean processItemAction(Trainer trainer, PlayerActionChoice choice) {
        if (trainer == null || choice == null || choice.itemToUse == null) {
            eventDispatcher.dispatchEvent(new MessageEvent("Error: Intento de usar ítem con datos inválidos."));
            System.err.println("[ItemUsageService] Intento de usar ítem con trainer, choice o itemToUse nulos.");
            return false;
        }

        Item item = choice.itemToUse;
        Pokemon initialTargetPokemon = choice.itemTargetPokemon; 
        Pokemon finalTargetForApply;

        // Determinar el objetivo final y aplicar validaciones específicas
        if (item instanceof AtaqueX || item instanceof DefensaX || item instanceof VelocidadX ||
            item instanceof AtaqueEspX || item instanceof DefensaEspX) {
            
            finalTargetForApply = battlefieldState.getActivePokemonForTrainer(trainer); // Los X-Items siempre al activo

            if (finalTargetForApply == null) {
                 eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó usar " + item.getNombre() + " sin un Pokémon activo."));
                 return false; 
            }
            if (finalTargetForApply.estaDebilitado()) {
                 eventDispatcher.dispatchEvent(new MessageEvent("No se puede usar " + item.getNombre() + " en " + finalTargetForApply.getNombre() + " porque está debilitado."));
                 return false;
            }
            // La lógica de si la stat ya está al máximo se maneja en el método apply() del ítem X.
        } else if (item instanceof Revive) {
            finalTargetForApply = initialTargetPokemon; // Revive necesita un objetivo específico de la elección
            if (finalTargetForApply == null) {
                eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " debe seleccionar un Pokémon debilitado para usar Revivir."));
                return false;
            }
            if (!finalTargetForApply.estaDebilitado()) {
                eventDispatcher.dispatchEvent(new MessageEvent(item.getNombre() + " solo se puede usar en Pokémon debilitados. " + finalTargetForApply.getNombre() + " no está debilitado."));
                return false;
            }
        } else { // Para otros ítems (Pociones)
            finalTargetForApply = (initialTargetPokemon != null) ? initialTargetPokemon : battlefieldState.getActivePokemonForTrainer(trainer);
            if (finalTargetForApply == null) {
                eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó usar " + item.getNombre() + " sin un objetivo válido."));
                return false;
            }
            if (finalTargetForApply.estaDebilitado()) { // No se puede usar poción en Pokémon KO
                eventDispatcher.dispatchEvent(new MessageEvent("No se puede usar " + item.getNombre() + " en " + finalTargetForApply.getNombre() + " porque está debilitado."));
                return false;
            }
        }

        // Verificar si el entrenador tiene el ítem
        if (!trainer.getItems().contains(item)) {
             eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó usar " + item.getNombre() + " pero no lo tiene en su mochila."));
             System.err.println("[ItemUsageService] " + trainer.getName() + " no tiene el ítem " + item.getNombre());
             return false; 
        }

        // Mensaje de uso del ítem
        eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " usa " + item.getNombre() +
                                          (finalTargetForApply != null ? " en " + finalTargetForApply.getNombre() : "") + "..."));

        int hpAntesDelItem = (finalTargetForApply != null) ? finalTargetForApply.getHpActual() : 0;

        // Callback para los mensajes del método apply del ítem
        Consumer<String> messageCallback = msg -> {
            if (this.eventDispatcher != null) {
                this.eventDispatcher.dispatchEvent(new MessageEvent(msg));
            }
        };
        
        // Aplicar el ítem
        item.apply(trainer, finalTargetForApply, messageCallback); 

        // Si el ítem afectó el HP (Pociones, Revivir), despachar el evento correspondiente.
        // No despachar para ítems X ya que no modifican HP.
        if (finalTargetForApply != null && finalTargetForApply.getHpActual() != hpAntesDelItem &&
            !(item instanceof AtaqueX || item instanceof DefensaX || item instanceof VelocidadX ||
              item instanceof AtaqueEspX || item instanceof DefensaEspX) ) {
            eventDispatcher.dispatchEvent(new PokemonHpChangedEvent(finalTargetForApply, finalTargetForApply.getHpActual(), finalTargetForApply.getHpMax(), hpAntesDelItem));
        }
        
        return true;
    }
}