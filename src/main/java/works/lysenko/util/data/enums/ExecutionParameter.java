package works.lysenko.util.data.enums;

import works.lysenko.util.apis.execution._Default;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.lang.word.F.FALSE;

/**
 * Enum class representing different execution parameters.
 * Each parameter has a default value that can be accessed
 * using the {@code def()} method.
 * The enum class also provides a static method {@code byName()}
 * to retrieve the enum value based on the parameter name.
 * <p>
 * The enum class implements the {@code _Default} interface,
 * which requires implementing the {@code def()} method to return
 * the default value for each parameter.
 */
@SuppressWarnings({"unused", "EnumClass", "CallToSuspiciousStringMethod"})
public enum ExecutionParameter implements _Default {

    /**
     * Parameter indicating whether all leafs should be executed.
     */
    ALL_LEAFS(FALSE),

    /**
     * Parameter specifying all leaf executions configuration.
     */
    ALL_LEAFS_EXECUTIONS(EMPTY),

    /**
     * Parameter for completion weight.
     */
    COMPLETION_WEIGHT(EMPTY),

    /**
     * Target device identifier.
     */
    DEVICE(EMPTY),

    /**
     * Domain name for execution.
     */
    DOMAIN(EMPTY),

    /**
     * Parameter to forbid over-execution.
     */
    FORBID_OVEREXECUTION(FALSE),

    /**
     * Parameter specifying headless mode execution.
     */
    HEADLESS(FALSE),

    /**
     * Parameter to hide off branches.
     */
    HIDE_OFF_BRANCHES(FALSE),

    /**
     * Legacy suite name.
     */
    OLD_SUITE(EMPTY),

    /**
     * Origin parameter.
     */
    ORIGIN(EMPTY),

    /**
     * Target platform parameter.
     */
    PLATFORM(Platform.CHROME.getString()),

    /**
     * Execution pool parameter.
     */
    POOL(EMPTY),

    /**
     * Parameter enabling resilient mode.
     */
    RESILIENT_MODE(FALSE),

    /**
     * Suite name parameter.
     */
    SUITE(EMPTY),

    /**
     * Tag filter parameter.
     */
    TAG(EMPTY),

    /**
     * Tests specification parameter.
     */
    TESTS(EMPTY),

    /**
     * Parameter enabling extension traversal.
     */
    TRAVERSE_EXTENSIONS(FALSE);

    private final String def;

    ExecutionParameter(final String def) {

        this.def = def;
    }

    /**
     * Retrieves the ExecutionParameter enum value based on the parameter name.
     *
     * @param s the parameter name to search for
     * @return the ExecutionParameter enum value that matches the parameter name or null if no match is found
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static ExecutionParameter byName(final String s) {

        if (null == s) return null;
        if ("TEST".equalsIgnoreCase(s)) return SUITE;
        for (final ExecutionParameter e : values())
            if (e.name().equalsIgnoreCase(s)) return e;
        return null;
    }

    @Override
    public final String def() {

        return def;
    }
}
