package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.Severity;

/**
 * Represents metadata associated with logging operations. Encapsulates details
 * such as expected ranges, accepted margins, validity of data within margins,
 * and severity levels.
 *
 * @param expectedRange         The range within which the values are expected
 *                              to fall, represented by a range of fractions.
 * @param absoluteMargin        The absolute margin allowed for deviations from
 *                              the expected range.
 * @param allowedMargin         The relative or absolute margin allowed for
 *                              deviations, represented as a fraction.
 * @param allowedAbsoluteMargin The allowed margin expressed as an absolute
 *                              value.
 * @param withinMargin          Indicates whether the values fall within the
 *                              defined margins.
 * @param severity              The level of severity associated with the log
 *                              metadata, represented by a predefined severity
 *                              type.
 */
public record LogMeta(_Range<Fraction> expectedRange, double absoluteMargin,
                      works.lysenko.util.apis.data._RangedMargin allowedMargin,
                      double allowedAbsoluteMargin, boolean withinMargin, Severity severity) {

}
