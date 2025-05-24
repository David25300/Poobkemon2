// Archivo: PokeBody/Services/DamageCalculator.java
package PokeBody.Services;

import java.util.List;
import java.util.Random;

import PokeBody.domain.Movements; // Necesario para StatBoosts.Stat
import PokeBody.domain.Pokemon;
import PokeBody.domain.StatBoosts;
import PokeBody.domain.Type;

/**
 * Clase especializada en calcular el daño entre Pokémon.
 * Implementa fórmulas de daño, considerando:
 * - STAB (Same-Type Attack Bonus)
 * - Efectividad de tipos
 * - Golpes críticos (con su interacción con boosts y etapas de crítico)
 * - Variación aleatoria (85–100%)
 * - Quemadura afectando el Ataque físico 
 * - Precisión y evasión.
 */
public class DamageCalculator {
    private final Random random;

    // Multiplicador de daño para golpes críticos (Gen VI+)
    private static final double MULTIPLICADOR_CRITICO = 1.5;

    // Probabilidades de golpe crítico según la etapa (Gen VI: 1/16, 1/8, 1/2, 1/1)
    // Gen VII+ es 1/24, 1/8, 1/2, 1/1. Usaremos Gen VI por simplicidad con Foco Energía.
    private static final double[] PROBABILIDADES_CRITICO_POR_ETAPA = {1.0/16.0, 1.0/8.0, 1.0/2.0, 1.0/1.0};


    /** Constructor por defecto */
    public DamageCalculator() {
        this.random = new Random();
    }

    /** Constructor con semilla para pruebas */
    public DamageCalculator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Pre-calcula y almacena el daño teórico de cada movimiento
     * para propósitos de visualización en la UI. No considera críticos para esta estimación.
     * @param atacante El Pokémon atacante.
     * @param defensor El Pokémon defensor.
     */
    public void precalcularDanioMovimientos(Pokemon atacante, Pokemon defensor) {
        if (atacante == null || defensor == null || atacante.getMovimientos() == null) return;

        for (Movements mov : atacante.getMovimientos()) {
            if (mov != null && mov.getCategoria() != Movements.TipoAtaque.ESTADO) {
                // Para la UI, calculamos el daño base sin asumir crítico.
                int danioEstimado = calcularDanioBase(atacante, defensor, mov, false);
                mov.setDanoCalculado(danioEstimado);
            } else if (mov != null) {
                 mov.setDanoCalculado(0); // Movimientos de estado causan 0 daño directo.
            }
        }
    }

    /**
     * Obtiene la estadística de ataque efectiva del atacante para el cálculo de daño.
     * Considera si el golpe es crítico (ignora drops de ataque) y el estado de quemadura.
     * @param atacante Pokémon atacante.
     * @param movimiento Movimiento usado.
     * @param esCritico Si el golpe es crítico.
     * @return La estadística de ataque efectiva.
     */
    private int getAtaqueEfectivo(Pokemon atacante, Movements movimiento, boolean esCritico) {
        int statAtaqueBase;
        StatBoosts.Stat statRelevante;

        if (movimiento.getCategoria() == Movements.TipoAtaque.FISICO) {
            statAtaqueBase = atacante.getAtaque(); // Stat escalada según nivel
            statRelevante = StatBoosts.Stat.ATTACK;
        } else { // ESPECIAL
            statAtaqueBase = atacante.getAtaqueEspecial(); // Stat escalada según nivel
            statRelevante = StatBoosts.Stat.SP_ATTACK;
        }

        // Aplicar boosts de combate
        double multiplicadorBoost = atacante.getStatBoosts().getMultiplier(statRelevante);
        if (esCritico && atacante.getStatBoosts().getLevel(statRelevante) < 0) {
            multiplicadorBoost = 1.0; // Críticos ignoran drops de ataque del usuario
        }
        int ataqueConBoosts = (int) (statAtaqueBase * multiplicadorBoost);

        // Aplicar reducción por quemadura a movimientos físicos
        if (movimiento.getCategoria() == Movements.TipoAtaque.FISICO &&
            "QUEMADURA".equals(atacante.getEstado()) &&
            !atacante.tieneModificadorTemporal("AGALLAS_ACTIVA")) { // Asumiendo un flag para Agallas
            ataqueConBoosts /= 2;
        }
        return Math.max(1, ataqueConBoosts);
    }

