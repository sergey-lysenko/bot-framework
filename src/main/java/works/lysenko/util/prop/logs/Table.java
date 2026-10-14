package works.lysenko.util.prop.logs;

import static works.lysenko.util.prop.PropEnum._LOGS_TABLE_WIDTH;

/**
 * Provides formatting configuration for tabular log outputs.
 */
public record Table() {

    /**
     * Width limit used for rendering formatted tables in logs.
     */
    public static final Integer width = _LOGS_TABLE_WIDTH.get();

}
