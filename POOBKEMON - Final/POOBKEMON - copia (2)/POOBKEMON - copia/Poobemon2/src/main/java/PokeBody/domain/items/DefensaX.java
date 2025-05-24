// Archivo: PokeBody/domain/items/DefensaX.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.StatBoosts;
import PokeBody.domain.Trainer;

public class DefensaX implements Item {

    @Override
    public String getName() {
        return "X Defense"; // Nombre interno/en inglés
    }

    @Override
    public String getNombre() {
        return "Defensa X"; // Nombre en español para la UI
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        if (target == null) {
            messageConsumer.accept("No hay un Pokémon objetivo para usar " + getNombre() + ".");
            return;
        }
        if (target.estaDebilitado()) {
            messageConsumer.accept(getNombre() + " no puede usarse en " + target.getNombre() + " porque está debilitado.");
            return;
        }

        int currentBoostLevel = target.getStatBoostLevel(StatBoosts.Stat.DEFENSE);
        if (currentBoostLevel >= 6) {
            messageConsumer.accept("¡La Defensa de " + target.getNombre() + " no puede subir más!");
            return; 
        }

        target.modificarStatBoost(StatBoosts.Stat.DEFENSE, 1);
        messageConsumer.accept(user.getName() + " usó " + getNombre() + " en " + target.getNombre() + ".");
        messageConsumer.accept("¡La Defensa de " + target.getNombre() + " subió!");
        
        user.removeItem(this);
    }
}
