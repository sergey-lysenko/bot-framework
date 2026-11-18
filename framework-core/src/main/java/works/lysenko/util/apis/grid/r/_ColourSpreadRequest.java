package works.lysenko.util.apis.grid.r;

import java.util.Collection;

/**
 * Represents a request to check if a given RGB value fits any of the colour spreads.
 */
@SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "InterfaceWithOnlyOneDirectInheritor"})
public interface _ColourSpreadRequest {

    /**
     * Probe if the given RGB value fits any of the colour spreads.
     *
     * @param rgb the RGB value to check
     * @return true if the RGB value fits any colour spread, false otherwise
     */
    boolean fits(final int rgb);

    /**
     * Retrieves a collection of unknown colors from the ColourSpreadRequest.
     *
     * @return a collection of unknown color names
     */
    Collection<String> getUnknownColours();

}
