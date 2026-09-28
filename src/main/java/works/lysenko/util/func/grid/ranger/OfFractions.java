package works.lysenko.util.func.grid.ranger;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.type.list.RangeResults;
import works.lysenko.util.func.grid.AbstractRanger;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.meta.ValidationMeta;

import java.util.Map;

import static works.lysenko.util.func.grid.ranger.Verify.isFractionValueInRange;

/**
 * Represents a Ranger for Fraction values.
 */
public class OfFractions extends AbstractRanger<Fraction> {

    /**
     * Constructs an instance of OfFractions with the specified parameters.
     *
     * @param meta        The ValidationMeta object containing metadata for the validation process.
     * @param actual      A map associating Fraction keys with ValuedRangeResult values representing the actual fractions.
     * @param expected    An instance of _Quotas(Fraction) representing the expected quotas for the fractions.
     * @param amount      An instance of IntegerRange defining the valid range for the amount of fractions.
     * @param ignoreOrder A boolean flag indicating whether the order of fractions should be ignored in comparisons.
     * @param ignoreOther A boolean flag indicating whether other elements beyond expected values should be ignored.
     * @param renderers   An instance of Renderers used for rendering validation results.
     * @param severity    The severity level associated with the validation process.
     */
    public OfFractions(final ValidationMeta meta, final Map<Fraction, _ValuedRangeResult> actual,
                       final _Quotas<Fraction> expected, final IntegerRange amount, final boolean ignoreOrder,
                       final boolean ignoreOther,
                       final Renderers renderers, final Severity severity) {

        super(meta, actual, expected, amount, ignoreOrder, ignoreOther, renderers, severity);
    }

    public final void addSample(final RangeResults toResults, final Quota<Fraction> expectedShare,
                                final Fraction actualValue, final _ValuedRangeResult actualShare) {

        toResults.add(isFractionValueInRange(meta(), expectedShare, actualValue, actualShare));
    }
}
