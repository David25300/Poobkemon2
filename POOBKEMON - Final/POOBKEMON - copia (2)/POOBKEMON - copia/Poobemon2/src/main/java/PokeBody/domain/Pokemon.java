// Archivo: PokeBody/domain/Pokemon.java
package PokeBody.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import PokeBody.Data.PokemonData; 
import PokeBody.Services.StatFormulas;

public class Pokemon {
    // --- Atributos de Identidad y Nivel ---
    private final int id; 
    private String nombre; 
    private int nivel;

    // --- Atributos de Tipo ---
    private List<Type.Tipo> tipos;

    // --- Estadísticas Base (Originales y Actuales) ---
    private final int initialBaseHP;
    private final int initialBaseAtaque;
    private final int initialBaseDefensa;
    private final int initialBaseAtaqueEspecial;
    private final int initialBaseDefensaEspecial;
    private final int initialBaseVelocidad;

    private int baseHP; 
    private int baseAtaque;
    private int baseDefensa;
    private int baseAtaqueEspecial;
    private int baseDefensaEspecial;
    private int baseVelocidad;

    // --- Estadísticas Calculadas ---
    private int hpMax;
    private int hpActual;
    private int ataque;
    private int defensa;
    private int ataqueEspecial;
    private int defensaEspecial;
    private int velocidad;

    // --- IVs, EVs, Naturaleza (Simplificado) ---
    private int ivHp = StatFormulas.DEFAULT_IV;
    private int ivAtaque = StatFormulas.DEFAULT_IV;
    private int ivDefensa = StatFormulas.DEFAULT_IV;
    private int ivAtaqueEspecial = StatFormulas.DEFAULT_IV;
    private int ivDefensaEspecial = StatFormulas.DEFAULT_IV;
    private int ivVelocidad = StatFormulas.DEFAULT_IV;
    private int evHp = StatFormulas.DEFAULT_EV;
    private int evAtaque = StatFormulas.DEFAULT_EV;
    
    private double modNaturalezaAtaque = 1.0;
    private double modNaturalezaDefensa = 1.0;
    private double modNaturalezaAtaqueEspecial = 1.0;
    private double modNaturalezaDefensaEspecial = 1.0;
    private double modNaturalezaVelocidad = 1.0;

    // --- Estados y Modificadores de Combate ---
    private String estadoActivo;
    private int duracionEstado;
    private StatBoosts statBoosts; 
    private int precisionBoost;
    private int evasionBoost;
    private List<Movements> movimientos; 
    private boolean estaRetrocediendo = false;
    private Set<String> modificadoresTemporales = new HashSet<>();
    private int critHitStage = 0;
    private transient Pokemon fuenteDeDrenadoras; 

    // --- Estado de Transformación ---
    private boolean isTransformed = false;
    private String originalNombre; 
    private List<Type.Tipo> originalTipos;
    private List<Movements> originalMovimientos; // Guarda los movimientos originales antes de transformar

    // --- Estado de Protección ---
    private boolean isProtectedThisTurn = false;
    private int consecutiveProtectUses = 0;
    
    private static final Random INTERNAL_RNG = new Random(); 

    public Pokemon(PokemonData data, Map<String, Movements> allMovesMap, int nivelActual) {
        this.id = data.getNombre().hashCode(); 
        this.nombre = data.getNombre();
        this.tipos = new ArrayList<>(data.getTipos()); 
        this.nivel = Math.max(1, Math.min(100, nivelActual));

        this.initialBaseHP = data.getBaseHP();
        this.initialBaseAtaque = data.getBaseAtaque();
        this.initialBaseDefensa = data.getBaseDefensa();
        this.initialBaseAtaqueEspecial = data.getBaseAtaqueEspecial();
        this.initialBaseDefensaEspecial = data.getBaseDefensaEspecial();
        this.initialBaseVelocidad = data.getBaseVelocidad();

        this.setBaseStatsToOriginal();
        
        this.originalNombre = this.nombre; 
        this.originalTipos = new ArrayList<>(this.tipos); 

        recalculateStats(); 

        this.precisionBoost = 0;
        this.evasionBoost = 0;
        this.estadoActivo = null;
        this.duracionEstado = 0;
        this.statBoosts = new StatBoosts(); 
        this.estaRetrocediendo = false;
        this.modificadoresTemporales = new HashSet<>();
        this.critHitStage = 0;
        this.fuenteDeDrenadoras = null;

        this.movimientos = new ArrayList<>(Collections.nCopies(4, null));
        if (data.getMovimientoNombres() != null && allMovesMap != null) {
            int moveSlot = 0;
            for (String moveName : data.getMovimientoNombres()) {
                if (moveSlot >= 4) break; 
                Movements baseMove = allMovesMap.get(moveName);
                if (baseMove != null) {
                    this.movimientos.set(moveSlot, new Movements(baseMove)); 
                }
                moveSlot++;
            }
        }
        // Guardar una copia profunda de los movimientos originales al crear el Pokémon
        this.originalMovimientos = new ArrayList<>(4); 
        for(int i = 0; i < 4; i++) {
            Movements m = (i < this.movimientos.size()) ? this.movimientos.get(i) : null;
            this.originalMovimientos.add(m != null ? new Movements(m) : null);
        }
    }

