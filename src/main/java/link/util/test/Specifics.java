package link.util.test;

import works.lysenko.tree.base.*;
import works.lysenko.util.apis.exception.checked.*;

/**
 * Exceptional handler for BiteHeist test execution.
 */
public class Specifics extends Exceptional {

    @Override
    public final void action() throws SafeguardException {

        if (unresolved()) super.action();
    }
}
