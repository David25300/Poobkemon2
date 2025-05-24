package PokeBody.Data;

import PokeBody.domain.Type;
import java.util.List;

/**
 * DTO para cargar datos base de Pokémon desde JSON.
 * Ahora almacena estadísticas BASE en lugar de finales.
 */
public class PokemonData {
    private String nombre;
    private List<Type.Tipo> tipos;
    // El nivel se asignará al crear el objeto Pokemon, no aquí para los datos base.
    // private int nivel; 

    // Estadísticas BASE
    private int baseHP;
    private int baseAtaque;
    private int baseDefensa;
    private int baseAtaqueEspecial;
    private int baseDefensaEspecial;
    private int baseVelocidad;
    
    // hpActual no suele ser parte de los datos base de una especie. Se inicializa al crear el Pokemon.
    // private int hpActual; 

    private List<String> movimientoNombres;

    // Constructor por defecto (requerido por Jackson)
    public PokemonData() {}

    // --- Getters para Estadísticas Base ---
    public String getNombre() { return nombre; }
    public List<Type.Tipo> getTipos() { return tipos; }
    public int getBaseHP() { return baseHP; }
    public int getBaseAtaque() { return baseAtaque; }
    public int getBaseDefensa() { return baseDefensa; }
    public int getBaseAtaqueEspecial() { return baseAtaqueEspecial; }
    public int getBaseDefensaEspecial() { return baseDefensaEspecial; }
    public int getBaseVelocidad() { return baseVelocidad; }
    public List<String> getMovimientoNombres() { return movimientoNombres; }

    // --- Setters para Estadísticas Base (requeridos por Jackson) ---
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipos(List<Type.Tipo> tipos) { this.tipos = tipos; }
    public void setBaseHP(int baseHP) { this.baseHP = baseHP; }
    public void setBaseAtaque(int baseAtaque) { this.baseAtaque = baseAtaque; }
    public void setBaseDefensa(int baseDefensa) { this.baseDefensa = baseDefensa; }
    public void setBaseAtaqueEspecial(int baseAtaqueEspecial) { this.baseAtaqueEspecial = baseAtaqueEspecial; }
    public void setBaseDefensaEspecial(int baseDefensaEspecial) { this.baseDefensaEspecial = baseDefensaEspecial; }
    public void setBaseVelocidad(int baseVelocidad) { this.baseVelocidad = baseVelocidad; }
    public void setMovimientoNombres(List<String> movimientoNombres) { this.movimientoNombres = movimientoNombres; }
}