    public Pokemon(Pokemon other) {
        // ... (constructor de copia, asegurándose de copiar originalMovimientos también)
        this.id = other.id;
        this.nombre = other.nombre;
        this.tipos = new ArrayList<>(other.tipos);
        this.nivel = other.nivel;

        this.initialBaseHP = other.initialBaseHP;
        this.initialBaseAtaque = other.initialBaseAtaque;
        this.initialBaseDefensa = other.initialBaseDefensa;
        this.initialBaseAtaqueEspecial = other.initialBaseAtaqueEspecial;
        this.initialBaseDefensaEspecial = other.initialBaseDefensaEspecial;
        this.initialBaseVelocidad = other.initialBaseVelocidad;

        this.baseHP = other.baseHP;
        this.baseAtaque = other.baseAtaque;
        this.baseDefensa = other.baseDefensa;
        this.baseAtaqueEspecial = other.baseAtaqueEspecial;
        this.baseDefensaEspecial = other.baseDefensaEspecial;
        this.baseVelocidad = other.baseVelocidad;

        this.hpMax = other.hpMax;
        this.hpActual = other.hpActual;
        this.ataque = other.ataque;
        this.defensa = other.defensa;
        this.ataqueEspecial = other.ataqueEspecial;
        this.defensaEspecial = other.defensaEspecial;
        this.velocidad = other.velocidad;

        this.ivHp = other.ivHp;
        this.ivAtaque = other.ivAtaque;
        this.ivDefensa = other.ivDefensa;
        this.ivAtaqueEspecial = other.ivAtaqueEspecial;
        this.ivDefensaEspecial = other.ivDefensaEspecial;
        this.ivVelocidad = other.ivVelocidad;
        this.evHp = other.evHp;
        this.evAtaque = other.evAtaque;
        this.modNaturalezaAtaque = other.modNaturalezaAtaque;
        this.modNaturalezaDefensa = other.modNaturalezaDefensa;
        this.modNaturalezaAtaqueEspecial = other.modNaturalezaAtaqueEspecial;
        this.modNaturalezaDefensaEspecial = other.modNaturalezaDefensaEspecial;
        this.modNaturalezaVelocidad = other.modNaturalezaVelocidad;

        this.estadoActivo = other.estadoActivo;
        this.duracionEstado = other.duracionEstado;
        this.statBoosts = new StatBoosts(other.statBoosts);
        this.precisionBoost = other.precisionBoost;
        this.evasionBoost = other.evasionBoost;

        this.movimientos = new ArrayList<>(Collections.nCopies(4, null));
        if (other.movimientos != null) {
            for (int i = 0; i < other.movimientos.size(); i++) {
                if (i < 4 && other.movimientos.get(i) != null) {
                    this.movimientos.set(i, new Movements(other.movimientos.get(i)));
                }
            }
        }

        this.estaRetrocediendo = other.estaRetrocediendo;
        this.modificadoresTemporales = new HashSet<>(other.modificadoresTemporales);
        this.critHitStage = other.critHitStage;
        this.fuenteDeDrenadoras = other.fuenteDeDrenadoras;

        this.isTransformed = other.isTransformed;
        this.originalNombre = other.originalNombre;
        if (other.originalTipos != null) this.originalTipos = new ArrayList<>(other.originalTipos);
        
        this.originalMovimientos = new ArrayList<>(4);
        if (other.originalMovimientos != null) {
            for (int i = 0; i < 4; i++) {
                Movements m = (i < other.originalMovimientos.size()) ? other.originalMovimientos.get(i) : null;
                this.originalMovimientos.add(m != null ? new Movements(m) : null);
            }
        } else { // Si el 'other' no tenía 'originalMovimientos' (caso improbable si se construye bien)
             for(int i = 0; i < 4; i++) {
                 Movements m = (i < this.movimientos.size()) ? this.movimientos.get(i) : null; // Usar los movimientos actuales como originales
                this.originalMovimientos.add(m != null ? new Movements(m) : null);
            }
        }

        this.isProtectedThisTurn = other.isProtectedThisTurn;
        this.consecutiveProtectUses = other.consecutiveProtectUses;
    }

