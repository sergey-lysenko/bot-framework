package works.lysenko.util.grid.record.gsrc;

import works.lysenko.util.apis.grid.r._ColourSpreadRequest;
import works.lysenko.util.func.data.Regexes;
import works.lysenko.util.grid.ColourSpread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a data class for holding a Colour Search Request, consisting of a normal distance value and a collection of
 * ColourSpread objects.
 * This class provides a method to check if a given RGB value fits any of the ColourSpread objects in the collection.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record ColourSearchRequest(Integer normal, Collection<ColourSpread> spreads) implements _ColourSpreadRequest {

    /**
     * Creates a Colour Search Request object with the specified normal distance value and a single colour zero spread.
     *
     * @param normal the normal distance value for the Colour Search Request
     * @param colour the colour to be added as a single ColourSpread to the search request
     * @return a new Colour Search Request object with the specified normal distance value and single ColourSpread
     */
    public static ColourSearchRequest csr(final Integer normal, final String colour) {

        return csr(normal, colour, ZERO);
    }

    /**
     * Creates a Colour Search Request object with the specified normal distance value and a single ColourSpread object.
     *
     * @param normal the normal distance value for the Colour Search Request
     * @param colour the colour to be added to the single ColourSpread in the search request
     * @param spread the spread value for the colour in the single ColourSpread
     * @return a new Colour Search Request object with the specified normal distance value and single ColourSpread
     */
    public static ColourSearchRequest csr(final Integer normal, final String colour, final int spread) {

        final Collection<ColourSpread> single = new ArrayList<>(ZERO);
        single.add(new ColourSpread(colour, spread));
        return new ColourSearchRequest(normal, single);
    }

    /**
     * Creates a Colour Search Request object with the specified normal distance value and a collection of ColourSpread
     * objects.
     *
     * @param normal  the normal distance value for the Colour Search Request
     * @param spreads the collection of ColourSpread objects for the Colour Search Request
     * @return a new Colour Search Request object
     */
    public static ColourSearchRequest csr(final Integer normal, final Collection<ColourSpread> spreads) {

        return new ColourSearchRequest(normal, spreads);
    }

    /**
     * Creates a Colour Search Request object based on the provided string.
     *
     * @param stringCSR the string representing the Colour Search Request
     * @return a new Colour Search Request object if the input is not null and has a length greater than zero,
     * null otherwise
     */
    public static ColourSearchRequest csr(final String stringCSR) {

        if (isNotNull(stringCSR) && stringCSR.length() > ZERO) return getColourSearchRequest(stringCSR);
        else return null;
    }

    private static ColourSearchRequest getColourSearchRequest(final String csrString) {

        final String[] l2 = csrString.split(Regexes.l2);
        if (ZERO == l2.length) return null;
        final Integer normal = l2.length > ONE ? i(l2[0]) : null;
        final String spreadsString = l2.length > ONE ? l2[1] : l2[0];

        final Collection<ColourSpread> spreads = getColourSpreads(spreadsString);
        if (isNull(spreads)) return null;
        return new ColourSearchRequest(normal, spreads);
    }

    /**
     * Retrieves a collection of ColourSpread instances from the provided spreads string.
     *
     * @param spreadsString the string containing colour spreads to parse
     * @return a collection of ColourSpread instances parsed from the spreads string, or null if invalid input
     */
    public static Collection<ColourSpread> getColourSpreads(final String spreadsString) {

        if (isNull(spreadsString)) return null;

        final String[] l1 = spreadsString.split(Regexes.l1);
        if (ZERO == l1.length) return null;

        final Collection<ColourSpread> spreads = new ArrayList<>(ZERO);
        for (final String spreadString : l1)
            if (isSpreadInvalid(spreadString, spreads)) return null;
        return spreads;
    }

    private static boolean isSpreadInvalid(final String spreadString, final Collection<? super ColourSpread> spreads) {

        final String[] l0 = spreadString.split(Regexes.l0);
        if (ZERO == l0.length) return true;
        final String colour = l0[0];
        final int spread = (ONE < l0.length) ? inn(l0[1]) : ZERO;
        spreads.add(new ColourSpread(colour, spread));
        return false;
    }

    public boolean fits(final int rgb) {

        for (final ColourSpread spread : spreads) {
            if (spread.fits(rgb)) return true;
        }
        return false;
    }

    public Collection<String> getUnknownColours() {

        final Collection<String> result = new HashSet<>(0);
        for (final ColourSpread spread : spreads)
            if (isNull(spread.value())) result.add(spread.name());
        return result;
    }
}
