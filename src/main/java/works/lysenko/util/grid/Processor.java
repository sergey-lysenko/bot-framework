package works.lysenko.util.grid;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._GridCalculator;
import works.lysenko.util.apis.grid.g._GridProcessor;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.func.grid.colours.ValuedRangeResult;
import works.lysenko.util.grid.record.misc.IgnoreHSB;
import works.lysenko.util.grid.record.rgbc.HSB;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.range.AbstractQuotas.LOWER_LIMIT;
import static works.lysenko.util.data.records.RGB24.rgb24;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.prop.grid.Validation.fences;

/**
 * The Processor class implements the _GridProcessor interface and provides methods for calculating rates of pixel values.
 */
@SuppressWarnings({"MissingJavadoc", "DataFlowIssue", "StandardVariableNames"})
public class Processor implements _GridProcessor {

    private final _GridCalculator calculator;

    public Processor(final _GridCalculator calculator) {

        this.calculator = calculator;
    }

    /**
     * Calculates the rates of each entry in the given map based on the total number of pixels.
     * Each entry's value is converted into a rate by dividing it by the totalPixels parameter.
     *
     * @param values      a map where the keys are integers and the values are counts
     * @param totalPixels the total number of pixels used for calculating individual rates
     * @return a map where the keys are the same as the input map, and the values are the calculated rates as doubles
     */
    private static Map<Integer, Double> calculateRates(final Map<Integer, Integer> values, final int totalPixels) {

        return values.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> (double) e.getValue() / totalPixels));
    }

    /**
     * Transforms a given map of integer keys and ValuedRangeResult values into a map with Fraction keys
     * and corresponding ValuedRangeResult values. Each integer key is converted to a Fraction
     * using the specified denominator.
     *
     * @param q   the denominator to be used for generating Fraction keys from the integer keys
     * @param raw a map containing integer keys and their associated ValuedRangeResult values
     * @return a map where the integer keys from the input map are converted to Fraction keys
     * and mapped to their corresponding ValuedRangeResult values
     */
    private static Map<Fraction, _ValuedRangeResult> getFractionActualFractionMap(final int q, final Map<Integer, ?
            extends _ValuedRangeResult> raw) {

        final Map<Fraction, _ValuedRangeResult> map = new LinkedHashMap<>(raw.size());
        raw.forEach((key, value) -> {
            final Fraction hueFraction = fr(key, q);
            map.put(hueFraction, value);
        });
        return map;
    }

    /**
     * Retrieves the rates based on the given values.
     *
     * @param distribution a results containing the values as keys and their corresponding counts as values
     * @return a results containing the values as keys and their corresponding rates as Fraction values
     */
    @SuppressWarnings("unused")
    private static Map<Integer, _ValuedRangeResult> getRates(final Map<Integer, Pair<Integer, String>> distribution) {

        return getRatesWithThreshold(distribution, Fraction.ZERO);
    }

    /**
     * Processes a given distribution map to calculate and filter rates based on a specified threshold.
     * The method calculates rates from the distribution, filters and sorts them according to the threshold,
     * then maps these rates to ValuedRangeResult objects with corresponding metadata.
     *
     * @param distribution a map containing Integer keys and Pair values where:
     *                     -
     */
    private static Map<Integer, _ValuedRangeResult> getRatesWithThreshold(final Map<Integer, Pair<Integer, String>> distribution, final Fraction threshold) {

        final Map<Integer, Integer> reduced = distribution.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().left()));
        final int totalPixels = reduced.values().stream().mapToInt(Integer::intValue).sum();
        final Map<Integer, Double> rates = calculateRates(reduced, totalPixels); // Apply threshold here?
        final Map<Integer, Double> sorted = sortAndFilterRates(rates, threshold);
        final Map<Integer, _ValuedRangeResult> fractions = new LinkedHashMap<>(sorted.size());
        sorted.forEach((key, value) -> fractions.put(key, new ValuedRangeResult(fr(value), distribution.get(key).right())));
        return fractions;
    }

    /**
     * Filters and sorts the given map of rates by a specified threshold.
     * Only entries with values greater than or equal to the threshold are retained,
     * and the resulting map is sorted in descending order by value.
     *
     * @param rates     a map containing Integer keys and Double values representing rates
     * @param threshold a Fraction value used as the filtering threshold;
     *                  if null, it defaults to a threshold of 1.0
     * @return a map containing filtered and sorted rates, with Integer keys and Double values
     */
    private static Map<Integer, Double> sortAndFilterRates(final Map<Integer, Double> rates, final Fraction threshold) {

        final Fraction t = (isNull(threshold)) ? fr(1.0) : threshold;
        return rates.entrySet().stream().filter(e -> e.getValue() >= t.doubleValue()).sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    public final Map<Fraction, _ValuedRangeResult> getBrightnessesByRates(final _FractionQuotas brightnesses) {

        final int q = isNull(brightnesses) ? i(fences) : brightnesses.fences();
        final Fraction f = isNull(brightnesses) ? LOWER_LIMIT : brightnesses.threshold();
        final Map<Integer, _ValuedRangeResult> raw = getRatesWithThreshold(calculator.countPixelsByBrightness(q), f);
        return getFractionActualFractionMap(q, raw);
    }

    public final Map<Integer, _ValuedRangeResult> getColoursByRates() {

        return getRatesWithThreshold(calculator.countPixelsByColor(), Fraction.ZERO);
    }

    public final Map<Integer, _ValuedRangeResult> getColoursByRates(final Fraction threshold) {

        return getRatesWithThreshold(calculator.countPixelsByColor(), threshold);
    }

    @SuppressWarnings("ContinueStatement")
    public final Map<Integer, _ValuedRangeResult> getColoursByRates(final Fraction threshold, final IgnoreHSB ignoreHSB) {

        final Map<Integer, _ValuedRangeResult> in = getRatesWithThreshold(calculator.countPixelsByColor(), threshold);
        final Map<Integer, _ValuedRangeResult> out = new LinkedHashMap<>(0);
        for (final Map.Entry<Integer, _ValuedRangeResult> entry : in.entrySet()) {
            final HSB colourHSB = HSB.convertRGBtoHSB(rgb24(entry.getKey()));
            if (!isNull(colourHSB)) {
                if (isNotNull(ignoreHSB.hue()) && ignoreHSB.hue().includes(colourHSB.hue())) continue;
                if (isNotNull(ignoreHSB.brightness()) && ignoreHSB.brightness().includes(colourHSB.brightness())) continue;
                if (isNotNull(ignoreHSB.saturation()) && ignoreHSB.saturation().includes(colourHSB.saturation())) continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }

    @Override
    public final Map<Fraction, _ValuedRangeResult> getHuesByRates(final _FractionQuotas hues) {

        final int q = isNull(hues) ? i(fences) : hues.fences();
        final Fraction f = isNull(hues) ? LOWER_LIMIT : hues.threshold();
        final Map<Integer, _ValuedRangeResult> raw = getRatesWithThreshold(calculator.countPixelsByHue(q), f);
        return getFractionActualFractionMap(q, raw);
    }

    @Override
    public final Map<Fraction, _ValuedRangeResult> getSaturationsByRates(final _FractionQuotas saturations) {

        final int q = isNull(saturations) ? i(fences) : saturations.fences();
        final Fraction f = isNull(saturations) ? LOWER_LIMIT : saturations.threshold();
        final Map<Integer, _ValuedRangeResult> raw = getRatesWithThreshold(calculator.countPixelsBySaturation(q), f);
        return getFractionActualFractionMap(q, raw);
    }
}
