// Archivo: PokeBody/domain/items/BombaRival.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class BombaRival implements Item {
    private static final int DAMAGE_AMOUNT = 200;

    @Override
    public String getName() {
        return "Foe Bomb";
    }

    @Override
    public String getNombre() {
        return "Bomba Rival";
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        // 'target' para este ítem DEBE ser el Pokémon activo del OPONENTE.
        if (target == null) {
            messageConsumer.accept(getNombre() + " no tiene un objetivo oponente válido.");
            return;
        }
        if (target.estaDebilitado()) {
            messageConsumer.accept(getNombre() + " no puede usarse en " + target.getNombre() + " (oponente) porque ya está debilitado.");
            return;
        }

        messageConsumer.accept(user.getName() + " usó " + getNombre() + " en " + target.getNombre() + " (oponente)!");
        
        int actualDamage = Math.min(DAMAGE_AMOUNT, target.getHpActual());
        if (actualDamage <= 0 && target.getHpActual() > 0) {
            actualDamage = 1; 
        }

        target.recibirDanio(actualDamage);
        messageConsumer.accept("¡" + target.getNombre() + " (oponente) recibió " + actualDamage + " PS de daño!");

        if (target.estaDebilitado()) {
            messageConsumer.accept("¡" + target.getNombre() + " (oponente) se debilitó!");
        }
        
        user.removeItem(this);
    }
}
