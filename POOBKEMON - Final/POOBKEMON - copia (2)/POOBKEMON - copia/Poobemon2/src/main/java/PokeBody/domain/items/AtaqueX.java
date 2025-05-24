// Archivo: PokeBody/domain/items/AtaqueX.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.StatBoosts;
import PokeBody.domain.Trainer;

public class AtaqueX implements Item {

    @Override
    public String getName() {
        return "X Attack"; // Nombre interno/en inglés
    }

    @Override
    public String getNombre() {
        return "Ataque X"; // Nombre en español para la UI
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

        // Los "X Items" suelen aumentar la estadística en 1 o 2 niveles.
        // Usaremos 1 nivel como estándar.
        int currentBoostLevel = target.getStatBoostLevel(StatBoosts.Stat.ATTACK);
        if (currentBoostLevel >= 6) {
            messageConsumer.accept("¡El Ataque de " + target.getNombre() + " no puede subir más!");
            return; // No consumir el ítem si no hay efecto
        }

        target.modificarStatBoost(StatBoosts.Stat.ATTACK, 1);
        messageConsumer.accept(user.getName() + " usó " + getNombre() + " en " + target.getNombre() + ".");
        messageConsumer.accept("¡El Ataque de " + target.getNombre() + " subió!");
        
        user.removeItem(this); // Eliminar el ítem del inventario del entrenador
    }
}