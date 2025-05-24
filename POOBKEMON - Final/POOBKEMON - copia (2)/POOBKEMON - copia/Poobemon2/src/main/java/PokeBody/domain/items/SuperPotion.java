// Archivo: PokeBody/domain/items/SuperPotion.java
package PokeBody.domain.items;

import java.util.function.Consumer;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer; // IMPORTANTE: Usar Consumer

/**
 * Superpoción: Recupera 60 PS (según el PDF, aunque antes teníamos 50) de un Pokémon no debilitado.
 * Ajustaremos a 60 PS según el ANEXO II del PDF que indica 50 para SuperPotion.
 * Si el PDF en otra sección dice 60, prevalece el Anexo de ítems.
 * El PDF ANEXO II dice: "Las superpociones recuperan 50 puntos de salud (PS) de un pokemon."
 * Por lo tanto, usaremos 50.
 */
public class SuperPotion implements Item {
    private static final int HEAL_AMOUNT = 50; // Cantidad de curación según ANEXO II

    @Override
    public String getName() {
        return "SuperPotion"; // Nombre interno/en inglés
    }

    @Override
    public String getNombre() {
        return "Superpoción"; // Nombre en español para la UI
    }

    @Override
    public void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer) {
        if (target == null) {
            messageConsumer.accept("No hay un Pokémon objetivo para usar la Superpoción.");
            return;
        }
        if (target.estaDebilitado()) {
            messageConsumer.accept("No se puede usar Superpoción: " + target.getNombre() + " está debilitado.");
            return;
        }

        // Calcula cuánto puede curar, limitado a la cantidad necesaria para alcanzar el HP máximo
        // y al valor máximo de la poción.
        int actualHeal = Math.min(HEAL_AMOUNT, target.getHpMax() - target.getHpActual());

        if (actualHeal <= 0) {
            messageConsumer.accept(target.getNombre() + " ya tiene la salud máxima.");
            return; // Salir si no hay nada que curar
        }

        target.curar(actualHeal); // Aplica la curación al Pokémon
        messageConsumer.accept(user.getName() + " usó Superpoción y restauró "
            + actualHeal + " PS a " + target.getNombre() + "!");

        // Eliminar el ítem del inventario del entrenador
        // Es importante que el objeto 'this' sea el que se elimine si las instancias son únicas por ítem.
        // Si los ítems son contables, la lógica de Trainer.removeItem debería manejar la cantidad.
        user.removeItem(this); 
    }
}