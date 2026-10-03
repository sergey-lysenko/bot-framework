package link.util;

import link.util.spec.PropEnum;
import link.util.test.Postflight;
import link.util.test.Preflight;
import link.util.test.Specifics;
import link.util.test.Status;
import works.lysenko.util.data.records.test.*;

import java.util.List;

/**
 * The Agenda record holds static references to various classes that implement specific actions
 * defined in the application workflow, including preflight, status reporting, exceptional handling,
 * and postflight actions.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Agenda() {

    /**
     * Static final reference to the Workflow records, holding classes for the different phases
     * of the application workflow: Preflight, Status, Specifics, and Postflight.
     */
    public static final Workflow workflow
            = new Workflow(
            Preflight.class,
            Status.class,
            Specifics.class,
            Postflight.class,
            List.of(PropEnum.class));
}
