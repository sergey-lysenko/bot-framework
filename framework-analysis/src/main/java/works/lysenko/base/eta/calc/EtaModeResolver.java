package works.lysenko.base.eta.calc;

import works.lysenko.util.prop.PropEnum;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.parameters;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Shared runtime configuration and execution state resolvers for ETA and leaf-completion tracking.
 */
public record EtaModeResolver() {

    /**
     * Checks whether exhaustive mode is currently enabled via runtime parameters or test properties.
     *
     * @return {@code true} if exhaustive mode is active
     */
    public static boolean isExhaustiveMode() {
        if (isNotNull(parameters)) {
            return parameters.isExhaustive();
        }
        final Integer count = PropEnum._TEST_EXHAUSTIVE_PASSES.get();
        return Boolean.TRUE.equals(PropEnum._TEST_EXHAUSTIVE.get()) || (isNotNull(count) && count > 1);
    }

    /**
     * Resolves the target number of passes required per leaf scenario in exhaustive mode.
     *
     * @return positive target pass count per leaf (at least {@code 1})
     */
    public static int resolveExhaustivePasses() {
        if (isNotNull(parameters)) {
            return Math.max(1, parameters.getExhaustivePasses());
        }
        final Integer prop = PropEnum._TEST_EXHAUSTIVE_PASSES.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }

    /**
     * Checks whether multiple passes per leaf scenario are configured.
     *
     * @return {@code true} when target pass count exceeds {@code 1}
     */
    public static boolean isMultiplePassesPerLeaf() {
        return resolveExhaustivePasses() > 1;
    }

    /**
     * Resolves the current 1-based test cycle number from core/repeater state.
     *
     * @return current test cycle number, or {@code 0} if unavailable
     */
    public static int resolveCurrentTest() {
        if (isNull(core) || isNull(core.getTest()) || isNull(core.getTest().repeater())) {
            return 0;
        }
        final Integer current = core.getCurrentTestNumber();
        return isNotNull(current) ? Math.max(0, current) : 0;
    }

    /**
     * Resolves the configured total test cycles count from core/repeater state.
     *
     * @return configured total test cycles count, or {@code 0} if unbounded or unavailable
     */
    public static int resolveTotalTests() {
        if (isNull(core) || isNull(core.getTest()) || isNull(core.getTest().repeater())) {
            return 0;
        }
        final Integer total = core.getTotalTests();
        return isNotNull(total) ? Math.max(0, total) : 0;
    }

    /**
     * Resolves the number of completed test cycles from the active repeater history.
     *
     * @return completed test cycle count, or {@code 0} if unavailable
     */
    public static int resolveCompletedTests() {
        if (isNull(core) || isNull(core.getTest()) || isNull(core.getTest().repeater())) {
            return 0;
        }
        final var history = core.getTest().repeater().getHistory();
        return (null != history) ? history.size() : 0;
    }
}
