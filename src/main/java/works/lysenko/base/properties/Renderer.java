package works.lysenko.base.properties;

import works.lysenko.base.properties.renderer.Routines;
import works.lysenko.util.data.records.PropertiesMeta;

import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static works.lysenko.base.properties.renderer.Routines.validate;
import static works.lysenko.base.properties.renderer.Sections.renderAppliedConfiguration;
import static works.lysenko.base.properties.renderer.Sections.renderDefaultValues;
import static works.lysenko.util.spec.Numbers.THREE;

/**
 * The Renderer class is responsible for rendering sorted and default test properties in a formatted manner.
 */
public record Renderer() {

    /**
     * Outputs and validates the applied and default configurations based on the provided sorted properties and metadata.
     * This method processes, formats, and validates the configuration by rendering applied properties,
     * default values, and ensuring correctness against the metadata constraints.
     *
     * @param sorted a map of properties sorted by their keys, containing the key-value pairs to be processed
     * @param meta   the metadata object specifying default properties and valid class references
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void outputAndValidate(final Map<String, String> sorted, final PropertiesMeta meta) {

        final int maxRecordLength = Routines.findLongestKeyValue(sorted) + THREE;
        final int draft = max(Routines.MIN_LENGTH, maxRecordLength);
        final int length = (1 == draft % 2) ? draft : draft + 1;

        final List<String> changed = renderAppliedConfiguration(sorted, meta, length);
        renderDefaultValues(meta.defaults(), maxRecordLength, changed, length);
        validate(meta, changed);
    }

}
