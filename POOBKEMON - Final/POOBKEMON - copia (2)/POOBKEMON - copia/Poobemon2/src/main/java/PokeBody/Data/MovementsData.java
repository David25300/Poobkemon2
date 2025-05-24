package PokeBody.Data;

import PokeBody.domain.Type;

/**
 * DTO for loading movement data from JSON or external sources.
 * Maps directly to the structure of the movements JSON file.
 */
public class MovementsData {
    private String nombre;
    private Type.Tipo tipoBase;
    private int ppInicial;
    private int potencia;
    private int precision;
    private int prioridad;
    private String tipoAtaque;          // Field for "FISICO", "ESPECIAL", "ESTADO"
    private String efectoNombre;        // Name of the effect (String) or null
    private double probabilidadEfecto;  // 0.0–1.0

    // Optional Buff/Debuff: stat name and amount
    private String boostAttribute;      // Name matching StatBoosts.Stat enum ("ATTACK", "DEFENSE", etc.) or null
    private int boostAmount;            // Boost amount (+/-)

    // Default constructor (required by Jackson)
    public MovementsData() {}

    // --- Getters ---

    public String getNombre() {
        return nombre;
    }

    public Type.Tipo getTipoBase() {
        return tipoBase;
    }

    public int getPpInicial() {
        return ppInicial;
    }

    public int getPotencia() {
        return potencia;
    }

    public int getPrecision() {
        return precision;
    }

    public int getPrioridad() {
        return prioridad;
    }

    /**
     * Getter for the attack category ("FISICO", "ESPECIAL", "ESTADO").
     * Renamed from getCategoria() for consistency with the field name "tipoAtaque".
     * @return The attack category as a String.
     */
    public String getTipoAtaque() { // Renamed from getCategoria
        return tipoAtaque;
    }

    /**
     * Getter for the effect name (String).
     * Note: The actual Efecto object linking happens later (e.g., in Movements.fromData).
     * @return The name of the effect, or null.
     */
    public String getEfectoNombre() { // Renamed for clarity
        return efectoNombre;
    }

    public double getProbabilidadEfecto() {
        return probabilidadEfecto;
    }

    /**
     * @return Stat attribute name to boost (matching StatBoosts.Stat), or null if none.
     */
    public String getBoostAttribute() {
        return boostAttribute;
    }

    /**
     * @return Amount of boost to apply (can be negative).
     */
    public int getBoostAmount() {
        return boostAmount;
    }

    // --- Setters (Required by Jackson for deserialization) ---

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setTipoBase(Type.Tipo tipoBase) {
        this.tipoBase = tipoBase;
    }

    public void setPpInicial(int ppInicial) {
        this.ppInicial = ppInicial;
    }

    public void setPotencia(int potencia) {
        this.potencia = potencia;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    /**
     * Setter for the attack category field. Required by Jackson.
     * @param tipoAtaque The attack category string ("FISICO", "ESPECIAL", "ESTADO").
     */
    public void setTipoAtaque(String tipoAtaque) { // Added Setter
        this.tipoAtaque = tipoAtaque;
    }

    /**
     * Setter for the effect name field. Required by Jackson.
     * @param efectoNombre The name of the effect (String).
     */
    public void setEfectoNombre(String efectoNombre) { // Added Setter (renamed for clarity)
        this.efectoNombre = efectoNombre;
    }

    public void setProbabilidadEfecto(double probabilidadEfecto) {
        this.probabilidadEfecto = probabilidadEfecto;
    }

    public void setBoostAttribute(String boostAttribute) {
        this.boostAttribute = boostAttribute;
    }

    public void setBoostAmount(int boostAmount) {
        this.boostAmount = boostAmount;
    }
}
      