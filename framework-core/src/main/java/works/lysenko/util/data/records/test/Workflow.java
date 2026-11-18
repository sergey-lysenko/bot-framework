package works.lysenko.util.data.records.test;

import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.core._Status;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.Collection;

/**
 * Represents a Workflow that encapsulates the various parts necessary for the
 * execution of a process, including preflight, status, exceptional scenarios,
 * postflight, and additional properties.
 *
 * @param preflight   the class of the preflight Runnable to be executed before the main workflow
 * @param status      the class implementing _Status to represent the current status of the workflow
 * @param exceptional the class implementing _Scenario to handle exceptional scenarios in the workflow
 * @param postflight  the class of the postflight Runnable to be executed after the main workflow
 * @param additional  a collection of classes implementing _PropEnum, representing additional
 *                    configurable properties for the workflow
 */
public record Workflow(
        Class<? extends Runnable> preflight,
        Class<? extends _Status> status,
        Class<? extends _Scenario> exceptional,
        Class<? extends Runnable> postflight,
        Collection<Class<? extends _PropEnum>> additional) {
}