    public void setBaseStatsToOriginal() {
        this.baseHP = this.initialBaseHP;
        this.baseAtaque = this.initialBaseAtaque;
        this.baseDefensa = this.initialBaseDefensa;
        this.baseAtaqueEspecial = this.initialBaseAtaqueEspecial;
        this.baseDefensaEspecial = this.initialBaseDefensaEspecial;
        this.baseVelocidad = this.initialBaseVelocidad;
    }

    public void recalculateStats() { 
        double healthPercentage = (this.hpMax > 0 && this.hpActual > 0) ? (double)this.hpActual / this.hpMax : 1.0;
        if (this.hpMax == 0 && this.hpActual == 0 && this.baseHP > 1) healthPercentage = 1.0;
        if (this.baseHP == 1) healthPercentage = 1.0;

        this.hpMax = StatFormulas.calcularHpMax(this.baseHP, this.nivel, this.ivHp, this.evHp);
        
        if (this.baseHP == 1) {
            this.hpActual = 1;
        } else {
            this.hpActual = (int) Math.round(this.hpMax * healthPercentage);
        }
        
        if (!estaDebilitado()) { 
            this.hpActual = Math.max(1, Math.min(this.hpMax, this.hpActual));
        } else {
            this.hpActual = 0; 
        }

        this.ataque = StatFormulas.calcularOtraStat(this.baseAtaque, this.nivel, this.ivAtaque, evAtaque, modNaturalezaAtaque);
        this.defensa = StatFormulas.calcularOtraStat(this.baseDefensa, this.nivel, ivDefensa, 0, modNaturalezaDefensa);
        this.ataqueEspecial = StatFormulas.calcularOtraStat(this.baseAtaqueEspecial, this.nivel, ivAtaqueEspecial, 0, modNaturalezaAtaqueEspecial);
        this.defensaEspecial = StatFormulas.calcularOtraStat(this.baseDefensaEspecial, this.nivel, ivDefensaEspecial, 0, modNaturalezaDefensaEspecial);
        this.velocidad = StatFormulas.calcularOtraStat(this.baseVelocidad, this.nivel, ivVelocidad, 0, modNaturalezaVelocidad);
    }

    // --- Getters ---
    public int getId() { return id; } 
    public String getNombre() { return nombre; }
    public List<Type.Tipo> getTipos() { return new ArrayList<>(tipos); } 
    public boolean tieneTipo(Type.Tipo tipo) { return tipos != null && tipos.contains(tipo); }
    public int getNivel() { return nivel; }
    public int getHpMax() { return hpMax; }
    public int getHpActual() { return hpActual; }
    public int getAtaque() { return ataque; }
    public int getDefensa() { return defensa; }
    public int getAtaqueEspecial() { return ataqueEspecial; }
    public int getDefensaEspecial() { return defensaEspecial; }
    public int getVelocidad() { return velocidad; }
    public String getEstado() { return estadoActivo; } 
    public Optional<String> getEstadoActivo() { return Optional.ofNullable(estadoActivo); } 
    public int getDuracionEstado() { return duracionEstado; }
    public StatBoosts getStatBoosts() { return statBoosts; } 
    public int getPrecisionBoost() { return precisionBoost; }
    public int getEvasionBoost() { return evasionBoost; }
    public List<Movements> getMovimientos() { 
        // Devolver una copia para evitar modificaciones externas directas a la lista interna
        List<Movements> currentMovesCopy = new ArrayList<>(4);
        for (Movements move : this.movimientos) {
            currentMovesCopy.add(move != null ? new Movements(move) : null);
        }
        return currentMovesCopy;
    }
    public boolean isRetrocediendo() { return estaRetrocediendo; }
    public Set<String> getModificadoresTemporales() { return new HashSet<>(modificadoresTemporales); }
    public int getCritHitStage() { return critHitStage; }
    public Pokemon getFuenteDeDrenadoras() { return fuenteDeDrenadoras; }

