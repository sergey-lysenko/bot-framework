package works.lysenko.util.func.grid.ranger;

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

import static works.lysenko.util.func.grid.ranger.Verify.isIntegerValueInRange;

/**
 * Checks if the values passed in are within the expected ranges.
 */
public class OfIntegers extends AbstractRanger<Integer> {

    /**
     * Constructs an instance of the OfIntegers class, which is used to validate and check
     * whether integer values fall within the expected ranges.
     *
     * @param meta        The metadata associated with the validation process, including severity, subject, and margin.
     * @param actual      A map containing integer keys and their corresponding actual fractional values.
     * @param expected    The expected quotas of integers to be validated against.
     * @param amount      The range of integers allowed for validation.
     * @param ignoreOrder A boolean flag indicating whether the order of elements should be ignored in the validation process.
     * @param ignoreOther A boolean flag indicating whether unrelated integers should be ignored during validation.
     * @param renderers   Renderers used to render the validation results.
     * @param severity    The severity level of the validation process.
     */
    public OfIntegers(final ValidationMeta meta,
                      final Map<Integer, _ValuedRangeResult> actual,
                      final _Quotas<Integer> expected,
                      final IntegerRange amount,
                      final boolean ignoreOrder,
                      final boolean ignoreOther,
                      final Renderers renderers,
                      final Severity severity) {

        super(meta, actual, expected, amount, ignoreOrder, ignoreOther, renderers, severity);
    }

    public final void addSample(final RangeResults toResults, final Quota<Integer> expectedShare, final Integer actualValue,
                                final _ValuedRangeResult actualShare) {

        toResults.add(isIntegerValueInRange(meta(), expectedShare, actualValue, actualShare));
    }
}
