package PokeBody.domain;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class StatBoosts implements Serializable {
    private static final long serialVersionUID = 1L; // Importante para la serialización
    private final Map<Stat, Integer> boosts;

    public enum Stat {
        ATTACK, DEFENSE, SP_ATTACK, SP_DEFENSE, SPEED
    }

    public StatBoosts() {
        boosts = new EnumMap<>(Stat.class);
        reset();
    }

    /**
     * Constructor de copia.
     * @param other El objeto StatBoosts a copiar.
     */
    public StatBoosts(StatBoosts other) {
        this(); // Llama al constructor por defecto para inicializar el mapa
        if (other != null && other.boosts != null) {
            this.boosts.putAll(other.boosts);
        }
    }

    public void increase(Stat stat, int amount) {
        if (amount <= 0) return;
        int oldLevel = boosts.getOrDefault(stat, 0);
        int newLevel = Math.max(-6, Math.min(6, oldLevel + amount));
        boosts.put(stat, newLevel);
    }

    public void decrease(Stat stat, int amount) {
        if (amount <= 0) return;
        int oldLevel = boosts.getOrDefault(stat, 0);
        int newLevel = Math.max(-6, Math.min(6, oldLevel - amount));
        boosts.put(stat, newLevel);
    }

    public int getLevel(Stat stat) {
        return boosts.getOrDefault(stat, 0);
    }

    public double getMultiplier(Stat stat) {
        int boostLevel = getLevel(stat);
        if (boostLevel >= 0) {
            return (2.0 + boostLevel) / 2.0;
        } else {
            return 2.0 / (2.0 - boostLevel);
        }
    }

    public void reset() {
        for (Stat s : Stat.values()) {
            boosts.put(s, 0);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Boosts{");
        boolean first = true;
        for (Map.Entry<Stat, Integer> entry : boosts.entrySet()) {
            if (entry.getValue() != 0) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(String.format("%+d", entry.getValue()));
                first = false;
            }
        }
        if (first) {
            sb.append("None");
        }
        sb.append("}");
        return sb.toString();
    }
}