    // --- Setters y Modificadores de Estado Básico ---
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipos(List<Type.Tipo> tipos) { this.tipos = new ArrayList<>(tipos); }
    public void setNivel(int nivel) {
        this.nivel = Math.max(1, Math.min(100, nivel));
        double healthPercentage = (this.hpMax > 0) ? (double)this.hpActual / this.hpMax : 1.0;
        recalculateStats(); 
        this.hpActual = (int) Math.round(this.hpMax * healthPercentage);
        this.hpActual = Math.max(1, Math.min(this.hpMax, this.hpActual)); 
    }
    public void setHpActual(int hpActual) { this.hpActual = Math.max(0, Math.min(this.hpMax, hpActual)); }
    public void recibirDanio(int danio) { this.hpActual = Math.max(0, this.hpActual - danio); }
    public void curar(int cantidad) { this.hpActual = Math.min(this.hpMax, this.hpActual + cantidad); }
    public boolean estaDebilitado() { return this.hpActual <= 0; }

    public void revivir(int hpPercentageToRestore) { 
        if (!this.estaDebilitado()) return; 
        if (isTransformed) { 
            revertToOriginalState(); 
        }
        
        setBaseStatsToOriginal(); 
        recalculateStats(); 

        int amountToHeal = (int) (this.hpMax * (hpPercentageToRestore / 100.0));
        this.hpActual = Math.max(1, Math.min(amountToHeal, this.hpMax)); 

        this.clearStatusEffects(); 
        this.clearRetroceso();
        this.modificadoresTemporales.clear();
        this.critHitStage = 0; 
        this.fuenteDeDrenadoras = null;
    }

    public void clearStatusEffects() {
        String estadoAnterior = this.estadoActivo;
        this.estadoActivo = null;
        this.duracionEstado = 0;
        if ("DRENADORAS".equalsIgnoreCase(estadoAnterior)) { this.fuenteDeDrenadoras = null; }
        limpiarModificadorTemporal("QUEMADURA_ATAQUE_REDUCIDO");
    }
    public void setEstadoActivo(String estado, int duracion) {
        if (this.estadoActivo == null || this.estadoActivo.isEmpty() || 
           ("TOXICO".equalsIgnoreCase(estado) && "VENENO".equalsIgnoreCase(this.estadoActivo)) ||
           (this.estadoActivo.equalsIgnoreCase(estado) && "TOXICO".equalsIgnoreCase(estado))) { 
             this.estadoActivo = estado;
             this.duracionEstado = duracion;
        } else if ("TOXICO".equalsIgnoreCase(estado) && this.estadoActivo != null && !this.estadoActivo.isEmpty()){
            System.out.println(this.nombre + " ya tiene el estado " + this.estadoActivo + ", no se puede aplicar " + estado);
        } else if (this.estadoActivo != null && !this.estadoActivo.isEmpty()){
             System.out.println(this.nombre + " ya tiene el estado " + this.estadoActivo + ", no se puede aplicar " + estado);
        }
    }
    public void setEstadoActivo(String estado, int duracion, Pokemon fuente) {
        setEstadoActivo(estado, duracion); 
        if ("DRENADORAS".equalsIgnoreCase(estado)) { this.fuenteDeDrenadoras = fuente; }
    }
    public void setEstadoActivo(String estado) { 
        int defaultDuration;
        switch (estado.toUpperCase()) {
            case "DORMIDO": defaultDuration = INTERNAL_RNG.nextInt(3) + 2; break; 
            case "CONFUSION": defaultDuration = INTERNAL_RNG.nextInt(4) + 1; break; 
            case "TOXICO": defaultDuration = 1; break; 
            default: defaultDuration = Integer.MAX_VALUE; break;
        }
        setEstadoActivo(estado, defaultDuration);
    }
    public void actualizarDuracionEstado() {
        if (this.estadoActivo != null && this.duracionEstado > 0 && this.duracionEstado != Integer.MAX_VALUE) {
            if (!"TOXICO".equalsIgnoreCase(this.estadoActivo)) { this.duracionEstado--; }
            if (this.duracionEstado == 0 && !"TOXICO".equalsIgnoreCase(this.estadoActivo)) {
                String estadoAnterior = this.estadoActivo;
                this.estadoActivo = null; 
                if ("DRENADORAS".equalsIgnoreCase(estadoAnterior)) { this.fuenteDeDrenadoras = null; }
            }
        }
    }
    public void incrementarContadorToxic() {
        if ("TOXICO".equalsIgnoreCase(this.estadoActivo)) { this.duracionEstado = Math.min(15, this.duracionEstado + 1); }
    }
    
