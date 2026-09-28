package works.lysenko.util.prop.logs;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.PropEnum._LOGS_TO_READ;

/**
 * A1 class to manage log properties for the application.
 * <p>
 * Provides information about the logs to be read and a limit on the number of characters
 * for each log line.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass", "AutoBoxing",
        "MissingJavadoc",
        "PublicStaticCollectionField", "StaticCollection"})
public record To() {

    private static final String logsToRead = _LOGS_TO_READ.get();
    public static final Set<String> read = isNull(logsToRead) ? null :
            Arrays.stream(logsToRead.split(s(L1))).map(String::trim).collect(Collectors.toSet());

}
