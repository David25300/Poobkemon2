package PokeBody.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import PokeBody.Data.DataLoader;
import PokeBody.Data.MovementsData;
import PokeBody.Data.PokemonData;
import PokeBody.domain.HealerPokemon;
import PokeBody.domain.Item;
import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon; // Asegúrate de importar HealerPokemon
import PokeBody.domain.items.AtaqueEspX;
import PokeBody.domain.items.AtaqueX;
import PokeBody.domain.items.BombaAliada;
import PokeBody.domain.items.BombaRival;
import PokeBody.domain.items.DefensaEspX;
import PokeBody.domain.items.DefensaX;
import PokeBody.domain.items.HyperPotion;
import PokeBody.domain.items.Potion;
import PokeBody.domain.items.Revive;
import PokeBody.domain.items.SuperPotion;
import PokeBody.domain.items.VelocidadX;

/**
 * Class responsible for the initial setup logic for different game modes.
 * It is independent of the graphical interface. Loads data from resources.
 * Ítems se crean manualmente.
 */
public class GameSetupManager {

    public static final String POKEMONS_RESOURCE_PATH = "/pokemons.json";
    public static final String MOVEMENTS_RESOURCE_PATH = "/movements.json";
    
    private static final int DEFAULT_POKEMON_LEVEL = 50; // Nivel por defecto para los Pokémon cargados

    // Constructor (si tuvieras dependencias como DataLoader aquí, las pasarías)
    // public GameSetupManager(DataLoader dataLoader) {
    //     this.dataLoader = dataLoader; // Ejemplo
    // }


    public NormalModeSetupData setupNormalMode() throws Exception {
        System.out.println("GameSetupManager: Setting up Normal Mode...");
        System.out.println("GameSetupManager: Attempting to load movements from: " + MOVEMENTS_RESOURCE_PATH);

        List<MovementsData> movementsDataList = DataLoader.loadMovementsData(MOVEMENTS_RESOURCE_PATH);
        if (movementsDataList == null) {
            throw new Exception("Failed to load movements data from: " + MOVEMENTS_RESOURCE_PATH);
        }
        System.out.println("GameSetupManager: Loaded " + movementsDataList.size() + " movement data entries.");

        Map<String, Movements> allAvailableMoves = movementsDataList.stream()
                .map(Movements::fromData)
                .filter(Objects::nonNull) 
                .collect(Collectors.toMap(Movements::getNombre, Function.identity(), (existing, replacement) -> existing));
        System.out.println("GameSetupManager: Created " + allAvailableMoves.size() + " unique Movement objects.");

        System.out.println("GameSetupManager: Attempting to load pokemons from: " + POKEMONS_RESOURCE_PATH);
        List<PokemonData> pokemonDataList = DataLoader.loadPokemonsData(POKEMONS_RESOURCE_PATH);
        if (pokemonDataList == null) {
            throw new Exception("Failed to load pokemon data from: " + POKEMONS_RESOURCE_PATH);
        }
        System.out.println("GameSetupManager: Loaded " + pokemonDataList.size() + " Pokemon data entries.");

        List<Pokemon> allAvailablePokemons = pokemonDataList.stream()
                .map(pData -> {
                    // --- MODIFICACIÓN AQUÍ ---
                    // Comprueba si el nombre del Pokémon es "PaisaVerse" (ignorando mayúsculas/minúsculas)
                    if ("PaisaVerse".equalsIgnoreCase(pData.getNombre())) {
                        // Si es "PaisaVerse", créalo como un HealerPokemon
                        System.out.println("INFO: Creando " + pData.getNombre() + " como HealerPokemon.");
                        return new HealerPokemon(pData, allAvailableMoves, DEFAULT_POKEMON_LEVEL);
                    } else {
                        // De lo contrario, créalo como un Pokemon normal
                        return new Pokemon(pData, allAvailableMoves, DEFAULT_POKEMON_LEVEL);
                    }
                })
                .filter(Objects::nonNull) 
                .collect(Collectors.toList());
        System.out.println("GameSetupManager: Created " + allAvailablePokemons.size() + " Pokemon objects with their moves at level " + DEFAULT_POKEMON_LEVEL + ".");

        List<Item> allAvailableItems = createDefaultItems();
        System.out.println("GameSetupManager: Created " + allAvailableItems.size() + " Item objects manually.");

        return new NormalModeSetupData(allAvailablePokemons, allAvailableItems);
    }

    /**
     * Creates a default list of available items manually.
     * This list includes all items implemented so far.
     * @return A list of Item objects.
     */
    private List<Item> createDefaultItems() {
        List<Item> items = new ArrayList<>();
        
        // Ítems de Curación
        items.add(new Potion());
        items.add(new SuperPotion());
        items.add(new HyperPotion());
        items.add(new Revive());
        
        // Ítems X (Mejora de Estadísticas del Usuario)
        items.add(new AtaqueX());
        items.add(new DefensaX());
        items.add(new VelocidadX());
        items.add(new AtaqueEspX());
        items.add(new DefensaEspX());

        // Ítems de Daño Directo
        items.add(new BombaAliada());
        items.add(new BombaRival());
        
        return items;
    }

    /**
     * Simple inner class to group the setup data for Normal Mode.
     */
    public static class NormalModeSetupData {
        private final List<Pokemon> availablePokemons;
        private final List<Item> availableItems;

        public NormalModeSetupData(List<Pokemon> availablePokemons, List<Item> availableItems) {
            this.availablePokemons = availablePokemons;
            this.availableItems = availableItems;
        }

        public List<Pokemon> getAvailablePokemons() { return availablePokemons; }
        public List<Item> getAvailableItems() { return availableItems; }
    }
}