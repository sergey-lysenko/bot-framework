package link.util.test;

import works.lysenko.util.spec.*;

import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Postflight lifecycle hook responsible for executing post-test summary and cleanup actions.
 */
public class Postflight implements Runnable {

    @Override
    public final void run() {

        section(getClass().getSimpleName());
        log(Level.none, bb(b(getClass().getSimpleName(), DONE)), false);
    }
}
