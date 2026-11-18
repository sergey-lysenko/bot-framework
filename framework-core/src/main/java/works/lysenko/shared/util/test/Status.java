package works.lysenko.shared.util.test;

import works.lysenko.util.apis.core.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Status reporter implementing the _Status interface for the test engine.
 */
public class Status implements _Status {

    /**
     * Retrieves the current status string.
     *
     * @return the status string representation
     */
    @Override
    public final String get() {

        return b(EMPTY);
    }
}
