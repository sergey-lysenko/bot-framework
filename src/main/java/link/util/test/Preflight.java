package link.util.test;

import works.lysenko.util.spec.*;

import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.*;

/**
 * Preflight lifecycle hook responsible for preparing data and state before tests.
 */
public class Preflight implements Runnable {

    @Override
    public final void run() {

        section(getClass().getSimpleName());
        log(Level.none, bb(b(getClass().getSimpleName(), DONE)), false);
    }
}
