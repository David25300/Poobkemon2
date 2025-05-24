// Archivo: PokeBody/domain/Item.java
package PokeBody.domain;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Interfaz para objetos que puede usar un entrenador en combate.
 * ACTUALIZADO: El método apply ahora usa Consumer<String> para los mensajes.
 */
public interface Item extends Serializable {
    /**
     * @return nombre del ítem (en inglés, para identificación interna)
     */
    String getName();

    /**
     * @return nombre del ítem (en español, para mostrar en la GUI)
     */
    String getNombre();

    /**
     * Aplica el efecto del ítem sobre un Pokémon o entrenador.
     * @param user entrenador que usa el ítem
     * @param target Pokémon objetivo (puede ser null si el efecto es global o no aplica a un Pokémon específico)
     * @param messageConsumer Un consumidor para enviar mensajes de log/feedback.
     */
    void apply(Trainer user, Pokemon target, Consumer<String> messageConsumer);
}
