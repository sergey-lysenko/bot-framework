package works.lysenko.util.grid.record.rgbc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.RGB24;

import java.awt.Color;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.S.SATURATION;
import static works.lysenko.util.prop.data.Delimeters.L0;

/**
 * Represents an RGB color with 24-bit values for red, green, and blue channels.
 */
public record HSB(Fraction hue, Fraction saturation, Fraction brightness) {

    /**
     * Converts an RGB color to HSB color representation.
     *
     * @param rgb the RGB color to be converted
     * @return the HSB color representation of the RGB color
     */
    @SuppressWarnings({"unused", "MethodWithMultipleReturnPoints"})
    public static HSB convertRGBtoHSB(final RGB24 rgb) {

        if (isNull(rgb)) return null;
        final Color color = new Color(rgb.red(), rgb.green(), rgb.blue());
        final float[] vals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new HSB(fr(vals[0]), fr(vals[1]), fr(vals[2]));
    }

    /**
     * Constructs a new HSB colour based on the provided hue, saturation, and brightness fractions.
     *
     * @param hue        the fraction representing the hue component of the HSB colour
     * @param saturation the fraction representing the saturation component of the HSB colour
     * @param brightness the fraction representing the brightness component of the HSB colour
     * @return a new HSB color object with the specified hue, saturation, and brightness values
     */
    public static HSB hsb(final Fraction hue, final Fraction saturation, final Fraction brightness) {

        return new HSB(hue, saturation, brightness);
    }

    /**
     * Returns a string representation of the HSB color values.
     *
     * @return a string representing the HSB color values
     */
    @SuppressWarnings("unused")
    String longHSB() {

        return a(java.util.List.of(
                        kv(HUE, ts(hue)),
                        kv(SATURATION, ts(saturation)),
                        kv(BRIGHTNESS, ts(brightness))),
                COMMA_SPACE);
    }

    /**
     * Generates a short string representation of the HSB color values.
     *
     * @return A1 string representing the HSB color values in a short format.
     */
    @SuppressWarnings("unused")
    String shortHSB() {

        return b(L0, ts(hue), ts(saturation), ts(brightness));
    }
}
