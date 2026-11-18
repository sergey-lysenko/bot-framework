package works.lysenko.base.parameters;

import works.lysenko.base.Parameters;
import works.lysenko.base.parameters.renderer.Routines;
import works.lysenko.util.data.records.ParametersMeta;

import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static works.lysenko.base.parameters.renderer.Routines.validate;
import static works.lysenko.base.parameters.renderer.Sections.renderAppliedParameters;
import static works.lysenko.base.parameters.renderer.Sections.renderDefaultValues;
import static works.lysenko.util.spec.Numbers.THREE;

/**
 * Renders sorted applied and default execution parameters in a formatted multi-column block.
 */
public record Renderer() {

    /**
     * Outputs and validates the applied and default execution parameters based on the provided sorted map and metadata.
     *
     * @param parameters the Parameters instance being rendered
     * @param sorted     sorted map of applied/redefined execution parameter key-value pairs
     * @param meta       metadata specifying default parameter values, unknown keys, and counts
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void outputAndValidate(final Parameters parameters,
                                         final Map<String, String> sorted,
                                         final ParametersMeta meta) {

        final int maxRecordLength = Routines.findLongestKeyValue(parameters, sorted, meta.defaults()) + THREE;
        final int draft = max(Routines.MIN_LENGTH, maxRecordLength);
        final int length = (1 == draft % 2) ? draft : draft + 1;

        final List<String> changed = renderAppliedParameters(parameters, sorted, meta, length);
        renderDefaultValues(meta.defaults(), maxRecordLength, length);
        validate(meta, changed);
    }
}
