package PokeBody.Services; // O el paquete que prefieras, ej: PokeBody.domain.util

/**
 * Contiene las fórmulas estándar para calcular las estadísticas de un Pokémon
 * basadas en su nivel, estadísticas base, IVs, EVs y naturaleza.
 */
public class StatFormulas {

    // Valores por defecto para IVs y EVs si no se implementan completamente.
    // En los juegos, los IVs van de 0 a 31. Los EVs tienen un máximo por stat y total.
    public static final int DEFAULT_IV = 15; // Un valor promedio para simplificar
    public static final int DEFAULT_EV = 0;  // Asumimos 0 EVs para simplificar

    /**
     * Calcula los PS (Puntos de Salud) máximos de un Pokémon.
     * Fórmula estándar: HP = floor( ( (2 * BaseHP + IV + floor(EV/4) ) * Nivel) / 100 ) + Nivel + 10
     * Caso especial: Shedinja siempre tiene 1 HP.
     *
     * @param baseHP La estadística base de HP del Pokémon.
     * @param nivel El nivel del Pokémon (1-100).
     * @param iv El Valor Individual de HP (0-31).
     * @param ev Los Puntos de Esfuerzo en HP (0-252).
     * @return Los PS máximos calculados.
     */
    public static int calcularHpMax(int baseHP, int nivel, int iv, int ev) {
        // Caso especial para Shedinja, cuya base HP es 1.
        if (baseHP == 1) {
            return 1;
        }
        // Asegurar que el nivel esté dentro de los límites
        nivel = Math.max(1, Math.min(100, nivel));

        double term1 = Math.floor(((2.0 * baseHP + iv + Math.floor(ev / 4.0)) * nivel) / 100.0);
        return (int) (term1 + nivel + 10);
    }

    /**
     * Calcula cualquier otra estadística (Ataque, Defensa, Ataque Especial, Defensa Especial, Velocidad).
     * Fórmula estándar: Stat = floor( ( floor( ( (2 * BaseStat + IV + floor(EV/4) ) * Nivel) / 100 ) + 5 ) * ModNaturaleza )
     *
     * @param baseStat La estadística base (ej. Ataque Base).
     * @param nivel El nivel del Pokémon (1-100).
     * @param iv El Valor Individual de la estadística (0-31).
     * @param ev Los Puntos de Esfuerzo en la estadística (0-252).
     * @param modificadorNaturaleza El modificador de naturaleza (generalmente 0.9, 1.0, o 1.1).
     * @return La estadística calculada.
     */
    public static int calcularOtraStat(int baseStat, int nivel, int iv, int ev, double modificadorNaturaleza) {
        // Asegurar que el nivel esté dentro de los límites
        nivel = Math.max(1, Math.min(100, nivel));

        double term1 = Math.floor(((2.0 * baseStat + iv + Math.floor(ev / 4.0)) * nivel) / 100.0);
        double term2 = Math.floor(term1) + 5;
        return (int) Math.floor(term2 * modificadorNaturaleza);
    }

    // --- Sobrecargas con valores por defecto para IVs, EVs y Naturaleza Neutra ---

    /**
     * Calcula HP Máx con IVs y EVs por defecto.
     */
    public static int calcularHpMax(int baseHP, int nivel) {
        return calcularHpMax(baseHP, nivel, DEFAULT_IV, DEFAULT_EV);
    }

    /**
     * Calcula otra estadística con IVs, EVs y Naturaleza neutra por defecto.
     */
    public static int calcularOtraStat(int baseStat, int nivel) {
        return calcularOtraStat(baseStat, nivel, DEFAULT_IV, DEFAULT_EV, 1.0); // 1.0 para naturaleza neutra
    }
}