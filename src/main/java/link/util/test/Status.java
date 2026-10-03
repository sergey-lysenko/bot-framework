package link.util.test;

import works.lysenko.util.apis.core.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Status reporter implementing the _Status interface for the test engine.
 */
public class Status implements _Status {

    @Override
    public final String get() {

        return b(EMPTY);
    }
}
