// Archivo: PokeBody/domain/items/Revive.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer; // IMPORTANTE: Usar Consumer

/**
 * Revivir: revive a un Pokémon debilitado con la mitad de sus PS máximos.
 * ACTUALIZADO: El método apply ahora usa Consumer<String>.
 */
public class Revive implements Item {
    @Override
    public String getName() {
        return "Revive"; // Nombre interno/en inglés
    }

    @Override
    public String getNombre() {
        return "Revivir"; // Nombre en español para la UI
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        if (target == null) {
            messageConsumer.accept("No hay un Pokémon objetivo para usar Revivir.");
            return;
        }
        if (!target.estaDebilitado()) {
            messageConsumer.accept("No se puede usar Revivir: " + target.getNombre() + " no está debilitado.");
            return;
        }

        // Calcula la mitad de los PS máximos, redondeando hacia arriba si es impar.
        int reviveHp = (target.getHpMax() + 1) / 2; 
        
        // Llama al método revivir del Pokémon.
        // Este método debería encargarse de cambiar el estado del Pokémon y ajustar sus PS.
        target.revivir(reviveHp); 
        
        messageConsumer.accept(user.getName() + " usó Revivir y revivió a " + target.getNombre() + " con " + reviveHp + " PS!");
        
        // Eliminar el ítem del inventario del entrenador
        user.removeItem(this); 
    }
}