    public int getAtaqueModificado() {
        int stat = (int) (this.ataque * statBoosts.getMultiplier(StatBoosts.Stat.ATTACK));
        if ("QUEMADURA".equals(this.estadoActivo) && !tieneModificadorTemporal("AGALLAS_ACTIVA")) {
            stat /= 2;
        }
        return Math.max(1, stat);
    }

    public int getDefensaModificada() {
        return Math.max(1, (int) (this.defensa * statBoosts.getMultiplier(StatBoosts.Stat.DEFENSE)));
    }

    public int getAtaqueEspecialModificado() {
        return Math.max(1, (int) (this.ataqueEspecial * statBoosts.getMultiplier(StatBoosts.Stat.SP_ATTACK)));
    }

    public int getDefensaEspecialModificada() {
        return Math.max(1, (int) (this.defensaEspecial * statBoosts.getMultiplier(StatBoosts.Stat.SP_DEFENSE)));
    }

    public int getVelocidadModificada() {
        double velocidadBaseModificada = this.velocidad * statBoosts.getMultiplier(StatBoosts.Stat.SPEED);
        if ("PARALIZADO".equalsIgnoreCase(this.estadoActivo)) {
            velocidadBaseModificada /= 2.0; 
        }
        return Math.max(1, (int) velocidadBaseModificada);
    }
    
    public void modificarStatBoost(StatBoosts.Stat stat, int change) {
        if (change > 0) { statBoosts.increase(stat, change); } 
        else if (change < 0) { statBoosts.decrease(stat, -change); }
    }
    public int getStatBoostLevel(StatBoosts.Stat stat) { return statBoosts.getLevel(stat); }
    public void setBoosts(StatBoosts boosts) { this.statBoosts = (boosts != null) ? new StatBoosts(boosts) : new StatBoosts(); }
    public void resetearBoosts() {
        statBoosts.reset();
        this.precisionBoost = 0;
        this.evasionBoost = 0;
        this.modificadoresTemporales.clear(); 
        this.clearRetroceso();
        this.critHitStage = 0; 
    }
    public void modificarPrecisionBoost(int change) { this.precisionBoost = Math.max(-6, Math.min(6, this.precisionBoost + change)); }
    public void modificarEvasionBoost(int change) { this.evasionBoost = Math.max(-6, Math.min(6, this.evasionBoost + change)); }
    public void setRetrocediendo(boolean retrocediendo) { this.estaRetrocediendo = retrocediendo; }
    public void clearRetroceso() { this.estaRetrocediendo = false; }
    public void marcarModificadorTemporal(String clave) { this.modificadoresTemporales.add(clave); }
    public boolean tieneModificadorTemporal(String clave) { return this.modificadoresTemporales.contains(clave); }
    public void limpiarModificadorTemporal(String clave) { this.modificadoresTemporales.remove(clave); }
    public void modificarCritHitStage(int cambio) { this.critHitStage = Math.max(0, Math.min(3, this.critHitStage + cambio)); }
    public void resetCritHitStage() { this.critHitStage = 0; }
    public void setFuenteDeDrenadoras(Pokemon fuente) { this.fuenteDeDrenadoras = fuente; }

