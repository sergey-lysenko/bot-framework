package works.lysenko.util.data.records;

import static java.util.Objects.isNull;
import static works.lysenko.util.chrs.___.RED;
import static works.lysenko.util.chrs.___.RGB;
import static works.lysenko.util.chrs.____.BLUE;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.G.GREEN;
import static works.lysenko.util.prop.data.Delimeters.L0;

/**
 * Represents an RGB color with 3*8-bit values for red, green, and blue channels.
 */
@SuppressWarnings("unused")
public record RGB24(int red, int green, int blue) {

    /**
     * Converts an RGB color value into a long string representation.
     *
     * @param rgb the RGB color value to be converted
     * @return a string representing the RGB color value
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints"})
    public static String longRgb(final int rgb) {

        final RGB24 colour = rgb24(rgb);
        if (isNotNull(colour)) {
            return a(java.util.List.of(
                            kv(RGB, rgb),
                            kv(RED, colour.red()),
                            kv(GREEN, colour.green()),
                            kv(BLUE, colour.blue())),
                    COMMA_SPACE);
        }
        return null;
    }

    /**
     * Converts an RGB color value into a RGB24 object representing the color's red, green, and blue channels.
     *
     * @param rgb the RGB color value to be converted
     * @return a RGB24 object representing the color's red, green, and blue channels
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static RGB24 rgb24(final Integer rgb) {

        if (isNull(rgb)) return null;
        final int red = (rgb >> 16) & 0xFF;
        final int green = (rgb >> 8) & 0xFF;
        final int blue = rgb & 0xFF;
        return new RGB24(red, green, blue);
    }

    /**
     * Converts an RGB color value into a short string representation.
     *
     * @param rgb the RGB color value to be converted
     * @return a string representing the short RGB color value
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static String shortRgb(final int rgb) {

        final RGB24 colour = rgb24(rgb);
        if (isNull(colour)) return null;
        return b(L0, rgb, colour.red(), colour.green(), colour.blue());
    }

    /**
     * Returns a long string representation of an RGB color value,
     * which includes the values for red, green, and blue channels.
     *
     * @return A1 string representing the RGB color value
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public String longRgb() {

        return a(java.util.List.of(
                        kv(RED, red()),
                        kv(GREEN, green()),
                        kv(BLUE, blue())),
                COMMA_SPACE);
    }

    /**
     * Converts an RGB color value into a short string representation.
     *
     * @return a string representing the short RGB color value
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public String shortRgb() {

        return b(L0, red(), green(), blue());
    }
}
