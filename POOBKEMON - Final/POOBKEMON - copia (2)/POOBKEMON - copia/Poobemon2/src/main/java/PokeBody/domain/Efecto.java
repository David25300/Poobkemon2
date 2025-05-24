// Archivo: PokeBody/domain/Efecto.java
package PokeBody.domain;

import java.util.Random;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent; // Necesario para InfligirEstado
import PokeBody.Services.events.PokemonHpChangedEvent;
import PokeBody.domain.StatBoosts.Stat;
import PokeBody.domain.efectos.AbsorberVidaEfecto;
import PokeBody.domain.efectos.DrenadorasPersistente;
import PokeBody.domain.efectos.InfligirEstado;
import PokeBody.domain.efectos.MetronomoEfecto;
import PokeBody.domain.efectos.ModificarStatEfecto;
import PokeBody.domain.efectos.ProteccionEfecto;
import PokeBody.domain.efectos.QuemaduraPersistente;
import PokeBody.domain.efectos.RetrocesoEfecto;
import PokeBody.domain.efectos.ToxicPersistente;
import PokeBody.domain.efectos.TransformacionEfecto;
import PokeBody.domain.efectos.VenenoPersistente;


public abstract class Efecto {
    protected static final Random RNG = new Random();

    protected final String nombreEfecto; 
    protected final double probabilidad;
    protected final int duracionBase;

    protected Efecto(String nombreEfecto, double probabilidad, int duracionBase) {
        this.nombreEfecto = nombreEfecto;
        this.probabilidad = Math.max(0.0, Math.min(1.0, probabilidad));
        this.duracionBase = Math.max(0, duracionBase);
    }

    public String getNombreEfecto() {
        return nombreEfecto;
    }

    public double getProbabilidad() {
        return probabilidad;
    }

    public int getDuracionBase() {
        return duracionBase;
    }

    public boolean intentarAplicar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        if (this instanceof ProteccionEfecto || this instanceof MetronomoEfecto || this instanceof TransformacionEfecto) {
            if (fuente == null || fuente.estaDebilitado()) return false;
        } else {
            if (objetivo == null) return false;
            if (objetivo.estaDebilitado() &&
                !(this instanceof InfligirEstado || this instanceof VenenoPersistente || this instanceof QuemaduraPersistente ||
                  this instanceof ToxicPersistente || this instanceof DrenadorasPersistente)) {
                return false;
            }
        }

