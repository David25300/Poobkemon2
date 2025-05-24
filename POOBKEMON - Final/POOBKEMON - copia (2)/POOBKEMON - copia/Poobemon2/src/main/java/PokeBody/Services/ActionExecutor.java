package PokeBody.Services;

import java.util.List;
import java.util.Map; 
import java.util.Random;

import PokeBody.Services.TurnOrderResolver.TurnAction;
import PokeBody.Services.events.EventDispatcher;
import PokeBody.Services.events.MessageEvent; 
import PokeBody.Services.events.MoveUsedEvent;
import PokeBody.Services.events.PokemonChangeEvent;
import PokeBody.Services.events.PokemonFaintedEvent; 
import PokeBody.Services.events.PokemonHpChangedEvent;
import PokeBody.domain.Efecto;
import PokeBody.domain.HealerPokemon; 
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Trainer;
import PokeBody.domain.Type;
import PokeBody.domain.efectos.AbsorberVidaEfecto; 
import PokeBody.domain.efectos.MetronomoEfecto;
import PokeBody.domain.efectos.ProteccionEfecto;
import PokeBody.domain.efectos.TransformacionEfecto;


public class ActionExecutor {
    private final BattlefieldState battlefieldState;
    private final DamageCalculator damageCalculator;
    private final EffectHandler effectHandler; 
    private final EventDispatcher eventDispatcher;
    private final Map<String, Movements> allGameMoves; 
    private static final Random RNG = new Random(); 

    public ActionExecutor(BattlefieldState battlefieldState, DamageCalculator dc, EffectHandler eh, 
                          EventDispatcher dispatcher, Map<String, Movements> allGameMoves) {
        if (battlefieldState == null || dc == null || eh == null || dispatcher == null || allGameMoves == null) {
            throw new IllegalArgumentException("ActionExecutor dependencies cannot be null.");
        }
        this.battlefieldState = battlefieldState;
        this.damageCalculator = dc;
        this.effectHandler = eh;
        this.eventDispatcher = dispatcher;
        this.allGameMoves = allGameMoves;
    }

