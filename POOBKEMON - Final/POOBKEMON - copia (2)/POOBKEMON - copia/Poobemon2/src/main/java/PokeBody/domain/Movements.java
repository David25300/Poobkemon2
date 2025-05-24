// Archivo: PokeBody/domain/Movements.java
package PokeBody.domain;

import PokeBody.Data.MovementsData;
import PokeBody.domain.efectos.ModificarStatEfecto; // Importar la clase específica

public class Movements {

    public enum TipoAtaque { FISICO, ESPECIAL, ESTADO }

    private final String nombre;
    private final Type.Tipo tipo;
    private final TipoAtaque categoria;
    private final int potencia;
    private final int precision;
    private final int ppMax;
    private int ppActual;
    private final int prioridad;
    private final Efecto efecto; 
    private final StatBoosts.Stat boostStat; // Stat que el movimiento podría boostear directamente (independiente del 'efecto')
    private final int boostAmount;   // Cantidad de ese boost directo

    private transient int danoCalculado; // Para UI, no persistente

    public static final Movements FUERZAGEO = new Movements(
        "Forcejeo",
        Type.Tipo.NORMAL,
        TipoAtaque.FISICO,
        50,
        0, 
        1, 
        0,
        null, 
        null,
        0
    );

    public static Movements fromData(MovementsData data) {
        StatBoosts.Stat statEnumForDirectBoost = null;
        if (data.getBoostAttribute() != null && !data.getBoostAttribute().isEmpty()) {
            try {
                // Este boostAttribute en MovementsData se refiere al boost directo del movimiento,
                // no necesariamente al que está encapsulado dentro de un objeto Efecto complejo.
                statEnumForDirectBoost = StatBoosts.Stat.valueOf(data.getBoostAttribute().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Advertencia: Atributo de boost directo inválido '" + data.getBoostAttribute() + "' para movimiento '" + data.getNombre() + "'.");
            }
        }

        TipoAtaque categoryEnum;
        try {
            categoryEnum = TipoAtaque.valueOf(data.getTipoAtaque().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
             System.err.println("Advertencia: Categoría inválida o nula '" + data.getTipoAtaque() + "' para movimiento '" + data.getNombre() + "'. Defecto a ESTADO.");
             categoryEnum = TipoAtaque.ESTADO;
        }

        // Crear el objeto Efecto basado en efectoNombre, probabilidadEfecto,
        // y potencialmente los campos de boost si el efecto nombrado es un ModificarStatEfecto.
        Efecto efectoObjeto = Efecto.fromMovimientoData(
            data.getEfectoNombre(),
            data.getProbabilidadEfecto(), 
            data.getBoostAttribute(), // Estos se pasan a la fábrica por si el efecto es un ModificarStatEfecto
            data.getBoostAmount()     // y la fábrica los usa para configurar ese efecto específico.
        );
        
        // Si el efectoObjeto es una instancia de ModificarStatEfecto,
        // significa que el efecto *principal* del movimiento es modificar stats.
        // En este caso, los campos boostStat y boostAmount de la clase Movements
        // podrían ser redundantes o usarse para un boost *adicional* y separado.
        // La línea original `(efectoObjeto instanceof ModificarStatEfecto) ? 0 : data.getBoostAmount()`
        // intentaba evitar duplicar el boostAmount si ya estaba manejado por el efecto.
        // Vamos a mantener la lógica de que si el efecto principal es un ModificarStatEfecto,
        // los campos directos boostStat/boostAmount del movimiento no se usan para ese mismo cambio de stat.
        // Si un movimiento tiene un efecto (ej. VENENO) Y un boost directo a una stat (ej. subir ATK propio),
        // entonces boostStat y boostAmount en Movements SÍ se usarían.

        int directBoostAmountToStore = data.getBoostAmount();
        StatBoosts.Stat directBoostStatToStore = statEnumForDirectBoost;

        if (efectoObjeto instanceof ModificarStatEfecto) {
            // Si el efecto principal es modificar una stat, y esa stat es la misma
            // que el boost directo definido en movements.json, entonces no almacenamos el boost directo
            // para evitar aplicarlo dos veces (una por el efecto, otra por el movimiento).
            ModificarStatEfecto mse = (ModificarStatEfecto) efectoObjeto;
            if (mse.getStatAfectada() == statEnumForDirectBoost && mse.getNiveles() == data.getBoostAmount()) {
                directBoostAmountToStore = 0; // Ya está manejado por el efecto
                directBoostStatToStore = null;
            }
        }


        return new Movements(
            data.getNombre(),
            data.getTipoBase(),
            categoryEnum,
            data.getPotencia(),
            data.getPrecision(),
            data.getPpInicial(),
            data.getPrioridad(),
            efectoObjeto, 
            directBoostStatToStore, 
            directBoostAmountToStore
        );
    }

    public Movements(String nombre, Type.Tipo tipo, TipoAtaque categoria,
                     int potencia, int precision, int ppMax, int prioridad,
                     Efecto efecto, 
                     StatBoosts.Stat boostStat, int boostAmount) {

        if (categoria == TipoAtaque.ESTADO && potencia != 0) {
            // Permitido para movimientos como Tinieblas, Mov. Sísmico
        }
        if (precision < 0 || precision > 100) {
             if (precision != 0) { // Precisión 0 puede significar "nunca falla" o que no aplica (mov. de estado)
                // Para movimientos de estado que no fallan, la precisión suele ser 0 o no especificada.
                // Para movimientos ofensivos, 0 usualmente significa que siempre golpean.
                // Si es un movimiento de estado que no tiene chequeo de precisión, no lanzar error.
                if (categoria != TipoAtaque.ESTADO) {
                    // throw new IllegalArgumentException("Precisión debe estar entre 0 y 100 para movimiento ofensivo: " + nombre + " (valor: " + precision + ")");
                    // System.err.println("Advertencia: Precisión fuera de rango (0-100) para movimiento ofensivo: " + nombre + " (valor: " + precision + "). Asumiendo 100 si es >100.");
                    // this.precision = Math.min(100, Math.max(0,precision)); // Clamp
                }
             }
        }
        // La validación de precisión se mantiene como estaba, pero se comenta el throw para flexibilidad.
        // Si se quiere ser estricto, descomentar el throw.
        // Por ahora, se asume que los datos de entrada son correctos o se manejan en otro lugar.

        if (prioridad < -7 || prioridad > 5) {
            throw new IllegalArgumentException("Prioridad inválida (" + prioridad + ") para movimiento: " + nombre);
        }
        if (ppMax <= 0 && !nombre.equals("Forcejeo")) { 
            throw new IllegalArgumentException("PP Max debe ser positivo para movimiento: " + nombre);
        }

        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.potencia = potencia;
        this.precision = precision; // Se asigna el valor original
        this.ppMax = ppMax;
        this.ppActual = ppMax;
        this.prioridad = prioridad;
        this.efecto = efecto; 
        this.boostStat = boostStat;
        this.boostAmount = boostAmount;
        this.danoCalculado = 0;
    }

    public Movements(Movements other) {
        this.nombre = other.nombre;
        this.tipo = other.tipo;
        this.categoria = other.categoria;
        this.potencia = other.potencia;
        this.precision = other.precision;
        this.ppMax = other.ppMax;
        this.ppActual = other.ppMax; // PP se resetea para la nueva instancia (o other.ppActual si se quiere copiar el PP actual)
        this.prioridad = other.prioridad;
        this.efecto = other.efecto; 
        this.boostStat = other.boostStat;
        this.boostAmount = other.boostAmount;
        this.danoCalculado = 0;
    }


    public void usar() {
        if (this.ppActual > 0) {
            this.ppActual--;
        }
    }

    public void restaurarPP() {
        this.ppActual = this.ppMax;
    }
    public void restaurarPP(int amount) {
        this.ppActual = Math.min(this.ppMax, this.ppActual + amount);
    }
    public boolean puedeUsarse() {
        return this.ppActual > 0;
    }

    public void setDanoCalculado(int dano) {
        this.danoCalculado = dano;
    }
    public int getDanoCalculado() {
        return danoCalculado;
    }

    // applyBoost y applySelfBoost podrían ser eliminados si toda la lógica de cambio de stats
    // se maneja a través del sistema de Efectos.
    // Si se mantienen, es para movimientos que tienen un cambio de stat *directo*
    // además de cualquier otro efecto nombrado.
    public void applyBoost(Pokemon target) { 
        if (this.boostStat != null && this.boostAmount != 0 && target != null) {
            target.modificarStatBoost(this.boostStat, this.boostAmount);
        }
    }
    public void applySelfBoost(Pokemon user) { 
         if (this.boostStat != null && this.boostAmount != 0 && user != null) {
            user.modificarStatBoost(this.boostStat, this.boostAmount);
         }
    }

    public String getNombre() { return nombre; }
    public Type.Tipo getTipo() { return tipo; }
    public TipoAtaque getCategoria() { return categoria; }
    public int getPotencia() { return potencia; }
    public int getPrecision() { return precision; }
    public int getPpMax() { return ppMax; }
    public int getPpActual() { return ppActual; }
    public void setPpActual(int pp) { this.ppActual = Math.max(0, Math.min(ppMax, pp));}
    public int getPrioridad() { return prioridad; }
    
    public Efecto getEfecto() { return efecto; }
    public boolean tieneEfecto() { return efecto != null; }
    
    public StatBoosts.Stat getBoostStat() { return boostStat; }
    public int getBoostAmount() { return boostAmount; }


    @Override
    public String toString() {
        String efectoStr = (efecto != null && efecto.getNombreEfecto() != null) ? efecto.getNombreEfecto() : "None";
        double probEfecto = (efecto != null) ? efecto.getProbabilidad() * 100 : 0.0;

        return String.format(
            "%s (%s/%s) PP:%d/%d Pow:%d Acc:%d Pri:%d Effect:(%s, %.0f%%) Boost:(%s,%+d)",
            nombre, tipo, categoria, ppActual, ppMax, potencia, precision, prioridad,
            efectoStr, probEfecto,
            (boostStat != null ? boostStat.name() : "None"), boostAmount);
    }
}