    /**
     * Obtiene la estadística de defensa efectiva del defensor para el cálculo de daño.
     * Considera si el golpe es crítico (ignora boosts de defensa).
     * @param defensor Pokémon defensor.
     * @param movimiento Movimiento usado.
     * @param esCritico Si el golpe es crítico.
     * @return La estadística de defensa efectiva.
     */
    private int getDefensaEfectiva(Pokemon defensor, Movements movimiento, boolean esCritico) {
        int statDefensaBase;
        StatBoosts.Stat statRelevante;

        if (movimiento.getCategoria() == Movements.TipoAtaque.FISICO) {
            statDefensaBase = defensor.getDefensa(); // Stat escalada según nivel
            statRelevante = StatBoosts.Stat.DEFENSE;
        } else { // ESPECIAL
            statDefensaBase = defensor.getDefensaEspecial(); // Stat escalada según nivel
            statRelevante = StatBoosts.Stat.SP_DEFENSE;
        }

        // Aplicar boosts de combate
        double multiplicadorBoost = defensor.getStatBoosts().getMultiplier(statRelevante);
        if (esCritico && defensor.getStatBoosts().getLevel(statRelevante) > 0) {
            multiplicadorBoost = 1.0; // Críticos ignoran boosts de defensa del objetivo
        }
        int defensaConBoosts = (int) (statDefensaBase * multiplicadorBoost);
        return Math.max(1, defensaConBoosts);
    }


    /**
     * Calcula el daño base de un movimiento, antes de la variación aleatoria y el multiplicador de crítico final.
     * Este método considera las interacciones de críticos con los boosts de stats.
     *
     * @param atacante El Pokémon atacante.
     * @param defensor El Pokémon defensor.
     * @param movimiento El movimiento que se usa.
     * @param esCritico Indica si el golpe es crítico (para ajustar stats según reglas de críticos).
     * @return El daño base calculado.
     */
    private int calcularDanioBase(Pokemon atacante, Pokemon defensor, Movements movimiento, boolean esCritico) {
        if (atacante == null || defensor == null || movimiento == null || movimiento.getPotencia() <= 0) {
            return 0; // No se puede calcular o el movimiento no tiene poder.
        }
        if (movimiento.getCategoria() == Movements.TipoAtaque.ESTADO) {
            return 0; // Movimientos de estado no usan esta fórmula de daño.
        }

        int ataqueStat = getAtaqueEfectivo(atacante, movimiento, esCritico);
        int defensaStat = getDefensaEfectiva(defensor, movimiento, esCritico);

        // Componente de Nivel
        double componenteNivel = (2.0 * atacante.getNivel() / 5.0) + 2.0;

        // Componente de Poder del Movimiento
        double componentePoder = movimiento.getPotencia();

        // Ratio de Estadísticas
        double ratioStats = (double) ataqueStat / Math.max(1, defensaStat); // Evitar división por cero

        // Cálculo de Daño Principal
        double danoPrincipal = (((componenteNivel * componentePoder * ratioStats) / 50.0) + 2.0);

        // --- Modificadores ---
        // STAB (Same-Type Attack Bonus)
        double modificadorStab = (atacante.tieneTipo(movimiento.getTipo())) ? 1.5 : 1.0;

        // Efectividad de Tipos
        double modificadorEfectividad = obtenerMultiplicadorEfectividad(atacante, defensor, movimiento);
        if (modificadorEfectividad == 0) { // Inmunidad total
            return 0;
        }
        
        // Otros Modificadores (ej. Objetos como Choice Band/Specs, Habilidades como Experto, Clima)
        // double otrosModificadores = 1.0; // Placeholder

        double danoModificado = danoPrincipal * modificadorStab * modificadorEfectividad; // * otrosModificadores;

        return Math.max(1, (int) Math.floor(danoModificado)); // Asegurar al menos 1 de daño si golpea y es efectivo.
    }