        if (RNG.nextDouble() < this.probabilidad) {
            ejecutar(objetivo, fuente, dispatcher);
            return true;
        }
        return false;
    }

    public abstract void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher);

    protected void aplicarDanoPorcentaje(Pokemon objetivo, double fraccion, EventDispatcher dispatcher, String mensajeAdicional) {
        if (objetivo == null || objetivo.estaDebilitado()) return;
        
        int hpAntes = objetivo.getHpActual();
        int dano = Math.max(1, (int) (objetivo.getHpMax() * fraccion));
        objetivo.recibirDanio(dano);
        
        if (dispatcher != null) {
            dispatcher.dispatchEvent(new MessageEvent(objetivo.getNombre() + " " + mensajeAdicional + " (" + dano + " PS)."));
            dispatcher.dispatchEvent(new PokemonHpChangedEvent(objetivo, objetivo.getHpActual(), objetivo.getHpMax(), hpAntes));
        }
    }

    protected void curarPorcentaje(Pokemon objetivo, double fraccion, EventDispatcher dispatcher, String mensajeAdicional) {
        if (objetivo == null) return;
        if (objetivo.getHpActual() >= objetivo.getHpMax()) return;

        int hpAntes = objetivo.getHpActual();
        int cura = Math.max(1, (int) (objetivo.getHpMax() * fraccion));
        objetivo.curar(cura);

        if (dispatcher != null) {
            dispatcher.dispatchEvent(new MessageEvent(objetivo.getNombre() + " " + mensajeAdicional + " (" + (objetivo.getHpActual() - hpAntes) + " PS)."));
            dispatcher.dispatchEvent(new PokemonHpChangedEvent(objetivo, objetivo.getHpActual(), objetivo.getHpMax(), hpAntes));
        }
    }

    protected boolean modificarStat(Pokemon pokemon, Stat stat, int niveles, EventDispatcher dispatcher, boolean esUsuario) {
        if (pokemon == null || pokemon.estaDebilitado()) return false;

        String targetName = pokemon.getNombre();
        int oldLevel = pokemon.getStatBoostLevel(stat);
        pokemon.modificarStatBoost(stat, niveles);
        int newLevel = pokemon.getStatBoostLevel(stat);

        if (dispatcher != null && oldLevel != newLevel) {
            String adverbio = "";
            if (Math.abs(niveles) == 2) adverbio = " mucho";
            else if (Math.abs(niveles) >= 3) adverbio = " muchísimo";

            String verbo = niveles > 0 ? "subió" : "bajó";
            
            dispatcher.dispatchEvent(new MessageEvent("¡El/La " + stat.name().toLowerCase().replace("_", " ") + 
                               (esUsuario ? " de " + targetName : " del Pokémon oponente") + 
                               " " + verbo + adverbio + "!"));
        }
        return oldLevel != newLevel;
    }

    // --- Fábrica de Efectos de Estado Persistente ---
    public static Efecto forEstadoPersistente(String nombreEstado) {
        if (nombreEstado == null) return null;
        switch (nombreEstado.toUpperCase()) {
            case "VENENO": return new VenenoPersistente();
            case "QUEMADURA": return new QuemaduraPersistente();
            case "TOXICO": return new ToxicPersistente();
            case "DRENADORAS": return new DrenadorasPersistente();
            default: return null;
        }
    }

    // --- Fábrica de Efectos para Movimientos ---
    public static Efecto fromMovimientoData(String nombreEfectoJson, double probabilidad, String boostAttribute, int boostAmount) {
        if (nombreEfectoJson == null || nombreEfectoJson.trim().isEmpty()) {
            if (boostAttribute != null && !boostAttribute.isEmpty() && boostAmount != 0) {
                try {
                    Stat stat = Stat.valueOf(boostAttribute.toUpperCase());
                    boolean esAlUsuario = (boostAmount > 0); 
                    String nombreDescriptivo = (boostAmount > 0 ? "SUBIR_" : "BAJAR_") + stat.name() + (esAlUsuario ? "_PROPIO_MOV" : "_OBJETIVO_MOV");
                    return new ModificarStatEfecto(nombreDescriptivo, probabilidad, stat, boostAmount, esAlUsuario);
                } catch (IllegalArgumentException e) {
                    System.err.println("Advertencia: Atributo de boost inválido '" + boostAttribute + "' sin nombre de efecto principal.");
                    return null;
                }
            }
            return null; 
        }
        String efUpper = nombreEfectoJson.toUpperCase();

        if (efUpper.equals("TRANSFORMACION")) return new TransformacionEfecto();
        if (efUpper.equals("METRONOMO")) return new MetronomoEfecto();
        if (efUpper.equals("PROTECCION") || efUpper.equals("DETECCION")) return new ProteccionEfecto();

        if (efUpper.equals("VENENO")) return new InfligirEstado("INFLIGIR_VENENO", probabilidad, "VENENO", Integer.MAX_VALUE);
        if (efUpper.equals("TOXICO")) return new InfligirEstado("INFLIGIR_TOXICO", probabilidad, "TOXICO", 1); 
        if (efUpper.equals("QUEMADURA")) return new InfligirEstado("INFLIGIR_QUEMADURA", probabilidad, "QUEMADURA", Integer.MAX_VALUE);
        if (efUpper.equals("PARALIZADO")) return new InfligirEstado("INFLIGIR_PARALISIS", probabilidad, "PARALIZADO", Integer.MAX_VALUE);
        if (efUpper.equals("DORMIDO")) return new InfligirEstado("INFLIGIR_SUENO", probabilidad, "DORMIDO", RNG.nextInt(3) + 2); 
        if (efUpper.equals("CONGELADO")) return new InfligirEstado("INFLIGIR_CONGELACION", probabilidad, "CONGELADO", Integer.MAX_VALUE);
        if (efUpper.equals("CONFUSION")) return new InfligirEstado("INFLIGIR_CONFUSION", probabilidad, "CONFUSION", RNG.nextInt(4) + 2); 
        
        if (efUpper.equals("FLINCH") || efUpper.equals("RETROCESO")) return new RetrocesoEfecto(probabilidad);
        if (efUpper.equals("DRENADORAS")) return new InfligirEstado("INFLIGIR_DRENADORAS", probabilidad, "DRENADORAS", Integer.MAX_VALUE); 

        if (boostAttribute != null && !boostAttribute.isEmpty() && boostAmount != 0) {
            try {
                Stat stat = Stat.valueOf(boostAttribute.toUpperCase());
                boolean esAlUsuarioDeterminado = (boostAmount > 0 && (efUpper.contains("PROPIO") || efUpper.contains("SELF") || efUpper.contains("USER") || efUpper.endsWith("_UP") || efUpper.endsWith("_BOOST"))) ||
                                           (boostAmount < 0 && (efUpper.contains("OBJETIVO") || efUpper.contains("TARGET") || efUpper.endsWith("_DOWN") || efUpper.endsWith("_DROP")));
                if (!(efUpper.contains("PROPIO") || efUpper.contains("SELF") || efUpper.contains("USER") || efUpper.contains("OBJETIVO") || efUpper.contains("TARGET"))) {
                     esAlUsuarioDeterminado = (boostAmount > 0);
                }

                String nombreDescriptivo = (boostAmount > 0 ? "SUBIR_" : "BAJAR_") + stat.name() + (esAlUsuarioDeterminado ? "_PROPIO" : "_OBJETIVO") + "_EFECTO_MOV";
                return new ModificarStatEfecto(nombreDescriptivo, probabilidad, stat, boostAmount, esAlUsuarioDeterminado);
            } catch (IllegalArgumentException e) {
                System.err.println("Advertencia: Atributo de boost inválido '" + boostAttribute + "' para efecto '" + nombreEfectoJson + "'.");
            }
        }
        
        if (efUpper.startsWith("ABSORBER_VIDA_")) {  
            try {
                String porcentajeStr = efUpper.substring("ABSORBER_VIDA_".length());
                double porcentaje = Double.parseDouble(porcentajeStr) / 100.0;
                return new AbsorberVidaEfecto(efUpper, porcentaje);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                 System.err.println("Advertencia: Porcentaje de absorción inválido para efecto '" + nombreEfectoJson + "'. Formato esperado: ABSORBER_VIDA_XX");
            }
        }

        System.err.println("Advertencia: Efecto de movimiento no reconocido o no implementado: '" + nombreEfectoJson + "'.");
        return null;
    } 
}