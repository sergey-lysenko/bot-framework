package works.lysenko.util.prop.core;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.joinWith;
import static works.lysenko.util.data.enums.EventType.FAILURE;
import static works.lysenko.util.data.enums.EventType.NOTICE;
import static works.lysenko.util.data.enums.EventType.SEVERE;
import static works.lysenko.util.data.enums.EventType.WARNING;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_CHECK;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_DELTA;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_DEPTH;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_EXCEPTIONS_PERMANENT;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_EXCEPTIONS_TEMPORARY;
import static works.lysenko.util.prop.PropEnum._LOGS_LINES_SIMILARITY_LIMIT;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Symbols.DATA_POINT;

/**
 * Configuration for checking and limiting similar/identical log lines.
 * <p>
 * Repeated similar log output is often a symptom of non-optimal implementation
 * or infinite loops.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Similarity() {

    /**
     * Indicates whether log lines similarity checking is enabled.
     */
    public static final boolean check = _LOGS_LINES_SIMILARITY_CHECK.get();

    /**
     * Maximum character distance (delta) threshold to consider two log lines similar.
     */
    public static final int delta = _LOGS_LINES_SIMILARITY_DELTA.get();

    /**
     * Number of previous log lines to inspect when checking for similarity.
     */
    public static final int depth = _LOGS_LINES_SIMILARITY_DEPTH.get();

    /**
     * Maximum number of permitted similar log line occurrences before a warning is triggered.
     */
    public static final int limit = _LOGS_LINES_SIMILARITY_LIMIT.get();

    private static final String INTERNAL_EXCEPTIONS = joinWith(s(L0), _RUNS_, DATA_POINT,
            WARNING.getString(), NOTICE.getString(), FAILURE.getString(), SEVERE.getString());

    /**
     * List of log line patterns exempt from similarity checking.
     */
    public static final List<String> exceptions = List.of((b(L0, INTERNAL_EXCEPTIONS,
            _LOGS_LINES_SIMILARITY_EXCEPTIONS_TEMPORARY.get(), _LOGS_LINES_SIMILARITY_EXCEPTIONS_PERMANENT.get())).split(l0));
}
