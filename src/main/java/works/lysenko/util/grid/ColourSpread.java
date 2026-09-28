package works.lysenko.util.grid;

import works.lysenko.util.apis.grid.t._ColourSpread;
import works.lysenko.util.data.records.RGB24;

import java.util.*;

import static works.lysenko.util.data.records.RGB24.rgb24;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.grid.record.rgbc.Colour.distance;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Layout.Files.knownColours;
import static works.lysenko.util.spec.Numbers.ONE;

/**
 * Represents a class for spreading and matching colours based on a given spread value.
 */

public class ColourSpread implements _ColourSpread {

    private final String name;
    private final Integer value;
    private final int spread;
    private final Set<Integer> fits = new HashSet<>(ONE);
    private final Set<Integer> not = new HashSet<>(ONE);

    /**
     * Constructs a ColourSpread object with the given name and spread value.
     * It calculates the Integer value using the Known colours and initialises the ColourSpread object.
     *
     * @param name   the name of the colour
     * @param spread the spread value to determine the fit of colours
     */
    public ColourSpread(final String name, final int spread) {

        this.name = name;
        this.spread = spread;
        value = i(knownColours.getProperty(name));
    }

    /**
     * Returns a new Collection of ColourSpread objects initialised with the provided ColourSpread objects.
     *
     * @param cs the array of ColourSpread objects to be included in the Collection
     * @return a Collection of ColourSpread objects
     */
    public static Collection<ColourSpread> cs(final ColourSpread... cs) {

        return new ArrayList<>(Arrays.asList(cs));
    }

    /**
     * Creates a new ColourSpread object with the given colour name and spread value.
     *
     * @param colour the name of the colour
     * @param spread the spread value to determine colour fit
     * @return a new ColourSpread object with the specified colour and spread
     */
    public static ColourSpread cs(final String colour, final int spread) {

        return new ColourSpread(colour, spread);
    }


    /**
     * Creates a new ColourSpread object with the given colour and a spread value of 0.
     *
     * @param colour the name of the colour
     * @return a new ColourSpread object with the specified colour and a spread value of 0
     */
    public static ColourSpread cs(final String colour) {

        return new ColourSpread(colour, 0);
    }

    @SuppressWarnings("DataFlowIssue")
    public final boolean fits(final int rgb) {

        // return rgb == value;
        if (isUnknown(rgb)) {
            final RGB24 origin = rgb24(value);
            final RGB24 sample = rgb24(rgb);
            final double distance = distance(origin, sample);
            if (distance <= spread) add(rgb);
            else not.add(rgb);
        }
        return fits.contains(rgb);
    }

    public final String name() {

        return name;
    }

    @Override
    public final String toString() {

        return s(b(L0, name, s(value), s(spread), s(fits.size()), s(not.size())));
    }

    public final Integer value() {

        return value;
    }

    private void add(final int rgb) {

        fits.add(rgb);
    }

    private boolean isUnknown(final int rgb) {

        return (!fits.contains(rgb) && !not.contains(rgb));
    }
}

