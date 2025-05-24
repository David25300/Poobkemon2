package PokeBody.Data;

// Item ya no se importa aquí si no se carga desde JSON
// import PokeBody.domain.Item; 
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * Utility class for loading data from JSON resources.
 * Assumes JSON files are located in the classpath (e.g., src/main/resources).
 */
public class DataLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Loads a list of objects of any type T from a JSON resource file.
     *
     * @param resourcePath Path to the JSON resource file within the classpath (e.g., "/data/pokemons.json").
     * @param typeRef      TypeReference specifying the target list type (e.g., new TypeReference<List<PokemonData>>() {}).
     * @param <T>          The type of objects in the list.
     * @return A List of objects loaded from the JSON resource.
     * @throws Exception If the resource cannot be found or read, or if JSON parsing fails.
     */
    public static <T> List<T> loadList(String resourcePath, TypeReference<List<T>> typeRef) throws Exception {
        InputStream inputStream = DataLoader.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new Exception("Resource not found: " + resourcePath);
        }
        try (InputStream is = inputStream) {
            return MAPPER.readValue(is, typeRef);
        }
    }

    /**
     * Loads a list of PokemonData objects from a JSON resource file.
     *
     * @param resourcePath Path to the JSON resource file (e.g., "/pokemons.json").
     * @return A List of PokemonData objects.
     * @throws Exception If loading or parsing fails.
     */
    public static List<PokemonData> loadPokemonsData(String resourcePath) throws Exception {
        return loadList(resourcePath, new TypeReference<List<PokemonData>>() {});
    }

    /**
     * Loads a list of MovementsData objects from a JSON resource file.
     *
     * @param resourcePath Path to the JSON resource file (e.g., "/movements.json").
     * @return A List of MovementsData objects.
     * @throws Exception If loading or parsing fails.
     */
    public static List<MovementsData> loadMovementsData(String resourcePath) throws Exception {
        return loadList(resourcePath, new TypeReference<List<MovementsData>>() {});
    }

    // El método loadItemsData(String resourcePath) ha sido eliminado.
    // Los ítems se crearán manualmente.
}
