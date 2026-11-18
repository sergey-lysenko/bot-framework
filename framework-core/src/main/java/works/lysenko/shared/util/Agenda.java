package works.lysenko.shared.util;

import works.lysenko.shared.util.spec.PropEnum;
import works.lysenko.shared.util.test.Postflight;
import works.lysenko.shared.util.test.Preflight;
import works.lysenko.shared.util.test.Specifics;
import works.lysenko.shared.util.test.Status;
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
