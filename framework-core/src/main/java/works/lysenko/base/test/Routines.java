package works.lysenko.base.test;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.base.Test;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.exception.unchecked.ScenarioRuntimeException;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.records.test.Workflow;
import works.lysenko.util.prop.PropEnum;

import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.parameters;
import static works.lysenko.base.core.Routines.isTestPropertiesEmpty;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.____.DONE;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ExitCode.SAFEGUARD_FAILED;
import static works.lysenko.util.data.enums.ExitCode.SCENARIO_FAILED;
import static works.lysenko.util.data.enums.ExitCode.UNHANDLED_EXCEPTION;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.CLOSING_TEST_SERVICE;
import static works.lysenko.util.lang.N.NO_TESTS_WERE_PERFORMED;
import static works.lysenko.util.lang.T.TEST_PROPERTIES_ARE___;
import static works.lysenko.util.lang.word.E.EXECUTING;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.spec.Level.none;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * The Routines class provides static utility methods for preparing tests,
 * managing services, running tests, and verifying properties.
 * This class acts as a utility and does not require instantiation.
 */
public record Routines() {

    /**
     * Prepares test based on the total number of test repetitions
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static void build() {

        final Integer total = core.getTotalTests();
        if (isNotNull(total) && ZERO == total) {
            logEvent(S2, NO_TESTS_WERE_PERFORMED);
            return;
        }

        final String numberOfRepetitions = yb((null == total) ? TESTS : s1(total, TEST));
        final String testDescription = parameters.testDescription();
        final String allLeafsMode = (isNotNull(parameters) && parameters.isAllLeafs())
                ? "in ALL_LEAFS mode" //NON-NLS
                : StringUtils.EMPTY;

        log(none, b(false, c(EXECUTING), numberOfRepetitions, OF, testDescription, allLeafsMode), true);
    }

    /**
     * Closes the service if it is not null.
     * This method logs the closing of the service, closes the service, and logs that the service has been closed.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void haltService() {

        final var service = exec.service();
        if (isNotNull(service)) {
            log(CLOSING_TEST_SERVICE);
            service.close();
            log(b(DOTS, DONE));
        }
    }

    /**
     * Determines whether all-leafs execution mode is active, either via initialised {@code parameters}
     * or via {@link PropEnum#_TEST_ALL_LEAFS} / {@link PropEnum#_TEST_ALL_LEAFS_EXECUTIONS}.
     *
     * @return {@code true} if all-leafs execution mode is enabled, {@code false} otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean isAllLeafsMode() {

        if (isNotNull(parameters)) return parameters.isAllLeafs();
        return Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()) || isCountAllLeafs();
    }

    /**
     * Executes the provided tests.
     *
     * @param test The _Test object representing the tests to be executed.
     */
    public static void run(final _Test test) {

        try {
            test.execute();
        } catch (final SafeguardException e) {
            test.processCheckedException(e, SAFEGUARD_FAILED);
        } catch (final ScenarioRuntimeException e) {
            test.processCheckedException(e, SCENARIO_FAILED);
        } catch (final Exception e) {
            test.processUncheckedException(e, UNHANDLED_EXCEPTION);
        } finally {
            test.complete();
        }
    }

    /**
     * Creates and executes a new Test object based on the provided Workflow object.
     *
     * @param workflow the Workflow object containing the necessary class associations for the tests
     */
    public static void run(final Workflow workflow) {

        run(new Test(workflow));
    }

    /**
     * Checks if the test properties are empty.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void verifyProperties() {

        if (isTestPropertiesEmpty()) logEvent(S1, TEST_PROPERTIES_ARE___);
    }

    private static boolean isCountAllLeafs() {

        final Integer count = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return isNotNull(count) && ONE < count;
    }
}
