package works.lysenko.base.eta.correction;

import works.lysenko.Base;
import works.lysenko.base.eta.model.EtaMetadata;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.prop.core.Start;
import works.lysenko.util.prop.core.Test;
import works.lysenko.util.prop.core.Time;
import works.lysenko.util.prop.tree.Scenario;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Resolves the active {@link EtaMetadata} configuration fingerprint from runtime parameters and property micro-caches.
 */
public record MetadataResolver() {

    /**
     * Captures the current run's configuration fingerprint for historical calibration matching.
     *
     * @return current {@link EtaMetadata}
     */
    public static EtaMetadata resolveCurrent() {
        final String test = (isNotNull(Base.parameters) && isNotNull(Base.parameters.getTest()))
                ? Base.parameters.getTest()
                : nonNullOr(Scenario.root, "default");
        final String tests = resolveTests();
        final int allLeafsExecutions = resolveAllLeafsExecutions();
        final boolean allLeafs = resolveAllLeafs(allLeafsExecutions);
        final String root = nonNullOr(Scenario.root, "");
        final String app = nonNullOr(Start.app, "");
        final int pauseLength = Time.pauseLength;
        final String platform = nonNullOr(Start.forcedPlatform, "web");

        return new EtaMetadata(test, tests, allLeafs, allLeafsExecutions, root, app, pauseLength, platform);
    }

    private static String resolveTests() {
        if (isNotNull(Base.parameters)) {
            final String val = Base.parameters.getTests();
            if (isNotNull(val) && !val.isEmpty()) {
                return val;
            }
        }
        return (null != Test.tests) ? Test.tests.toString() : "";
    }

    private static int resolveAllLeafsExecutions() {
        if (isNotNull(Base.parameters)) {
            final String val = Base.parameters.getValue(ExecutionParameter.ALL_LEAFS_EXECUTIONS);
            if (isNotNull(val) && !val.isEmpty()) {
                try {
                    return Math.max(1, Integer.parseInt(val));
                } catch (final NumberFormatException _) {
                }
            }
        }
        return Math.max(1, Test.allLeafsExecutions);
    }

    private static boolean resolveAllLeafs(final int allLeafsExecutions) {
        if (allLeafsExecutions > 1) return true;
        if (isNotNull(Base.parameters)) {
            final String val = Base.parameters.getValue(ExecutionParameter.ALL_LEAFS);
            if (isNotNull(val) && !val.isEmpty()) {
                return Boolean.parseBoolean(val);
            }
        }
        return Test.allLeafs;
    }

    private static String nonNullOr(final Object val, final String fallback) {
        return (null != val) ? val.toString() : fallback;
    }
}