    /**
     * Determina si el ataque resulta en un golpe crítico.
     * Considera la etapa de crítico del Pokémon atacante.
     * @param atacante El Pokémon atacante.
     * @return true si es un golpe crítico, false en caso contrario.
     */
    public boolean esGolpeCritico(Pokemon atacante) {
        if (atacante == null) return false;
        int etapaCritico = atacante.getCritHitStage(); 
        etapaCritico = Math.max(0, Math.min(etapaCritico, PROBABILIDADES_CRITICO_POR_ETAPA.length - 1)); // Asegurar que la etapa esté en rango

        return random.nextDouble() < PROBABILIDADES_CRITICO_POR_ETAPA[etapaCritico];
    }

    /**
     * Calcula el daño final que se inflige, incluyendo golpe crítico y variación aleatoria.
     * @param atacante El Pokémon atacante.
     * @param defensor El Pokémon defensor.
     * @param movimiento El movimiento que se usa.
     * @param esRealmenteCritico Indica si el golpe fue determinado como crítico (después de la tirada de `esGolpeCritico`).
     * @return La cantidad final de daño.
     */
    public int calcularDanioReal(Pokemon atacante, Pokemon defensor, Movements movimiento, boolean esRealmenteCritico) {
        // Calcular daño base, considerando ya las reglas de crítico para boosts de stats
        int danoBase = calcularDanioBase(atacante, defensor, movimiento, esRealmenteCritico);
        if (danoBase == 0) return 0; // Si es inmune o el movimiento no hace daño.

        // Aplicar multiplicador de crítico si corresponde
        double multiplicadorCriticoFinal = esRealmenteCritico ? MULTIPLICADOR_CRITICO : 1.0;

        // Aplicar variación aleatoria (85% a 100%)
        double modificadorAleatorio = 0.85 + (random.nextDouble() * 0.15); // Valor entre 0.85 y 1.00

        double danoFinal = danoBase * multiplicadorCriticoFinal * modificadorAleatorio;

        return Math.max(1, (int) Math.floor(danoFinal)); // Asegurar al menos 1 de daño.
    }
    
    /**
     * Calcula el daño que un Pokémon se inflige a sí mismo por confusión.
     * Es un ataque físico sin tipo de 40 de poder base.
     * @param pokemon El Pokémon confuso.
     * @return El daño calculado por confusión.
     */
    public int calcularDanioConfusion(Pokemon pokemon) {
        if (pokemon == null) return 0;

        // Crear un "movimiento de confusión" temporal para usar la fórmula de daño.
        // No tiene tipo elemental (o a veces se considera Normal, pero no interactúa con efectividades).
        // La confusión no recibe STAB.
        int poderConfusion = 40;
        int nivel = pokemon.getNivel();
        
        // Para la confusión, el atacante y el defensor son el mismo Pokémon.
        // Se usan el Ataque y Defensa físicos del Pokémon.
        // Los boosts se aplican normalmente (no hay reglas especiales de crítico para el autogolpe).
        int ataqueStat = pokemon.getAtaqueModificado(); // Ataque con boosts de combate
        int defensaStat = pokemon.getDefensaModificada();  // Defensa con boosts de combate
           
        // Fórmula de daño simplificada (similar a la principal)
        double componenteNivel = (2.0 * nivel / 5.0) + 2.0;
        double ratioStats = (double) ataqueStat / Math.max(1, defensaStat);
        double danoPrincipal = (((componenteNivel * poderConfusion * ratioStats) / 50.0) + 2.0);
        
        // Variación aleatoria
        double modificadorAleatorio = 0.85 + (random.nextDouble() * 0.15);
        double danoFinal = danoPrincipal * modificadorAleatorio;

        return Math.max(1, (int) Math.floor(danoFinal));
    }


