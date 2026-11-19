package works.lysenko.util.apis.grid.t;

/**
 * Represents an interface for spreading and matching colours based on a given spread value.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ColourSpread {

    /**
     * Determines if the given RGB value fits within the spread value of the ColourSpread object.
     *
     * @param rgb the RGB color value to check for fit
     * @return true if the RGB value fits within the spread value, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean fits(final int rgb);

    /**
     * @return The name of the object.
     */
    String name();

    /**
     * Retrieves the value associated with this object.
     *
     * @return the value associated with this object
     */
    Integer value();
}
