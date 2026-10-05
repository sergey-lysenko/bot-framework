package works.lysenko.util.prop.logs;

import static works.lysenko.util.spec.PropEnum._LOGS_TABLE_WIDTH;

/**
 * Represents a class to manage table properties for the application.
 * Provides the width of the table.
 */
@SuppressWarnings({"MissingJavadoc", "NonFinalStaticVariableUsedInClassInitialization"})
public record Table() {

    public static final Integer width = _LOGS_TABLE_WIDTH.get();

}
