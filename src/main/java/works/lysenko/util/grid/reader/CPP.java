package works.lysenko.util.grid.reader;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.r._Palette;
import works.lysenko.util.apis.grid.r._Polychromy;
import works.lysenko.util.data.enums.ColoursIgnore;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.grid.expected.ColoursQuotas;
import works.lysenko.util.grid.expected.Palette;
import works.lysenko.util.grid.expected.Polychromy;
import works.lysenko.util.grid.record.misc.IgnoreHSB;
import works.lysenko.util.grid.record.misc.Limits;
import works.lysenko.util.prop.grid.Allowed;
import works.lysenko.util.prop.grid.Validation;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.fraction.Fraction.ONE;
import static org.apache.commons.math3.fraction.Fraction.ZERO;
import static works.lysenko.util.data.range.FractionRange.frr;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.grid.Fields.*;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.P.PALETTE;
import static works.lysenko.util.lang.word.P.POLYCHROMY;
import static works.lysenko.util.lang.word.U.UPDATE;

/**
 *
 */
@SuppressWarnings({"NestedConditionalExpression", "StaticMethodOnlyUsedInOneClass"})
public record CPP() {

    /**
     * Retrieves the ColoursQuotas based on the provided parameters.
     *
     * @param grid       The _ScaledProperties grid containing colour-related key-value pairs
     * @param predefined The ImageRequirements object containing predefined colour information
     * @param origin     The origin information for the ColoursQuotas
     * @param silent     A1 boolean indicating whether to run the operation silently
     * @return The ColoursQuotas object based on the inputs and predefined colours
     */
    public static _ColoursQuotas getColours(final _GridProperties grid, final _ImageRequirements predefined,
                                            final String origin, final boolean silent) {

        final String coloursProperty = grid.getKV(COLOURS, true).v();
        final String coloursIgnore = grid.getKV(CDI, true).v();
        final String coloursAmount = grid.getKV(CDA, true).v();

        return (isAnyNull(predefined, isNotNull(predefined)
                ? predefined.colours() : null)) ? (isNull(coloursProperty)) ? null :
                new ColoursQuotas(origin, coloursProperty,
                        Limits.of(
                                getV(grid, CDB, Validation.defaultColoursBorder),
                                getRV(grid, CDD, Allowed.defaultRangedMargin)),
                        ColoursIgnore.set(coloursIgnore),
                        IgnoreHSB.of(getI(grid, CDIH), getI(grid, CDIS), getI(grid, CDIB)),
                        (isNull(coloursAmount)) ? null :
                                new IntegerRange(coloursAmount), silent) : predefined.colours();
    }

    /**
     * Retrieves a FractionRange based on the specified key from the _ScaledProperties grid.
     *
     * @param grid the _ScaledProperties grid containing key-value pairs
     * @param key  the key to look up in the grid
     * @return a FractionRange object if the key is present and valid in the grid, otherwise null
     */
    @SuppressWarnings("StandardVariableNames")
    private static FractionRange getI(final _GridProperties grid, final String key) {

        final String k = grid.getKV(key, true).v();
        return (isNull(k)) ? null : frr(k, ZERO, ONE);
    }

    /**
     * Retrieves a palette based on the provided _ScaledProperties grid and ImageRequirements object.
     *
     * @param grid       The _ScaledProperties grid containing colour-related key-value pairs
     * @param predefined The ImageRequirements object containing predefined colour information
     * @return The palette retrieved based on the inputs and predefined colours, or null if conditions are not met
     */
    public static _Palette getPalette(final _GridProperties grid, final _ImageRequirements predefined) {

        final String pV = grid.getKV(PALETTE, true).v();
        return (isAnyNull(predefined, isNotNull(predefined) ? predefined.palette() : null)) ? isNull(pV) ? null :
                new Palette(pV) : predefined.palette();
    }

    /**
     * Retrieves a Polychromy object based on the given _ScaledProperties grid and ImageRequirements.
     *
     * @param grid       The _ScaledProperties grid containing colour-related key-value pairs
     * @param predefined The ImageRequirements object containing predefined colour information
     * @return A1 Polychromy object based on the inputs and predefined colours, or null if conditions are not met
     */
    public static _Polychromy getPolychromy(final _GridProperties grid, final _ImageRequirements predefined) {

        final String pV = grid.getKV(POLYCHROMY, true).v();
        final String pM = grid.getKV(PDI, true).v();
        return (isAnyNull(predefined, isNotNull(predefined) ? predefined.polychromy() : null)) ? (null == pV) ? null :
                new Polychromy(pV,
                        grid.getKV(PDM, true).v(),
                        (isNotNull(pM) && pM.contains(UPDATE))) : predefined.polychromy();
    }

    /**
     * Retrieves the value associated with the given key from the _ScaledProperties grid.
     * If the key is not present, the default value is returned.
     *
     * @param grid the _ScaledProperties grid containing key-value pairs
     * @param key  the key to look up in the grid
     * @param def  the default value to return if the key is not found
     * @return the value associated with the key, or the default value if not found
     */
    @SuppressWarnings("SameParameterValue")
    private static RangedMargin getRV(final _GridProperties grid, final String key,
                                      final RangedMargin def) {

        return grid.getKV(key, def).v();
    }

    /**
     * Retrieves the value associated with the given key from the _ScaledProperties grid.
     * If the key is not present, the default value is returned.
     *
     * @param grid the _ScaledProperties grid containing key-value pairs
     * @param key  the key to look up in the grid
     * @param def  the default value to return if the key is not found
     * @return the value associated with the key, or the default value if not found
     */
    @SuppressWarnings("SameParameterValue")
    private static Fraction getV(final _GridProperties grid, final String key, final String def) {

        return fr(grid.getKV(key, def).v());
    }
}