    public boolean canPokemonAct(Pokemon pokemon, EventDispatcher currentEventDispatcher) {
        if (pokemon == null || pokemon.estaDebilitado()) {
            return false;
        }

        if (pokemon.isRetrocediendo()) {
            currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " retrocedió y no pudo moverse!"));
            pokemon.clearRetroceso(); 
            return false; 
        }

        String estado = pokemon.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            return true;
        }

        switch (estado.toUpperCase()) {
            case "DORMIDO":
                if (pokemon.getDuracionEstado() <= 0) {
                    currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " se despertó!"));
                    pokemon.clearStatusEffects(); 
                    return true;
                } else {
                    currentEventDispatcher.dispatchEvent(new MessageEvent(pokemon.getNombre() + " está profundamente dormido."));
                    return false;
                }
            case "CONGELADO":
                if (RNG.nextDouble() < 0.2) { 
                    currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " se descongeló!"));
                    pokemon.clearStatusEffects(); 
                    return true;
                } else {
                    currentEventDispatcher.dispatchEvent(new MessageEvent(pokemon.getNombre() + " está congelado y no se puede mover."));
                    return false;
                }
            case "PARALIZADO":
                if (RNG.nextDouble() < 0.25) { 
                    currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " está totalmente paralizado! No se puede mover."));
                    return false;
                }
                break; 
            case "CONFUSION": 
                currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " está confuso!"));
                if (pokemon.getDuracionEstado() <= 0) {
                    currentEventDispatcher.dispatchEvent(new MessageEvent(pokemon.getNombre() + " ya no está confuso."));
                    pokemon.clearStatusEffects(); 
                } else if (RNG.nextDouble() < 0.33) { 
                    currentEventDispatcher.dispatchEvent(new MessageEvent("¡Se hirió a sí mismo en su confusión!"));
                    int prevHp = pokemon.getHpActual();
                    int selfDamage = damageCalculator.calcularDanioConfusion(pokemon);
                    pokemon.recibirDanio(selfDamage);
                    currentEventDispatcher.dispatchEvent(new PokemonHpChangedEvent(pokemon, pokemon.getHpActual(), pokemon.getHpMax(), prevHp));
                    currentEventDispatcher.dispatchEvent(new MessageEvent(pokemon.getNombre() + " recibió " + selfDamage + " PS de daño por la confusión."));
                    
                    if (pokemon.estaDebilitado()) {
                        currentEventDispatcher.dispatchEvent(new MessageEvent("¡" + pokemon.getNombre() + " se debilitó por la confusión!"));
                    }
                    return false; 
                }
                break;
        }
        return true;
    }

    public void ejecutarAccionesDeAtaque(List<TurnAction> accionesOrdenadas, BattlefieldState currentState) {
        if (accionesOrdenadas == null || currentState == null) {
            System.err.println("ActionExecutor: accionesOrdenadas o currentState es null.");
            return;
        }

        for (TurnAction accion : accionesOrdenadas) {
            Pokemon atacanteOriginalQueEligio = accion.getAtacante(); 
            Movements movimientoOriginalSeleccionado = accion.getMovimiento(); 
            
            Pokemon atacanteActualEnCampo = currentState.getActivePokemonForTrainer(currentState.getOwnerOf(atacanteOriginalQueEligio));

            if (atacanteActualEnCampo == null || atacanteActualEnCampo.estaDebilitado()) {
                continue; 
            }
            
            if (!canPokemonAct(atacanteActualEnCampo, this.eventDispatcher)) {
                if (atacanteActualEnCampo.estaDebilitado()) { 
                    this.eventDispatcher.dispatchEvent(new PokemonFaintedEvent(atacanteActualEnCampo, currentState.getOwnerOf(atacanteActualEnCampo)));
                }
                continue; 
            }
            
            Movements movimientoAEjecutar = movimientoOriginalSeleccionado;
            Efecto efectoDelMovimientoOriginal = (movimientoOriginalSeleccionado != null) ? movimientoOriginalSeleccionado.getEfecto() : null;

            if (efectoDelMovimientoOriginal instanceof MetronomoEfecto) {
                eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usó Metrónomo!"));
                MetronomoEfecto metronomo = (MetronomoEfecto) efectoDelMovimientoOriginal;
                metronomo.seleccionarMovimientoAleatorio(atacanteActualEnCampo, eventDispatcher, allGameMoves);
                Movements chosenMove = metronomo.getChosenMoveByMetronome();
                
                if (chosenMove != null) {
                    movimientoAEjecutar = chosenMove; 
                    eventDispatcher.dispatchEvent(new MessageEvent("¡Metrónomo eligió " + movimientoAEjecutar.getNombre() + "!"));
                } else {
                    eventDispatcher.dispatchEvent(new MessageEvent("¡Metrónomo falló al elegir un movimiento! Usará Forcejeo."));
                    movimientoAEjecutar = Movements.FUERZAGEO; 
                }
                metronomo.clearChosenMove(); 
            } else if (movimientoAEjecutar == null || movimientoAEjecutar.getPpActual() <= 0 && !movimientoAEjecutar.getNombre().equalsIgnoreCase("Forcejeo")) {
                if (movimientoAEjecutar != null && !movimientoAEjecutar.getNombre().equalsIgnoreCase("Forcejeo")) { // Solo si no es ya Forcejeo
                    eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " no tiene más PP para " + movimientoAEjecutar.getNombre() + ". ¡Usará Forcejeo!"));
                } else if (movimientoAEjecutar == null) {
                     eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " no tiene un movimiento seleccionado. ¡Usará Forcejeo!"));
                }
                movimientoAEjecutar = Movements.FUERZAGEO;
            }
            
            if (movimientoOriginalSeleccionado != null && 
                !(efectoDelMovimientoOriginal instanceof MetronomoEfecto) && 
                !movimientoOriginalSeleccionado.getNombre().equalsIgnoreCase("Forcejeo") &&
                movimientoOriginalSeleccionado.getPpActual() > 0 ) { 
                movimientoOriginalSeleccionado.usar(); 
            }


            if (movimientoAEjecutar.getEfecto() instanceof TransformacionEfecto) {
                eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + "!"));
                Pokemon objetivoTransformacion = currentState.getOpponentOf(atacanteActualEnCampo);
                ((TransformacionEfecto) movimientoAEjecutar.getEfecto()).ejecutarTransformacion(atacanteActualEnCampo, objetivoTransformacion, allGameMoves, eventDispatcher);
                continue; 
            }

            if (movimientoAEjecutar.getEfecto() instanceof ProteccionEfecto) {
                eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + "!"));
                ((ProteccionEfecto) movimientoAEjecutar.getEfecto()).ejecutar(null, atacanteActualEnCampo, eventDispatcher); // Asumiendo que el primer param es el objetivo, que no aplica para Protección en sí mismo
                continue; 
            }
            
            Pokemon objetivoPrincipal = currentState.getOpponentOf(atacanteActualEnCampo);
            Trainer defendingTrainer = (objetivoPrincipal != null) ? currentState.getOwnerOf(objetivoPrincipal) : null;
            
            boolean isSelfDestructMove = movimientoAEjecutar.getNombre().equalsIgnoreCase("Autodestrucción") || movimientoAEjecutar.getNombre().equalsIgnoreCase("Explosión");

            if (objetivoPrincipal == null && !isSelfDestructMove && movimientoAEjecutar.getCategoria() != Movements.TipoAtaque.ESTADO) {
                this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + " pero no hay objetivo válido."));
                continue;
            }
            
            if (objetivoPrincipal != null && objetivoPrincipal.isProtectedThisTurn() && !canMoveBypassProtection(movimientoAEjecutar)) {
                this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + "!"));
                this.eventDispatcher.dispatchEvent(new MessageEvent("¡" + objetivoPrincipal.getNombre() + " se protegió del ataque de " + atacanteActualEnCampo.getNombre() + "!"));
                if (!(movimientoAEjecutar.getEfecto() instanceof ProteccionEfecto)) { 
                    atacanteActualEnCampo.didNotUseProtectOrDetectThisTurn();
                }
                continue; 
            }
            if (!(movimientoAEjecutar.getEfecto() instanceof ProteccionEfecto)) {
                atacanteActualEnCampo.didNotUseProtectOrDetectThisTurn();
            }

            if (objetivoPrincipal != null && objetivoPrincipal.estaDebilitado() && !isSelfDestructMove && movimientoAEjecutar.getCategoria() != Movements.TipoAtaque.ESTADO) {
                this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + "!"));
                this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " intentó atacar a " + objetivoPrincipal.getNombre() + ", ¡pero ya está debilitado!"));
                continue;
            }

            this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " usa " + movimientoAEjecutar.getNombre() + "!"));

            boolean moveHits = true;
            if (objetivoPrincipal != null && movimientoAEjecutar.getPrecision() > 0 && movimientoAEjecutar.getPrecision() <= 100) { 
                if (!damageCalculator.movimientoImpacta(movimientoAEjecutar, atacanteActualEnCampo, objetivoPrincipal)) {
                    this.eventDispatcher.dispatchEvent(new MoveUsedEvent(atacanteActualEnCampo, objetivoPrincipal, movimientoAEjecutar, 0, false, "¡Pero falló!"));
                    moveHits = false;
                }
            }
            if (!moveHits) continue;

            if (atacanteActualEnCampo instanceof HealerPokemon && movimientoAEjecutar.getPotencia() > 0 && objetivoPrincipal != null && !objetivoPrincipal.estaDebilitado()) {
                boolean typesMatch = false;
                for (Type.Tipo attackerType : atacanteActualEnCampo.getTipos()) {
                    if (objetivoPrincipal.tieneTipo(attackerType)) {
                        typesMatch = true;
                        break;
                    }
                }

                if (typesMatch) {
                    int healAmount = movimientoAEjecutar.getPotencia(); 
                    healAmount = Math.max(1, healAmount);

                    int prevHpObjetivo = objetivoPrincipal.getHpActual();
                    objetivoPrincipal.curar(healAmount); 
                    int healedAmount = objetivoPrincipal.getHpActual() - prevHpObjetivo;
                    
                    if (healedAmount > 0) {
                        this.eventDispatcher.dispatchEvent(new MessageEvent(
                            atacanteActualEnCampo.getNombre() + " usó " + movimientoAEjecutar.getNombre() + " sobre " + objetivoPrincipal.getNombre() +
                            " de su mismo tipo. ¡" + objetivoPrincipal.getNombre() + " fue curado por " + healedAmount + " PS!"
                        ));
                        this.eventDispatcher.dispatchEvent(new PokemonHpChangedEvent(objetivoPrincipal, objetivoPrincipal.getHpActual(), objetivoPrincipal.getHpMax(), prevHpObjetivo));
                    } else {
                         this.eventDispatcher.dispatchEvent(new MessageEvent(
                            atacanteActualEnCampo.getNombre() + " usó " + movimientoAEjecutar.getNombre() + " sobre " + objetivoPrincipal.getNombre() +
                            " de su mismo tipo, ¡pero su vida ya estaba al máximo!"
                        ));
                    }
                    continue; 
                }
            }
            
            double efectividad = 1.0;
            String effectivenessMessage = "";
            if (objetivoPrincipal != null && movimientoAEjecutar.getCategoria() != Movements.TipoAtaque.ESTADO) {
                efectividad = damageCalculator.obtenerMultiplicadorEfectividad(atacanteActualEnCampo, objetivoPrincipal, movimientoAEjecutar);
                if (efectividad == 0) {
                    this.eventDispatcher.dispatchEvent(new MoveUsedEvent(atacanteActualEnCampo, objetivoPrincipal, movimientoAEjecutar, 0, false, "¡No afecta a " + objetivoPrincipal.getNombre() + "!"));
                    continue; 
                }
                if (efectividad > 1.5) effectivenessMessage = "¡Es súper efectivo!"; 
                else if (efectividad < 1.0 && efectividad > 0) effectivenessMessage = "No es muy efectivo...";
            }

            int dano = 0;
            boolean esCritico = false;

            if (movimientoAEjecutar.getCategoria() != Movements.TipoAtaque.ESTADO) {
                if (objetivoPrincipal != null) { 
                    esCritico = damageCalculator.esGolpeCritico(atacanteActualEnCampo); // esGolpeCritico en tu DamageCalculator solo toma atacante
                    if (esCritico) {
                        this.eventDispatcher.dispatchEvent(new MessageEvent("¡Golpe crítico!"));
                    }
                    int prevHpObjetivo = objetivoPrincipal.getHpActual();
                    dano = damageCalculator.calcularDanioReal(atacanteActualEnCampo, objetivoPrincipal, movimientoAEjecutar, esCritico);
                    objetivoPrincipal.recibirDanio(dano);
                    this.eventDispatcher.dispatchEvent(new PokemonHpChangedEvent(objetivoPrincipal, objetivoPrincipal.getHpActual(), objetivoPrincipal.getHpMax(), prevHpObjetivo));
                }
            }
            
            this.eventDispatcher.dispatchEvent(new MoveUsedEvent(atacanteActualEnCampo, objetivoPrincipal, movimientoAEjecutar, dano, esCritico, effectivenessMessage));

            Efecto efectoSecundarioDelMovimientoEjecutado = movimientoAEjecutar.getEfecto();
            if (efectoSecundarioDelMovimientoEjecutado != null && 
                !(efectoSecundarioDelMovimientoEjecutado instanceof MetronomoEfecto) && 
                !(efectoSecundarioDelMovimientoEjecutado instanceof TransformacionEfecto) && 
                !(efectoSecundarioDelMovimientoEjecutado instanceof ProteccionEfecto)) {
                
                if (efectoSecundarioDelMovimientoEjecutado instanceof AbsorberVidaEfecto) {
                    if (dano > 0 && !atacanteActualEnCampo.estaDebilitado()) {
                        // Asumiendo que AbsorberVidaEfecto tiene un método para obtener el porcentaje
                        // Si no, necesitarás una forma de acceder a ese valor desde el efecto.
                        // Por ejemplo, ((AbsorberVidaEfecto) efectoSecundarioDelMovimientoEjecutado).getPorcentajeDanoAbsorbido()
                        // Aquí usaré un valor fijo como placeholder si el método no existe.
                        double porcentajeAbsorcion = 0.5; // Placeholder, ajusta según tu clase AbsorberVidaEfecto
                        if (efectoSecundarioDelMovimientoEjecutado instanceof AbsorberVidaEfecto) {
                            // Intenta obtener el porcentaje real si el método existe
                            // porcentajeAbsorcion = ((AbsorberVidaEfecto) efectoSecundarioDelMovimientoEjecutado).getPorcentajeDanoAbsorbido();
                        }
                        int cura = Math.max(1, (int) (dano * porcentajeAbsorcion));
                        int prevHpAtacante = atacanteActualEnCampo.getHpActual();
                        atacanteActualEnCampo.curar(cura);
                        this.eventDispatcher.dispatchEvent(new PokemonHpChangedEvent(atacanteActualEnCampo, atacanteActualEnCampo.getHpActual(), atacanteActualEnCampo.getHpMax(), prevHpAtacante));
                        this.eventDispatcher.dispatchEvent(new MessageEvent(atacanteActualEnCampo.getNombre() + " recuperó " + cura + " PS."));
                    }
                } else {
                    // Adaptar la llamada a intentarAplicar si su firma es diferente
                    // La firma en tu Efecto.java podría ser (Pokemon objetivo, Pokemon lanzador, EventDispatcher dispatcher)
                    // O (Pokemon objetivo, Pokemon lanzador, EventDispatcher dispatcher, Movements movimientoUtilizado, int danoCausado)
                    // Usaré una firma común, ajústala si es necesario.
                    boolean puedeAplicar = true;
                    if (objetivoPrincipal != null && objetivoPrincipal.estaDebilitado()) {
                        // Comprobar si el efecto puede aplicar sobre debilitados (ej. Respiro)
                        // if (!efectoSecundarioDelMovimientoEjecutado.puedeAplicarSobreDebilitado()) {
                        // puedeAplicar = false;
                        // }
                    }
                    if (puedeAplicar) {
                         efectoSecundarioDelMovimientoEjecutado.intentarAplicar(objetivoPrincipal, atacanteActualEnCampo, this.eventDispatcher);
                    }
                }
            }

            if (objetivoPrincipal != null && objetivoPrincipal.estaDebilitado()) {
                this.eventDispatcher.dispatchEvent(new PokemonFaintedEvent(objetivoPrincipal, defendingTrainer));
            }
            
            if (isSelfDestructMove) {
                int prevHpAtacanteRecoil = atacanteActualEnCampo.getHpActual();
                atacanteActualEnCampo.recibirDanio(atacanteActualEnCampo.getHpMax()); 
                this.eventDispatcher.dispatchEvent(new PokemonHpChangedEvent(atacanteActualEnCampo, atacanteActualEnCampo.getHpActual(), atacanteActualEnCampo.getHpMax(), prevHpAtacanteRecoil));
                this.eventDispatcher.dispatchEvent(new PokemonFaintedEvent(atacanteActualEnCampo, currentState.getOwnerOf(atacanteActualEnCampo)));
            }
            
            if (atacanteActualEnCampo.estaDebilitado() && !isSelfDestructMove) { 
                this.eventDispatcher.dispatchEvent(new PokemonFaintedEvent(atacanteActualEnCampo, currentState.getOwnerOf(atacanteActualEnCampo)));
            }
        } 
    }
    
    private boolean canMoveBypassProtection(Movements move) {
        if (move == null) return false;
        
        if (move.getCategoria() == Movements.TipoAtaque.ESTADO) {
            return true; 
        }

        List<String> bypassingMoves = List.of(
            "Amago", "Golpe Umbrío", "Hiperespacio Furioso", "Vastaguardia", 
            "Anticipo", "Golpe Fantasma", "Presa Maxilar" 
        ); 
        return bypassingMoves.contains(move.getNombre());
    }

    public void executeChangePokemon(Trainer trainer, Pokemon newPokemon) {
        Pokemon oldPokemon = battlefieldState.getActivePokemonForTrainer(trainer);

        if (newPokemon == null) {
            eventDispatcher.dispatchEvent(new MessageEvent("Error: No se seleccionó un Pokémon para cambiar."));
            return;
        }
        if (oldPokemon == newPokemon && oldPokemon != null && !oldPokemon.estaDebilitado()) { 
            eventDispatcher.dispatchEvent(new MessageEvent(newPokemon.getNombre() + " ya está en combate."));
            return; 
        }
        if (newPokemon.estaDebilitado()) {
            eventDispatcher.dispatchEvent(new MessageEvent(newPokemon.getNombre() + " está debilitado y no puede luchar."));
            return; 
        }
        
        if (oldPokemon != null) {
            eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " retira a " + oldPokemon.getNombre() + "."));
            if (oldPokemon.isTransformed()){
                oldPokemon.revertToOriginalState(); 
            }
            oldPokemon.resetearBoosts(); 
            oldPokemon.resetProtectionStateForEndOfTurn(); 
            oldPokemon.resetConsecutiveProtectUses();   
        }
        
        battlefieldState.setActivePokemonForTrainer(trainer, newPokemon); 
        eventDispatcher.dispatchEvent(new PokemonChangeEvent(trainer, oldPokemon, newPokemon));
        eventDispatcher.dispatchEvent(new MessageEvent("¡Adelante, " + newPokemon.getNombre() + "!"));
        newPokemon.resetConsecutiveProtectUses(); 
        newPokemon.setProtectedThisTurn(false);   
    }

    public void executeUseItem(Trainer trainer, String itemID, Pokemon targetPokemon) {
        eventDispatcher.dispatchEvent(new MessageEvent("La funcionalidad de usar ítems (" + itemID + ") aún no está implementada completamente."));
    }

    public void executeRun(Trainer trainer) {
        eventDispatcher.dispatchEvent(new MessageEvent(trainer.getName() + " intentó huir..."));
        eventDispatcher.dispatchEvent(new MessageEvent("¡No se puede huir de un combate de entrenador!")); 
    }
}