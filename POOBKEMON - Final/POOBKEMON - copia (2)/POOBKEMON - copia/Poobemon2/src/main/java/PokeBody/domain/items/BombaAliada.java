// Archivo: PokeBody/domain/items/BombaAliada.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;

public class BombaAliada implements Item {
    private static final int DAMAGE_AMOUNT = 200;

    @Override
    public String getName() {
        return "Ally Bomb";
    }

    @Override
    public String getNombre() {
        return "Bomba Aliada";
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        if (target == null) {
            messageConsumer.accept(getNombre() + " no tiene un objetivo válido (el propio Pokémon activo).");
            return;
        }
        if (target.estaDebilitado()) {
            messageConsumer.accept(getNombre() + " no puede usarse en " + target.getNombre() + " porque ya está debilitado.");
            return;
        }

        messageConsumer.accept(user.getName() + " usó " + getNombre() + " en su propio " + target.getNombre() + "!");
        
        int actualDamage = Math.min(DAMAGE_AMOUNT, target.getHpActual());
        if (actualDamage <= 0 && target.getHpActual() > 0) {
            actualDamage = 1;
        }
        
        target.recibirDanio(actualDamage);
        messageConsumer.accept("¡" + target.getNombre() + " recibió " + actualDamage + " PS de daño!");

        if (target.estaDebilitado()) {
            messageConsumer.accept("¡" + target.getNombre() + " se debilitó!");
        }
        
        user.removeItem(this);
    }
}