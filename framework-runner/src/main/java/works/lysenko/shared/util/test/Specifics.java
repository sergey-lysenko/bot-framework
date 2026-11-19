package works.lysenko.shared.util.test;

import works.lysenko.tree.base.*;
import works.lysenko.util.apis.exception.checked.*;

/**
 * Exceptional handler for generic test execution.
 */
public class Specifics extends Exceptional {

    /**
     * Performs exceptional execution actions when unresolved issues exist.
     *
     * @throws SafeguardException if a safeguard assertion fails
     */
    @Override
    public final void action() throws SafeguardException {

        if (unresolved()) super.action();
    }
}
