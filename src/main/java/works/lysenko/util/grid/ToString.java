package works.lysenko.util.grid;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._GridToString;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.data.records.Element;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.grid.record.misc.IgnoreHSB;
import works.lysenko.util.grid.record.rgbc.Colour;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.func.type.Collector.mapToString;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.prop.data.Delimeters.L0;

/**
 * This class implements the _GridToString interface and provides implementations for various methods to convert the grid
 * data to string representations.
 */
@SuppressWarnings("CallToNumericToString")
public class ToString implements _GridToString {

    private final Grid grid;

    /**
     * Provides a string representation of a Grid object
     *
     * @param grid The Grids object to convert to a string representation
     */
    public ToString(final Grid grid) {

        this.grid = grid;

    }

    // TODO: rename to better variants

    public final String byShares_Brightnesses(final _FractionQuotas brightness, final int brightnessQ) {

        return mapToString(grid.getProcessor().getBrightnessesByRates(brightness),
                fraction -> fraction.multiply(brightnessQ).toString(),
                value -> ts(fr(value.toString())));
    }

    @Override
    public final String byShares_Colors(final Fraction threshold) {

        return mapToString(grid.getProcessor().getColoursByRates(threshold), Colour::shortOfValue,
                value -> ts(fr(value.toString())));
    }

    @Override
    public final String byShares_Colours(final Fraction threshold, final IgnoreHSB ignoreHSB) {

        return mapToString(grid.getProcessor().getColoursByRates(threshold, ignoreHSB), Colour::shortOfValue,
                value -> ts(fr(value.toString())));
    }

    public final String byShares_Hues(final _FractionQuotas hue, final int hueQ) {

        return mapToString(grid.getProcessor().getHuesByRates(hue),
                fraction -> fraction.multiply(hueQ).toString(),
                value -> ts(fr(value.toString())));
    }

    public final String byShares_Saturations(final _FractionQuotas saturation, final int saturationQ) {

        return mapToString(grid.getProcessor().getSaturationsByRates(saturation),
                fraction -> fraction.multiply(saturationQ).toString(),
                value -> ts(fr(value.toString())));
    }

    @Override
    public final String colorsPropertyValue(final Fraction threshold, final IgnoreHSB ignoreHSB) {

        return mapToString(grid.getProcessor().getColoursByRates(threshold, ignoreHSB), Object::toString, L0,
                value -> ts(fr(value.toString())));
    }

    public final String pixelsCountByColor() {

        final Map<Integer, Pair<Integer, String>> map = grid.getCalculator().countPixelsByColor();
        return mapToString(map, Colour::shortOfValue, Object::toString);
    }

    @Override
    public final List<String> toListString() {

        final List<String> rows = new ArrayList<>(0);
        for (final Element[] row : grid.getGrid())
            rows.add(StringUtils.join(row, COMMA_SPACE));
        return rows;
    }
}
