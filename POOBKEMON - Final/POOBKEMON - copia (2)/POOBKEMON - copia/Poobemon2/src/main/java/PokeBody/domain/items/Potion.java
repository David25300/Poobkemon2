// Archivo: PokeBody/domain/items/Potion.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer; // IMPORTANTE

/**
 * Poción: restaura hasta 20 PS del Pokémon objetivo.
 * ACTUALIZADO: El método apply ahora usa Consumer<String>.
 */
public class Potion implements Item {
    private static final int HEAL_AMOUNT = 20;

    @Override
    public String getName() {
        return "Potion";
    }

    @Override
    public String getNombre() {
        return "Poción";
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        if (target == null) {
            messageConsumer.accept("No hay un Pokémon objetivo para usar la Poción.");
            return;
        }
        if (target.estaDebilitado()) {
            messageConsumer.accept("No se puede usar Poción: " + target.getNombre() + " está debilitado.");
            return;
        }

        int heal = Math.min(HEAL_AMOUNT, target.getHpMax() - target.getHpActual());

        if (heal <= 0) {
            messageConsumer.accept(target.getNombre() + " ya tiene máxima salud.");
            return; 
        }

        target.curar(heal); 
        messageConsumer.accept(user.getName() + " usó Poción y restauró " + heal + " PS a " + target.getNombre() + "!");
        user.removeItem(this); 
    }
}
