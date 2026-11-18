package works.lysenko.util.prop.logs;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.prop.PropEnum._LOGS_TO_READ;

/**
 * Configuration for log types to be collected and read during execution.
 * <p>
 * Parses the configured log types (delimited by {@link works.lysenko.util.prop.data.Delimeters#L1})
 * into a set of log target names to be captured by the logger.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "AutoBoxing",
        "PublicStaticCollectionField", "StaticCollection"})
public record To() {

    /**
     * Raw delimited string containing the log types configured to be read.
     */
    private static final String logsToRead = _LOGS_TO_READ.get();

    /**
     * Set of log type identifiers to read and capture during execution,
     * or {@code null} if no log types were specified in properties.
     */
    public static final Set<String> read = isNull(logsToRead) ? null :
            Arrays.stream(logsToRead.split(s(L1))).map(String::trim).collect(Collectors.toSet());

}
