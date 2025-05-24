// Archivo: PokeBody/Services/TurnOrderResolver.java
package PokeBody.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;

/**
 * Resuelve el orden de las acciones de ataque en cada turno de combate.
 */
public class TurnOrderResolver {
    private final Random random;

    public TurnOrderResolver() {
        this.random = new Random();
    }

    public TurnOrderResolver(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Ordena una lista de acciones de ataque (`TurnAction`).
     * @param accionesSinOrdenar La lista de TurnAction.
     * @return Una nueva lista de TurnAction ordenada.
     */
    public List<TurnAction> ordenarAccionesDeAtaque(List<TurnAction> accionesSinOrdenar) {
        List<TurnAction> accionesOrdenadas = new ArrayList<>(accionesSinOrdenar);
        // Ordenar la lista
        Collections.sort(accionesOrdenadas, (a1, a2) -> {
            Movements mov1 = a1.getMovimiento();
            Movements mov2 = a2.getMovimiento();

            // Manejar casos donde el movimiento podría ser nulo (ej. si Lucha se representa así)
            if (mov1 == null && mov2 == null) return 0;
            if (mov1 == null) return 1; // Acciones sin movimiento (o Lucha) al final o según reglas
            if (mov2 == null) return -1;

            // 1. Comparar por prioridad del movimiento
            int prioridad1 = mov1.getPrioridad();
            int prioridad2 = mov2.getPrioridad();
            if (prioridad1 != prioridad2) {
                return Integer.compare(prioridad2, prioridad1); // Mayor prioridad primero
            }

            // 2. Si la prioridad es la misma, comparar por velocidad del Pokémon
            Pokemon atacante1 = a1.getAtacante();
            Pokemon atacante2 = a2.getAtacante();

            // Es poco probable que los atacantes sean nulos aquí si llegaron a esta etapa,
            // pero una verificación no hace daño.
            if (atacante1 == null && atacante2 == null) return 0;
            if (atacante1 == null) return 1;
            if (atacante2 == null) return -1;
            
            int velocidad1 = atacante1.getVelocidadModificada();
            int velocidad2 = atacante2.getVelocidadModificada();
            if (velocidad1 != velocidad2) {
                return Integer.compare(velocidad2, velocidad1); // Mayor velocidad primero
            }

            // 3. Si la velocidad también es la misma, desempate aleatorio
            return random.nextBoolean() ? -1 : 1;
        });
        return accionesOrdenadas;
    }

    /**
     * Representa una acción de ataque en un turno.
     * Constructor actualizado para incluir targetIndices.
     */
    public static class TurnAction {
        private final Pokemon atacante;
        private final Pokemon objetivoPrincipal; // El objetivo primario (para simplificar en 1v1)
        private final int movimientoIdx;
        private final Movements movimientoCache;
        private final List<Integer> targetIndices; // Índices de los objetivos

        /**
         * Constructor de TurnAction.
         * @param atacante El Pokémon que ataca.
         * @param objetivoPrincipal El objetivo principal del ataque (puede ser null para algunos movimientos).
         * @param movimientoIdx El índice del movimiento en la lista del atacante.
         * @param targetIndices Lista de índices de los objetivos (puede ser null o vacía, se creará una lista vacía si es null).
         */
        public TurnAction(Pokemon atacante, Pokemon objetivoPrincipal, int movimientoIdx, List<Integer> targetIndices) {
            if (atacante == null) throw new IllegalArgumentException("Atacante no puede ser nulo en TurnAction.");
            
            this.atacante = atacante;
            this.objetivoPrincipal = objetivoPrincipal; // Puede ser null para movimientos sin objetivo directo como Autodestrucción
            this.movimientoIdx = movimientoIdx;
            // Asegurarse de que targetIndices nunca sea null internamente.
            this.targetIndices = (targetIndices != null) ? new ArrayList<>(targetIndices) : new ArrayList<>();

            if (movimientoIdx >= 0 && atacante.getMovimientos() != null && movimientoIdx < atacante.getMovimientos().size()) {
                this.movimientoCache = atacante.getMovimientos().get(movimientoIdx);
            } else {
                // Esto podría indicar que se usará "Lucha" (Struggle) o es un error.
                // ActionExecutor deberá manejar un movimientoCache nulo.
                System.err.println("Advertencia en TurnAction: Índice de movimiento inválido (" + movimientoIdx + ") para " + atacante.getNombre() + ". Movimiento será null.");
                this.movimientoCache = null; 
            }
        }

        public Pokemon getAtacante() { return atacante; }
        public Pokemon getObjetivoPrincipal() { return objetivoPrincipal; }
        public Movements getMovimiento() { return movimientoCache; }
        public int getMovimientoIdx() { return movimientoIdx; }
        
        /**
         * Devuelve la lista de índices de los objetivos.
         * @return Una lista de enteros; puede estar vacía pero no será nula.
         */
        public List<Integer> getTargetIndices() {
            return targetIndices;
        }

        @Override
        public String toString() {
            String movNombre = "N/A (Índice: " + movimientoIdx + ")";
            if (movimientoCache != null) {
                movNombre = movimientoCache.getNombre();
            } else if (movimientoIdx < 0) { // Podría ser un indicador de no acción o Lucha
                 movNombre = "Acción especial/Lucha (idx < 0)";
            }

            return "TurnAction{" +
                   "atacante=" + (atacante != null ? atacante.getNombre() : "null") +
                   ", objetivoPrincipal=" + (objetivoPrincipal != null ? objetivoPrincipal.getNombre() : "null") +
                   ", movimiento=" + movNombre +
                   ", targetsIdx=" + targetIndices +
                   '}';
        }
    }
}
