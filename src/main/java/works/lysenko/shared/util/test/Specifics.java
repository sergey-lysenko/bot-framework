package works.lysenko.shared.util.test;

import works.lysenko.tree.base.*;
import works.lysenko.util.apis.exception.checked.*;

/**
 * Exceptional handler for generic test execution.
 */
public class Specifics extends Exceptional {

    @Override
    public final void action() throws SafeguardException {

        if (unresolved()) super.action();
    }
}