    public void setLearnedMove(int slotIndex, Movements newMoveInstance) {
        if (slotIndex < 0 || slotIndex >= 4) { return; }
        this.movimientos.set(slotIndex, newMoveInstance); 
    }
    public void setMovimientos(List<Movements> nuevosMovimientos) { 
        this.movimientos.clear();
        for (int i = 0; i < 4; i++) { 
            if (i < nuevosMovimientos.size() && nuevosMovimientos.get(i) != null) {
                this.movimientos.add(new Movements(nuevosMovimientos.get(i))); // Asegurar copia profunda
            } else {
                this.movimientos.add(null);
            }
        }
    }

    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean transformed) { 
        if (transformed && !this.isTransformed) { // Si se va a transformar y no lo estaba ya
            // Guardar movimientos actuales como originales ANTES de cambiarlos
            // Esto asegura que si se transforma múltiples veces, siempre se guarda el set de movimientos
            // que tenía justo antes de ESA transformación particular.
            // Sin embargo, la lógica estándar es que Transformación copia del objetivo,
            // y al revertir, vuelve a SUS movimientos base originales (los del constructor).
            // Por lo tanto, originalMovimientos se establece una vez en el constructor.
        }
        this.isTransformed = transformed; 
    }
    public String getOriginalNombre() { return originalNombre; }
    public List<Type.Tipo> getOriginalTipos() { return new ArrayList<>(originalTipos); }
    
    public List<Movements> getOriginalMovimientos() { 
        List<Movements> copy = new ArrayList<>(4);
        for(int i=0; i<4; i++){
            Movements m = (this.originalMovimientos != null && i < this.originalMovimientos.size()) ? this.originalMovimientos.get(i) : null;
            copy.add(m != null ? new Movements(m) : null);
        }
        return copy;
    }
   
    public void revertToOriginalState() { 
        if (!isTransformed) return;

        this.nombre = this.originalNombre; // El nombre visual vuelve al original
        this.tipos = new ArrayList<>(this.originalTipos != null ? this.originalTipos : Collections.emptyList());
        setBaseStatsToOriginal();

        double healthPercentage = (this.hpMax > 0 && this.hpActual > 0) ? (double)this.hpActual / this.hpMax : 1.0;
        if (this.hpMax == 0 && this.hpActual == 0 && this.initialBaseHP > 1) healthPercentage = 1.0;
        if (this.initialBaseHP == 1) healthPercentage = 1.0;

        recalculateStats();
        this.hpActual = (int) Math.round(this.hpMax * healthPercentage);
        this.hpActual = Math.max(1, Math.min(this.hpMax, this.hpActual));

        // Restaurar los movimientos originales guardados
        this.movimientos.clear();
        for(int i=0; i < 4; i++) {
            Movements m = (this.originalMovimientos != null && i < this.originalMovimientos.size()) ? this.originalMovimientos.get(i) : null;
            this.movimientos.add(m != null ? new Movements(m) : null); // Asegurar copia
        }
        this.isTransformed = false;
        // Los boosts de stats (statBoosts) se mantienen después de la transformación y su reversión.
    }


    public int getInitialBaseHP() { return initialBaseHP; }
    public int getInitialBaseAtaque() { return initialBaseAtaque; }
    public int getInitialBaseDefensa() { return initialBaseDefensa; }
    public int getInitialBaseAtaqueEspecial() { return initialBaseAtaqueEspecial; }
    public int getInitialBaseDefensaEspecial() { return initialBaseDefensaEspecial; }
    public int getInitialBaseVelocidad() { return initialBaseVelocidad; }

    public void setBaseHP(int baseHP) { this.baseHP = baseHP; }
    public void setBaseAtaque(int baseAtaque) { this.baseAtaque = baseAtaque; }
    public void setBaseDefensa(int baseDefensa) { this.baseDefensa = baseDefensa; }
    public void setBaseAtaqueEspecial(int baseAtaqueEspecial) { this.baseAtaqueEspecial = baseAtaqueEspecial; }
    public void setBaseDefensaEspecial(int baseDefensaEspecial) { this.baseDefensaEspecial = baseDefensaEspecial; }
    public void setBaseVelocidad(int baseVelocidad) { this.baseVelocidad = baseVelocidad; }
    
    public boolean isProtectedThisTurn() { return isProtectedThisTurn; }
    public void setProtectedThisTurn(boolean isProtected) { this.isProtectedThisTurn = isProtected; }
    public int getConsecutiveProtectUses() { return consecutiveProtectUses; }
    public void setConsecutiveProtectUses(int uses) { this.consecutiveProtectUses = Math.max(0, uses); }
    
    public void incrementConsecutiveProtectUses() { this.consecutiveProtectUses++; } 
    public void resetConsecutiveProtectUses() { this.consecutiveProtectUses = 0; }   
    
    public void resetProtectionStateForEndOfTurn() {
        this.isProtectedThisTurn = false;
    }
    public void didNotUseProtectOrDetectThisTurn() {
        this.resetConsecutiveProtectUses(); 
    }


    @Override
    public String toString() {
        StringBuilder movesString = new StringBuilder("[");
        if (movimientos != null) {
            for (int i = 0; i < movimientos.size(); i++) {
                movesString.append(movimientos.get(i) != null ? movimientos.get(i).getNombre() : "null");
                if (i < movimientos.size() - 1) movesString.append(", ");
            }
        }
        movesString.append("]");

        return "Pokemon{" +
               "nombre='" + nombre + '\'' +
               (isTransformed ? " (transformado de " + originalNombre + ")" : "") +
               ", nivel=" + nivel +
               ", hpActual=" + hpActual + "/" + hpMax +
               ", movimientos=" + movesString.toString() +
               ", estado=" + (estadoActivo == null ? "OK" : estadoActivo + " ("+duracionEstado+")") +
               '}';
    }
}