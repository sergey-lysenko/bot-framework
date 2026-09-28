package works.lysenko.util.apis.grid.t;

/**
 * _Colour is an interface that represents a color object.
 * It provides methods to retrieve the RGB value of the color
 * and to get string representations of the color object.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Colour {

    /**
     * Returns the name of the Colour.
     *
     * @return The name of the Colour.
     */
    String name();

    /**
     * Returns the RGB value of the color.
     *
     * @return The RGB value of the color.
     */
    int rgb();

    /**
     * Returns a string representation of the Colour object.
     * <p>
     * The string representation includes the name of the Colour object and its corresponding RGB value,
     * as well as the individual red, green, and blue colour components.
     *
     * @return A1 string representation of the Colour object.
     */
    String toLongString();

    /**
     * Returns a string representation of the Colour object.
     * <p>
     * The string representation includes the name of the Colour object and its corresponding RGB value,
     * as well as the individual red, green, and blue colour components.
     *
     * @return A1 string representation of the Colour object.
     */
    String toShortString();

}
