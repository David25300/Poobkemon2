// Archivo: PokeBody/domain/items/VelocidadX.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.StatBoosts;
import PokeBody.domain.Trainer;

public class VelocidadX implements Item {

    @Override
    public String getName() {
        return "X Speed"; // Nombre interno/en inglés
    }

    @Override
    public String getNombre() {
        return "Velocidad X"; // Nombre en español para la UI
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

        int currentBoostLevel = target.getStatBoostLevel(StatBoosts.Stat.SPEED);
        if (currentBoostLevel >= 6) {
            messageConsumer.accept("¡La Velocidad de " + target.getNombre() + " no puede subir más!");
            return;
        }

        target.modificarStatBoost(StatBoosts.Stat.SPEED, 1);
        messageConsumer.accept(user.getName() + " usó " + getNombre() + " en " + target.getNombre() + ".");
        messageConsumer.accept("¡La Velocidad de " + target.getNombre() + " subió!");
        
        user.removeItem(this);
    }
}