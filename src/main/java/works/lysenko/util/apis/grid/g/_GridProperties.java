package works.lysenko.util.apis.grid.g;

import works.lysenko.util.apis.data._ScaledProperties;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.data.strs.Swap;
import works.lysenko.util.grid.GridProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.DOES;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Properties.arePropertiesStoredToFile;
import static works.lysenko.util.lang.word.E.EXIST;
import static works.lysenko.util.lang.word.G.GRIDS;
import static works.lysenko.util.spec.Layout.Parts.GRID_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;
import static works.lysenko.util.spec.Layout.s;

/**
 * Interface representing operations related to grid properties, including retrieval, checking existence, and
 * writing properties to grid files. This interface provides utility methods that handle grids based on their names
 * or paths.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "StaticMethodOnlyUsedInOneClass"})
public interface _GridProperties {

    /**
     * Retrieves the file name of a grid given its path.
     *
     * @param name the path or name of the file
     * @return the file name of the grid
     */
    static String getGridFileName(final String name) {

        final String grid;

        if (name.startsWith(s)) {
            final String container = GRIDS;
            final int startIndex = name.indexOf(container) + container.length() + 1;
            grid = name.substring(startIndex, name.length() - Swap.s(GRID_EXTENSION).length());
        } else grid = name;

        return grid;
    }

    /**
     * Retrieves the path of a grid given its name.
     *
     * @param name the name of the grid
     * @return the path of the grid
     */
    static String getGridPath(final String name) {

        return Swap.s(_GRIDS_, getGridFileName(name), GRID_EXTENSION);
    }

    /**
     * Retrieves a GridProperties object with the specified name and aspect ratio.
     *
     * @param name the name of the grid file
     * @return a new GridProperties object with the given name and aspect ratio
     */
    static _GridProperties getGridPropertiesByName(final String name) {

        return new GridProperties(name, false);
    }

    /**
     * Retrieves a GridProperties object with the specified name.
     *
     * @param path the name of the grid file
     * @return a new GridProperties object with the given name
     */
    static _GridProperties getGridPropertiesByPath(final String path) {

        return new GridProperties(path, true);
    }

    /**
     * Checks if a grid is defined.
     *
     * @param name the name of the grid
     * @return true if the grid is defined, false otherwise
     */
    static boolean isGridDefined(final String name) {

        final Path path = Paths.get(getGridPath(name));
        final boolean result = Files.exists(path);
        if (!result) logDebug(b(q(path.toString()), DOES, NOT, EXIST));
        return result;
    }

    /**
     * Writes the provided Properties object to a grid file.
     *
     * @param properties the Properties object to be written
     * @param name       the name of the grid file
     */
    static void putPropertiesToGridFile(final _GridProperties properties, final String name) {

        arePropertiesStoredToFile(((_ScaledProperties) properties).get(), getGridPath(name));
    }

    /**
     * Retrieves a key-value pair for a given key, optionally allowing null values.
     *
     * @param key           the key for which the key-value pair is to be retrieved
     * @param isNullAllowed flag indicating whether null values are allowed
     * @return a KeyValue pair containing the key and its associated value
     */
    KeyValue<String, String> getKV(final String key, boolean isNullAllowed);

    /**
     * Retrieves the value of the property associated with the given key.
     * If the property is not present, the default value provided is returned.
     *
     * @param key the key of the property to retrieve
     * @param def the default value to return if the property is not found
     * @return the value of the property associated with the key, or the default value if not found
     */
    KeyValue<String, String> getKV(String key, String def);

    /**
     * Retrieves a key-value pair for the specified key. If the key does not exist,
     * the provided default value is returned as the associated value in the key-value pair.
     *
     * @param key the key for which the key-value pair is to be retrieved
     * @param def the default value to be used if the key is not found
     * @return a KeyValue object containing the key and its associated value, or the default value if the key is not found
     */
    KeyValue<String, RangedMargin> getKV(String key, RangedMargin def);

    /**
     * Updates the property associated with the given key in the properties object.
     *
     * @param key   the key of the property to be updated
     * @param value the new value to set for the property
     */
    void updateProperty(String key, String value);
}