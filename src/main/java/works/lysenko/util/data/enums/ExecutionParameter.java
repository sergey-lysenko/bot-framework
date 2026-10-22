package works.lysenko.util.data.enums;


import works.lysenko.util.apis.execution._Default;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.lang.word.F.*;

/**
 * Enum class representing different execution parameters.
 * Each parameter has a default value that can be accessed
 * using the `def()` method.
 * The enum class also provides a static method `byName()`
 * to retrieve the enum value based on the parameter name.
 * <p>
 * The enum class implements the `ProvidesDefault` interface,
 * which requires implementing the `def()` method to return
 * the default value for each parameter.
 * <p>
 * The enum class also includes a private constructor that
 * accepts a default value and initializes the `def` field
 * with it. It also provides a `def()` method to retrieve the
 * default value of each enum value.
 */
@SuppressWarnings({"unused", "EnumClass", "MissingJavadoc", "CallToSuspiciousStringMethod"})
public enum ExecutionParameter implements _Default {

    ALL_LEAFS(FALSE),
    ALL_LEAFS_EXECUTIONS(EMPTY),
    COMPLETION_WEIGHT(EMPTY),
    DEVICE(EMPTY),
    DOMAIN(EMPTY),
    FORBID_OVEREXECUTION(FALSE),
    HEADLESS(FALSE),
    HIDE_OFF_BRANCHES(FALSE),
    ORIGIN(EMPTY),
    PLATFORM(Platform.CHROME.getString()),
    POOL(EMPTY),
    RESILIENT_MODE(FALSE),
    SUITE(EMPTY),
    TAG(EMPTY),
    TESTS(EMPTY),
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

    public final String def() {

        return def;
    }
}
