package PokeBody.domain.efectos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent;
import PokeBody.domain.Efecto;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;

public class TransformacionEfecto extends Efecto {
    
    public TransformacionEfecto() {
        super("TRANSFORMACION", 1.0, 0); 
    }

    public boolean ejecutarTransformacion(Pokemon fuente, Pokemon objetivo, Map<String, Movements> allGameMoves, EventDispatcher dispatcher) {
        if (fuente == null || fuente.estaDebilitado()) {
            if (dispatcher != null && fuente != null) dispatcher.dispatchEvent(new MessageEvent(fuente.getNombre() + " está debilitado y no puede transformarse."));
            return false;
        }
        if (objetivo == null || objetivo.estaDebilitado()) {
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("¡Pero falló! No hay un objetivo válido o está debilitado."));
            return false;
        }
        if (fuente.isTransformed()) {
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent(fuente.getNombre() + " ya está transformado."));
            return false;
        }
         if (objetivo == fuente || (objetivo.getId() == fuente.getId())) { 
             if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent(fuente.getNombre() + " no puede transformarse en sí mismo."));
            return false;
        }
        if (allGameMoves == null || allGameMoves.isEmpty()){
            if (dispatcher != null) dispatcher.dispatchEvent(new MessageEvent("Error interno: No se pueden cargar los movimientos para la transformación."));
            System.err.println("TransformacionEfecto: allGameMoves es nulo o vacío.");
            return false;
        }

        // Los movimientos originales ya se guardaron en `fuente.originalMovimientos` en el constructor de Pokemon.
        // No es necesario volver a guardarlos aquí a menos que quieras permitir transformar múltiples veces
        // sin revertir, lo cual complicaría la lógica de "original".

        fuente.setTransformed(true);
        // El nombre visual en la UI podría cambiar, pero el 'originalNombre' se mantiene para la lógica interna.
        // fuente.setNombre(objetivo.getNombre()); // No cambiar el nombre real del objeto Pokemon
        
        fuente.setTipos(new ArrayList<>(objetivo.getTipos()));

        fuente.setBaseHP(objetivo.getInitialBaseHP()); 
        fuente.setBaseAtaque(objetivo.getInitialBaseAtaque());
        fuente.setBaseDefensa(objetivo.getInitialBaseDefensa());
        fuente.setBaseAtaqueEspecial(objetivo.getInitialBaseAtaqueEspecial());
        fuente.setBaseDefensaEspecial(objetivo.getInitialBaseDefensaEspecial());
        fuente.setBaseVelocidad(objetivo.getInitialBaseVelocidad());
        
        double healthPercentage = (fuente.getHpMax() > 0 && fuente.getHpActual() > 0) ? (double)fuente.getHpActual() / fuente.getHpMax() : 1.0;
        if (fuente.getHpMax() == 0 && fuente.getHpActual() == 0 && fuente.getInitialBaseHP() > 1) healthPercentage = 1.0; 
        if (fuente.getInitialBaseHP() == 1 && fuente.getHpActual() == 1) healthPercentage = 1.0;

        fuente.recalculateStats(); 
        
        int newHpActual = (int) Math.round(fuente.getHpMax() * healthPercentage);
        fuente.setHpActual(Math.max(1, Math.min(fuente.getHpMax(), newHpActual)));

        // --- Lógica de Copia de Movimientos ---
        List<Movements> nuevosMovimientos = new ArrayList<>(Collections.nCopies(4, null));
        List<Movements> targetMoves = objetivo.getMovimientos(); // Obtiene una copia de los movimientos del objetivo

        for (int i = 0; i < 4; i++) {
            if (i < targetMoves.size() && targetMoves.get(i) != null) {
                Movements moveDelObjetivo = targetMoves.get(i);
                // Es importante obtener la plantilla del movimiento desde allGameMoves
                // para no copiar el estado actual del PP del movimiento del objetivo, sino crear uno nuevo.
                Movements plantillaMovimiento = allGameMoves.get(moveDelObjetivo.getNombre()); 
                
                if (plantillaMovimiento != null) {
                    Movements movimientoCopiado = new Movements(plantillaMovimiento); // Crear nueva instancia desde la plantilla
                    movimientoCopiado.setPpActual(Math.min(5, plantillaMovimiento.getPpMax())); // Establecer PP a 5 o max si es menor
                    nuevosMovimientos.set(i, movimientoCopiado);
                } else {
                     System.err.println("Error en TransformacionEfecto: Plantilla de movimiento '" + moveDelObjetivo.getNombre() + "' no encontrada en allGameMoves.");
                     nuevosMovimientos.set(i, null); // Dejar el slot vacío si no se encuentra la plantilla
                }
            } else {
                nuevosMovimientos.set(i, null); // Si el objetivo no tiene movimiento en ese slot
            }
        }
        fuente.setMovimientos(nuevosMovimientos); // Actualizar los movimientos del Pokémon que se transforma

        if (dispatcher != null) {
            dispatcher.dispatchEvent(new MessageEvent("¡" + fuente.getOriginalNombre() + " se transformó en " + objetivo.getNombre() + "!"));
        }
        return true;
    }

    @Override
    public void ejecutar(Pokemon objetivo, Pokemon fuente, EventDispatcher dispatcher) {
        // Este método es llamado por ActionExecutor si el efecto se aplica como un efecto secundario normal.
        // Para Transformación, ActionExecutor debe llamar a ejecutarTransformacion con el mapa de movimientos.
        System.out.println("TransformacionEfecto.ejecutar() llamado, pero la lógica principal está en ejecutarTransformacion(). ActionExecutor debería manejar esto.");
    }
}
