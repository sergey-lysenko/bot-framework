package works.lysenko.util.grid.record.meta;

import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.grid.record.Request;

/**
 * Represents the metadata associated with a validation process.
 * <p>
 * This record encapsulates several attributes that define the scope
 * and characteristics of a validation operation, including severity,
 * subject, margin, number of fences, and the associated request.
 *
 * @param max     The maximum severity level encountered during the validation.
 * @param subject The subject of validation represented as a noun.
 * @param margin  A ranged margin specifying dynamic tolerance.
 * @param fences  The number of fences or limits defined in the context.
 * @param vr      The validation request associated with the metadata.
 */
public record ValidationMeta(Severity max, Subject subject, _RangedMargin margin, int fences, Request vr) {

}
