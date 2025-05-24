// Archivo: PokeBody/Data/SaveManager.java
package PokeBody.Data;

import PokeBody.domain.Item;
import PokeBody.domain.Pokemon;
import PokeBody.domain.StatBoosts;
import PokeBody.domain.Trainer;
import PokeBody.domain.Movements; 
import Face.SwingGUI; 

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SaveManager {

    public static class SaveData implements Serializable {
        private static final long serialVersionUID = 3L; 
        public TrainerSave aliado;
        public TrainerSave rival;
        public String gameModeName; 
        public String normalGameSubMode; 
        public String scenarioName; 
        public int turnNumber; 

        public SwingGUI.CombatantType player1CombatantType;
        public SwingGUI.CombatantType player2CombatantType;
    }

    public static class TrainerSave implements Serializable {
        private static final long serialVersionUID = 2L; 
        public String nombreEntrenador;
        public List<PokemonSave> equipo;
        public List<String> itemsNombres;

        public TrainerSave() {
            this.equipo = new ArrayList<>();
            this.itemsNombres = new ArrayList<>();
        }

        public TrainerSave(Trainer trainer) {
            this();
            if (trainer == null) {
                this.nombreEntrenador = "Entrenador Desconocido";
                return;
            }
            this.nombreEntrenador = trainer.getName();
            if (trainer.getteam() != null) {
                this.equipo = trainer.getteam().stream()
                                  .filter(p -> p != null)
                                  .map(PokemonSave::new) 
                                  .collect(Collectors.toList());
            }
            if (trainer.getItems() != null) {
                this.itemsNombres = trainer.getItems().stream()
                                       .filter(item -> item != null && item.getName() != null)
                                       .map(Item::getName)
                                       .collect(Collectors.toList());
            }
        }
    }

    public static class PokemonSave implements Serializable {
        private static final long serialVersionUID = 4L; // Incrementado por cambios en guardado de movimientos
        public String nombreEspecie;
        public int nivel; // Nivel actual del Pokémon
        public int hpActual;
        public String estadoNombre;
        public int estadoDuracion;
        public StatBoosts statBoosts;
        public int precisionBoost;
        public int evasionBoost;
        public List<MoveSave> movimientos; // Siempre guardará 4, con nulls para slots vacíos

        public PokemonSave() {
            this.movimientos = new ArrayList<>(4);
             for (int i = 0; i < 4; i++) {
                this.movimientos.add(null); // Inicializar con nulls
            }
        }

        public PokemonSave(Pokemon pokemon) {
            this(); // Llama al constructor por defecto para inicializar la lista de movimientos
            if (pokemon == null) {
                this.nombreEspecie = "Desconocido";
                this.nivel = 1;
                // La lista de movimientos ya está inicializada con 4 nulls
                return;
            }
            this.nombreEspecie = pokemon.getNombre();
            this.nivel = pokemon.getNivel(); // Guardar el nivel actual
            this.hpActual = pokemon.getHpActual();
            this.estadoNombre = pokemon.getEstado();
            this.estadoDuracion = pokemon.getDuracionEstado();
            this.statBoosts = new StatBoosts(pokemon.getStatBoosts()); 
            this.precisionBoost = pokemon.getPrecisionBoost();
            this.evasionBoost = pokemon.getEvasionBoost();

            List<Movements> currentPokemonMoves = pokemon.getMovimientos(); // Debe devolver una lista de 4
            for (int i = 0; i < 4; i++) {
                Movements move = (i < currentPokemonMoves.size()) ? currentPokemonMoves.get(i) : null;
                if (move != null) {
                    this.movimientos.set(i, new MoveSave(move.getNombre(), move.getPpActual()));
                } else {
                    this.movimientos.set(i, null); // Asegurar que el slot se guarde como null si está vacío
                }
            }
            System.out.println("[SaveManager PokemonSave] Guardando " + this.nombreEspecie + " Nivel: " + this.nivel + " Movimientos: " + this.movimientos.stream().map(m -> m != null ? m.moveName : "null").collect(Collectors.toList()));
        }
    }

    public static class MoveSave implements Serializable {
        private static final long serialVersionUID = 2L; 
        public String moveName;
        public int currentPp;

        public MoveSave() {}

        public MoveSave(String moveName, int currentPp) {
            this.moveName = moveName;
            this.currentPp = currentPp;
        }
    }

    public static void guardar(String nombreArchivo, Trainer aliado, Trainer rival,
                               String gameModeName, String normalGameSubMode, String scenarioName, int turnNumber,
                               SwingGUI.CombatantType p1Type, SwingGUI.CombatantType p2Type) throws IOException {
        SaveData data = new SaveData();
        data.aliado = new TrainerSave(aliado);
        data.rival = new TrainerSave(rival);
        data.gameModeName = gameModeName;
        data.normalGameSubMode = normalGameSubMode;
        data.scenarioName = scenarioName;
        data.turnNumber = turnNumber; 
        data.player1CombatantType = p1Type;
        data.player2CombatantType = p2Type;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(nombreArchivo))) {
            oos.writeObject(data);
            System.out.println("SaveManager: Estado del juego guardado en: " + nombreArchivo);
        } catch (IOException e) {
            System.err.println("SaveManager: Error al guardar el estado del juego en: " + nombreArchivo);
            throw e;
        }
    }

    public static SaveData cargar(String nombreArchivo) throws IOException, ClassNotFoundException, ClassCastException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(nombreArchivo))) {
            SaveData loadedData = (SaveData) ois.readObject();
            System.out.println("SaveManager: Estado del juego cargado desde: " + nombreArchivo);
            return loadedData;
        } catch (java.io.FileNotFoundException e) {
            System.out.println("SaveManager: Archivo de guardado no encontrado: " + nombreArchivo + ". Se tratará como ranura vacía.");
            return null;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("SaveManager: Error al cargar el estado del juego desde: " + nombreArchivo + " - " + e.getMessage());
            e.printStackTrace(); 
            throw e;
        }
    }
}