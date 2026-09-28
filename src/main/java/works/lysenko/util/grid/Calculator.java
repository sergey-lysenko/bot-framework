package works.lysenko.util.grid;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._GridCalculator;
import works.lysenko.util.data.records.Element;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.grid.record.rgbc.HSB;
import works.lysenko.util.prop.grid.Stamps;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.records.RGB24.rgb24;
import static works.lysenko.util.data.strs.Compressor.compress;

/**
 * Represents a Calculator object that performs various calculations on a grid.
 */
@SuppressWarnings("MethodWithMultipleLoops")
public class Calculator implements _GridCalculator {

    /**
     * Represents the radix or base used for number representation in the Calculator class.
     * <p>
     * The value of this constant is fixed at 36, which means it is typically used
     * for representing alphanumeric characters (0-9 and A-Z) in calculations or
     * conversions performed by the associated methods in the Calculator.
     */
    private static final int RADIX = 36;
    private final Grid grid;

    /**
     * Constructs a Calculator object with the provided Grid.
     *
     * @param grid the grid to perform calculations on
     */
    public Calculator(final Grid grid) {

        this.grid = grid;
    }

    @Override
    public final HSB[][] countHSBgrid() {

        final int height = grid.getGrid().length;
        final int width = grid.getGrid()[0].length;
        final HSB[][] hsb = new HSB[height][width];

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                hsb[i][j] = HSB.convertRGBtoHSB(rgb24(isNull(grid.getGrid()[i][j]) ? null : grid.getGrid()[i][j].colour()));
        return hsb;
    }

    public final Map<Integer, Pair<Integer, String>> countPixelsByBrightness(final int fences) {

        return countPixels(HSB::brightness, fences);
    }

    @SuppressWarnings("ObjectAllocationInLoop")
    public final Map<Integer, Pair<Integer, String>> countPixelsByColor() {

        final Map<Integer, Integer> counts = new HashMap<>(0);
        final Map<Integer, BitSet> bins = new HashMap<>(0);
        int i = -1;
        for (final Element[] row : grid.getGrid())
            for (final Element pixel : row) {
                i++;
                if (null != pixel) {
                    counts.put(pixel.colour(), counts.getOrDefault(pixel.colour(), 0) + 1);
                    bins.put(pixel.colour(), bins.getOrDefault(pixel.colour(), new BitSet(1)));
                    bins.get(pixel.colour()).set(i);
                }
            }

        return getIntegerPairMap(counts, bins);
    }

    /**
     * Counts the number of pixels based on a given property extracted from the HSB colour representation.
     *
     * @param propertyExtractor a function that extracts a property from the HSB colour representation
     * @param fences            the number of fences levels
     * @return a result containing the count of pixels for each quantised property value
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "ObjectAllocationInLoop"})
    private Map<Integer, Pair<Integer, String>> countPixels(final Function<? super HSB, ? extends Fraction> propertyExtractor, final int fences) {

        if (isNull(propertyExtractor)) return null;

        final Map<Integer, Integer> counts = new HashMap<>(0);
        final Map<Integer, BitSet> bins = new HashMap<>(0);
        int i = -1;
        for (final HSB[] row : countHSBgrid()) {
            for (final HSB hsb : row) {
                i++;
                if (null != hsb) {
                    final Fraction property = propertyExtractor.apply(hsb);
                    final int value = Math.round(property.floatValue() * fences);
                    counts.put(value, counts.getOrDefault(value, 0) + 1);
                    bins.put(value, bins.getOrDefault(value, new BitSet(1)));
                    bins.get(value).set(i);
                }
            }
        }

        return getIntegerPairMap(counts, bins);
    }

    public final Map<Integer, Pair<Integer, String>> countPixelsByHue(final int fences) {

        return countPixels(HSB::hue, fences);
    }

    public final Map<Integer, Pair<Integer, String>> countPixelsBySaturation(final int fences) {

        return countPixels(HSB::saturation, fences);
    }

    /**
     * Processes input maps to generate a mapping of integers to pairs, where each pair consists of a count and an encoded
     * string representation.
     *
     * @param counts a map containing integer keys and their associated counts
     * @param bins   a map containing integer keys and their associated BitSet values that represent data to encode
     * @return a map where each key corresponds to an integer from the input maps, and each value is a pair containing the
     * count from {@code counts} and an encoded string generated
     * from the associated {@code BitSet} in {@code bins}
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    private static Map<Integer, Pair<Integer, String>> getIntegerPairMap(final Map<Integer, Integer> counts,
                                                                         final Map<Integer, ? extends BitSet> bins) {

        final Map<Integer, Pair<Integer, String>> result = new HashMap<>(counts.size());

        final java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        for (final Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            final BitSet bin = bins.get(entry.getKey());
            final StringBuilder encoded = new StringBuilder(1);

            if (Stamps.process) {

                int start = bin.nextSetBit(0);
                while (0 <= start) {
                    int end = start;
                    int next = bin.nextSetBit(end + 1);

                    while (0 <= next && 8 > (next - end)) {
                        end = next;
                        next = bin.nextSetBit(end + 1);
                    }

                    if (!encoded.isEmpty()) {
                        encoded.append("~");
                    }

                    final BitSet slice = bin.get(start, end + 1);
                    final byte[] bytes = slice.toByteArray();
                    final String b64 = encoder.encodeToString(bytes);

                    encoded.append(Integer.toString(start, RADIX)).append("=").append(b64);
                    start = next;
                }
            }

            final String encodedString = encoded.toString();
            final String compressed = Stamps.compress ? compress(encodedString) : null;
            final String stamp = Stamps.compress && (compressed.length() < encodedString.length()) ? compressed : encodedString;
            result.put(entry.getKey(), new Pair<>(entry.getValue(),
                    encoded.isEmpty() ? "" : stamp));
        }

        return result;
    }
}
