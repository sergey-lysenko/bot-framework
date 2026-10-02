package works.lysenko.base.properties;

import works.lysenko.Base;
import works.lysenko.base.properties.renderer.Routines;
import works.lysenko.util.data.records.PropertiesMeta;

import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static works.lysenko.base.properties.renderer.Routines.validate;
import static works.lysenko.base.properties.renderer.Sections.renderAppliedConfiguration;
import static works.lysenko.base.properties.renderer.Sections.renderDefaultValues;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Numbers.THREE;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._NUMBR_;

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
        renderRecommendedCycles(maxRecordLength);
        validate(meta, changed);
    }

    /**
     * Renders recommended execution cycles count if root scenario is configured.
     *
     * @param maxRecordLength the maximum length of a record, used for formatting the output
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void renderRecommendedCycles(final int maxRecordLength) {

        if (isNotNull(Base.core)) {
            final int recommended = Base.core.getActiveScenarioPaths();
            if (0 < recommended) {
                System.out.println(s(_DASH_).repeat(maxRecordLength));
                System.out.println(b(gray(_NUMBR_), "Recommended cycles to cover all", s1(recommended, "leaf") + ":", yb(s(recommended))));
            }
        }
    }

}
