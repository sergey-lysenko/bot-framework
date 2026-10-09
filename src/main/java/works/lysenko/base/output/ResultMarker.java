package works.lysenko.base.output;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.spec.Level;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.log;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.____.EXIT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Null.sn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenAndCodeSnapshot;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.P.PASSED;
import static works.lysenko.util.lang.word.R.RESULTS;
import static works.lysenko.util.lang.word.S.SUCCESSFULLY;
import static works.lysenko.util.spec.Symbols.*;

@SuppressWarnings({"NestedMethodCall", "FeatureEnvy", "AutoBoxing"})
record ResultMarker() {

    /**
     * Generates a failure message using the core's stoppage reason or failure summary.
     *
     * @return The generated failure message.
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod"})
    private static String generateFailureMessage() {

        if (core.getStopFlag()) {
            final String stopReason = core.getStopReason();
            final String message;
            final _Scenario scenario = findOriginatingScenario();
            if (null != stopReason && !stopReason.isBlank()) {
                String clean = stopReason.trim().replace(_LFD_, _BULLT_);
                if (!clean.startsWith("[FAILURE]") && !clean.startsWith("[ERROR]")) {
                    clean = b(s("[FAILURE]"), clean);
                }
                if (isNotNull(scenario)) {
                    final String fqn = scenario.getName();
                    if (!clean.contains(fqn)) {
                        clean = b(clean, IN, q(fqn));
                    }
                }
                message = clean;
            } else {
                if (isNotNull(scenario)) {
                    message = b(s("[FAILURE]"), TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY, IN, q(scenario.getName()));
                } else {
                    message = b(s("[FAILURE]"), TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY);
                }
            }
            return generateMessage(message);
        }

        final int failedCount = countFailedScenarios();
        if (failedCount > 0) {
            return generateMessage(b(s("[FAILURE]"), String.format("Execution finished with %d failed scenario%s",
                    failedCount, (1 == failedCount) ? "" : "s")));
        }

        if (core.getResults().getFailures().isEmpty()) return null;
        final String s = core.getResults().getFailures().get(0);
        return generateMessage(s.trim().replace(_LFD_, _BULLT_));
    }

    private static _Scenario findOriginatingScenario() {

        if (isNotNull(core)) {
            if (isNotNull(core.getStopScenario())) {
                return core.getStopScenario();
            }
            if (isNotNull(exec) && isNotNull(exec.scenarios()) && isNotNull(exec.scenarios().current())) {
                return exec.scenarios().current();
            }
            if (isNotNull(core.getResults())) {
                final Map<_Scenario, _Result> sorted = core.getResults().getSorted();
                if (isNotNull(sorted)) {
                    for (final Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
                        final _Scenario scen = entry.getKey();
                        final _Result res = entry.getValue();
                        if ((isNotNull(scen) && scen.hasFailed()) || (isNotNull(res) && res.status() == ExecutionStatus.FAILED)) {
                            return scen;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int countFailedScenarios() {

        int count = 0;
        if (isNotNull(core) && isNotNull(core.getResults())) {
            final Map<_Scenario, _Result> sorted = core.getResults().getSorted();
            if (isNotNull(sorted)) {
                for (final Map.Entry<_Scenario, _Result> entry : sorted.entrySet()) {
                    final _Scenario scen = entry.getKey();
                    final _Result res = entry.getValue();
                    if ((isNotNull(scen) && scen.hasFailed()) || (isNotNull(res) && res.status() == ExecutionStatus.FAILED)) {
                        count++;
                    }
                }
            }
            if (0 == count && !core.getResults().getFailures().isEmpty()) {
                count = core.getResults().getFailures().size();
            }
        }
        return count;
    }

    /**
     * Generates a message by enclosing the given message with symbols.
     *
     * @param message The message to be enclosed.
     * @return The generated message.
     */
    private static String generateMessage(final String message) {

        return s(e(s(_EQUAL_)), message, e(s(_EQUAL_)));
    }

    /**
     * Generates a plaque with the given message using the specified foreground and background colours.
     *
     * @param message    The message to be displayed on the plaque.
     * @param foreground The foreground colour of the plaque.
     * @param background The background colour of the plaque.
     */
    private static void plaque(final String message, final Ansi foreground, final Ansi background) {

        log(Level.none, ansi(ansi(SPACE.repeat(message.length()), background), foreground), false);
        log(Level.none, ansi(ansi(message, background), foreground), false);
        log(Level.none, ansi(ansi(SPACE.repeat(message.length()), background), foreground), false);
    }

    /**
     * Runs the specified method and generates a result message based on success or failure.
     * It displays the result message on a plaque with specified foreground and background colours.
     * It also logs an empty line before displaying the result message.
     */
    static void run() {

        exec.logEmptyLine();

        final String resultMessage;
        final Ansi foreground;
        final Ansi background;

        if (core.getResults().getSorted().isEmpty()) {
            resultMessage = generateMessage(b(c(NO), TEST, RESULTS));
            foreground = WHITE_BOLD_BRIGHT;
            background = BLACK_BACKGROUND_BRIGHT;
        } else if (!core.getStopFlag() && core.getResults().getFailures().isEmpty()) {
            resultMessage = generateMessage(b(c(EXECUTION), PASSED, SUCCESSFULLY));
            foreground = BLACK;
            background = GREEN_BACKGROUND;
        } else {
            resultMessage = generateFailureMessage();
            foreground = WHITE_BOLD_BRIGHT;
            background = RED_BACKGROUND;
            makeScreenAndCodeSnapshot(true, e(UND_SCR, u(EXIT)));
        }
        plaque(sn(resultMessage), foreground, background);
    }
}
