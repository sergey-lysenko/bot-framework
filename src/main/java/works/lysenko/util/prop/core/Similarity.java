package works.lysenko.util.prop.core;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.joinWith;
import static works.lysenko.util.data.enums.EventType.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.PropEnum.*;
import static works.lysenko.util.spec.Symbols.DATA_POINT;

/**
 * Similar/same lines of log output is a sigh of potential issue.
 * In most cases that is a sign of non-optimal implementation.
 * Also, this is one of the mechanisms to detect a dead loop.
 * <p>
 * This class includes static properties for checking and limiting the similarity of log lines.
 * These properties are fetched from a configuration source to ensure consistent behaviour
 * across the application.
 */
@SuppressWarnings({"MissingJavadoc", "StaticCollection", "StaticMethodOnlyUsedInOneClass"})
public record Similarity() {

    public static final boolean check = _LOG_LINES_SIMILARITY_CHECK.get();
    public static final int delta = _LOG_LINES_SIMILARITY_DELTA.get();
    public static final int depth = _LOG_LINES_SIMILARITY_DEPTH.get();
    public static final int limit = _LOG_LINES_SIMILARITY_LIMIT.get();
    private static final String INTERNAL_EXCEPTIONS = joinWith(s(L0), _RUNS_, DATA_POINT,
            WARNING.getString(), NOTICE.getString(), FAILURE.getString(), SEVERE.getString());
    public static final List<String> exceptions = List.of((b(L0, INTERNAL_EXCEPTIONS,
            _LOG_LINES_SIMILARITY_EXCEPTIONS_TEMPORARY.get(), _LOG_LINES_SIMILARITY_EXCEPTIONS_PERMANENT.get())).split(l0));
}
