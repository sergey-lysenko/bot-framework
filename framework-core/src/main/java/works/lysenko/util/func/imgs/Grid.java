package works.lysenko.util.func.imgs;

import works.lysenko.util.apis.grid.v._Validation;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.sets.GridValidationResults;
import works.lysenko.util.grid.Probe;
import works.lysenko.util.grid.record.Request;

import static works.lysenko.util.data.enums.Severity.S0;

/**
 * The BackgroundValidator class is responsible for validating a pixel grid in a given Request object.
 */
public record Grid() {

    /**
     * Checks if the pixel grid within the given Request is valid.
     *
     * @param request the validation request containing the pixel grid information
     * @return the result of the grid validation as GridResult
     */
    public static GridValidationResults test(final Request request) {

        return test(request, S0);
    }

    /**
     * Verifies the grid in the given Request object with the specified tolerances and severity level.
     *
     * @param request  the Request object containing the pixel grid information to be verified
     * @param severity the severity level for validation issues
     * @return the results of the grid validation as GridValidationResults
     */
    public static GridValidationResults test(final Request request, final Severity severity) {

        final _Validation validation = Probe.create(request, severity);
        validation.perform();
        return validation.results();
    }
}