    /**
     * Determina si un movimiento impacta basado en su precisión, la precisión del usuario
     * y la evasión del objetivo.
     * @param mov El movimiento que se usa.
     * @param usuario El Pokémon que usa el movimiento.
     * @param objetivo El Pokémon objetivo.
     * @return true si el movimiento impacta, false en caso contrario.
     */
    public boolean movimientoImpacta(Movements mov, Pokemon usuario, Pokemon objetivo) {
        if (mov == null || usuario == null || objetivo == null) return false;

        int precisionBase = mov.getPrecision();
        if (precisionBase <= 0) return true; // Movimientos con precisión 0 o negativa siempre golpean (ej. Aerial Ace)

        // Obtener boosts de precisión del usuario y evasión del objetivo
        int etapaPrecisionUsuario = usuario.getPrecisionBoost();
        int etapaEvasionObjetivo = objetivo.getEvasionBoost();

        // Calcular multiplicador de precisión/evasión
        // Fórmula común: Multiplicador = BaseAcc * (ModAccUsuario / ModEvaObjetivo)
        // Modificador de etapa: Si Etapa >= 0 -> (3 + Etapa) / 3; Si Etapa < 0 -> 3 / (3 - Etapa)
        double modAccUsuario = calcularMultiplicadorAccEvaEtapa(etapaPrecisionUsuario);
        double modEvaObjetivo = calcularMultiplicadorAccEvaEtapa(etapaEvasionObjetivo);
        
        // Precisión efectiva
        // Algunas habilidades/items pueden modificar esto (ej. Vista Lince ignora boosts de evasión)
        double precisionEfectiva = precisionBase * (modAccUsuario / modEvaObjetivo);
        
        // La precisión no puede superar el 100% (a menos que sea un movimiento que nunca falla)
        // precisionEfectiva = Math.min(100.0, precisionEfectiva);

        return random.nextDouble() * 100.0 < precisionEfectiva;
    }

    /**
     * Calcula el multiplicador para una etapa de precisión o evasión.
     * @param etapa El nivel de boost/debuff (-6 a +6).
     * @return El multiplicador correspondiente.
     */
    private double calcularMultiplicadorAccEvaEtapa(int etapa) {
        etapa = Math.max(-6, Math.min(6, etapa)); // Asegurar que la etapa esté en el rango -6 a +6
        if (etapa >= 0) {
            return (3.0 + etapa) / 3.0;
        } else {
            // Para etapas negativas, el denominador es (3 - etapaNegativa)
            // Ejemplo: etapa -1 -> 3 / (3 - (-1)) = 3/4
            return 3.0 / (3.0 - etapa);
        }
    }

    /**
     * Obtiene el multiplicador de efectividad de tipo para un movimiento contra un defensor.
     * @param atacante El Pokémon atacante (puede ser relevante para ciertas habilidades).
     * @param defensor El Pokémon defensor.
     * @param movimiento El movimiento que se usa.
     * @return El multiplicador de efectividad (ej. 0.5, 1.0, 2.0, 4.0).
     */
    public double obtenerMultiplicadorEfectividad(Pokemon atacante, Pokemon defensor, Movements movimiento) {
        if (defensor == null || movimiento == null) return 1.0;

        List<Type.Tipo> tiposDefensor = defensor.getTipos();
        Type.Tipo tipoMovimiento = movimiento.getTipo();

        if (tiposDefensor == null || tiposDefensor.isEmpty()) {
            return 1.0; // Si el defensor no tiene tipos (no debería pasar)
        }

        // Habilidad "Intrépido" (Scrappy) permite a movimientos Normal y Lucha golpear Fantasmas
        // if (atacante.tieneHabilidad("Intrepido") && 
        //     (tipoMovimiento == Type.Tipo.NORMAL || tipoMovimiento == Type.Tipo.LUCHA) &&
        //     defensor.tieneTipo(Type.Tipo.FANTASMA)) {
        //     // Se calcula la efectividad como si Fantasma no fuera inmune
        //     double efectividadContraOtrosTipos = 1.0;
        //     if (tiposDefensor.size() > 1) {
        //          Type.Tipo otroTipo = tiposDefensor.get(0) == Type.Tipo.FANTASMA ? tiposDefensor.get(1) : tiposDefensor.get(0);
        //          efectividadContraOtrosTipos = Type.obtenerMultiplicador(tipoMovimiento, otroTipo, null);
        //     }
        //     return efectividadContraOtrosTipos; // Devuelve la efectividad contra el/los otro(s) tipo(s)
        // }


        Type.Tipo tipoDefensor1 = tiposDefensor.get(0);
        Type.Tipo tipoDefensor2 = (tiposDefensor.size() > 1) ? tiposDefensor.get(1) : null;

        return Type.obtenerMultiplicador(tipoMovimiento, tipoDefensor1, tipoDefensor2);
    }
}
