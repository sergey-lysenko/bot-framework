package works.lysenko.base.test;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import works.lysenko.base.core.Routines;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._ExceptionsHandler;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Stat;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.prop.core.Test;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static works.lysenko.util.lang.word.E.ENTRY;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.math.NumberUtils.min;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.LOW;
import static works.lysenko.util.chrs.___.TOO;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.sn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CLOSING;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.H.HISTORY;
import static works.lysenko.util.lang.word.I.INDEX;
import static works.lysenko.util.lang.word.L.LOCATED;
import static works.lysenko.util.lang.word.O.ORPHANED;
import static works.lysenko.util.lang.word.P.PERSISTING;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.T.TOOK;
import static works.lysenko.util.prop.core.Safeguard.historyDepth;
import static works.lysenko.util.spec.PropEnum._TESTS;
import static works.lysenko.util.spec.Symbols._BULLT_;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * The Repeater class represents a test manager that keeps track of the test history, test times,
 * and test scenarios. It provides methods for processing the test history, test time, closing the current test,
 * adding history entries, and managing the current test.
 */
@SuppressWarnings({"FieldHasSetterButNoGetter", "MethodCallInLoopCondition", "InstanceVariableMayNotBeInitialized"})
public class Repeater implements _Repeater {

    private static final String GREEN_CHECKMARK = Ansi.gb("✓");
    private static final String RED_QUESTION_MARK = Ansi.rb("?");
    private final _ExceptionsHandler handler;
    private final _Exec executor;
    private final _Stat statistics;
    private List<String> history = new ArrayList<>(1);
    private List<_Scenario> scenarios = new ArrayList<>(1);
    private Long startedAt = null;
    private boolean done = false;
    private CircularFifoQueue<Long> safeguard;

    /**
     * Creates a new instance of the Repeater class.
     *
     * @param handler    the object that handles exceptions
     * @param executor   the object that executes scenarios
     * @param statistics the object that collects statistics
     */
    public Repeater(final _ExceptionsHandler handler, final _Exec executor, final _Stat statistics) {

        this.handler = handler;
        this.executor = executor;
        this.statistics = statistics;
    }

    /**
     * Returns the readable form of the given raw string by replacing certain characters.
     * It replaces the string "NULL" with the provided nullReplacement value.
     * It also removes the occurrences of the string "[Start.location]" from the raw string.
     *
     * @param raw             The raw string to be transformed.
     * @param nullReplacement The replacement value for the string "NULL".
     * @return The transformed readable form of the raw string.
     */
    private static String replace(final String raw, final CharSequence nullReplacement) {

        return raw.replace(NULL, nullReplacement).replace(Scenario.root, StringUtils.EMPTY);
    }

    public final void addToHistory(final _Scenario scenario) throws SafeguardException {

        final String info = scenario.info();
        final boolean isInfo = (null == info) || (info.isEmpty());
        final String entry = isInfo ? scenario.getSimpleName() : b(scenario.getSimpleName(), info);
        final int index = scenarios.indexOf(scenario);
        logTrace(b(c(SCENARIO), q(scenario.getName()), b(LOCATED, IN, HISTORY, AT, INDEX), s(index)));
        addHistory(entry, index);
        show();
        validate();
    }

    public final void addToScenarios(final _Scenario scenario) {

        scenarios.add(scenario);
        final int size = scenarios.size();
        if (isNotNull(core.getDashboard())) core.getDashboard().setBreadcrumb(scenarios);
        logDebug(b(s(size), replace(scenarios.toString(), RED_QUESTION_MARK)));
    }

    public final void close() {

        final String test = sn(StringUtils.EMPTY, getCurrent());
        close(test);
        time();
        history();
        timeSafeguard();
        done = core.getStopFlag();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Integer getCurrent() {

        if (isNull(startedAt)) return null;
        if (isNull(exec) || done) return null;
        final int current = statistics.history().size() + 1;
        if (isNull(getTotalTests())) return current;
        return (current > getTotalTests()) ? null : current;
    }

    public final List<List<String>> getHistory() {

        return statistics.history();
    }

    @Override
    public final List<List<String>> getSummary() {

        return statistics.summary();
    }

    @Override
    public final Integer getTotalTests() {

        return _TESTS.get();
    }

    public final void run() throws SafeguardException {

        startedAt = Routines.msSinceStart();
        title();
        try {
            executor.exec();
        } catch (final RuntimeException exception) {
            handler.handle(exception);
        } finally {
            info();
            close();
        }
    }

    /**
     * Adds a history entry at the specified index.
     * If the history list does not have enough elements, null elements are added until the specified index is reached.
     * The previous entry at the specified index is replaced with the new entry.
     *
     * @param entry The history entry to add.
     * @param at    The index at which to add the history entry.
     */
    private void addHistory(final String entry, final int at) {

        while (at >= history.size()) history.add(null);
        logTrace(b(a(AT, at, _COMMA_), a(ENTRY, entry)));
        history.set(at, entry);
        scenarios.set(at, null); // remove source reference for case of several same scenario executions
    }

    /**
     * Processes a test by adding history entries for each scenario in the test, including orphaned due to interruptions.
     *
     * @param test the test string to process
     */
    private void close(final String test) {

        log(b(false, c(CLOSING), TEST, test, DOTS));
        for (final _Scenario scenario : scenarios) {
            if (isNotNull(scenario)) {
                final String info = scenario.info();
                final String entry = ((null == info) || (info.isEmpty())) ? scenario.getSimpleName() :
                        b(scenario.getSimpleName(), info);
                final int index = scenarios.indexOf(scenario);
                logDebug(b(c(ORPHANED), q(scenario.getName()), TO, HISTORY, AT, INDEX, s(index)));
                addHistory(entry, index);
                show();
            }
        }
    }

    /**
     * Retrieves the circular buffer containing the time safeguard.
     * <p>
     * If the time safeguard has not been initialized, it creates a new instance of
     * CircularFifoQueue with the number of tests specified by Test.timeSafeguard.tests().
     *
     * @return The circular buffer containing the time safeguard.
     */
    private CircularFifoQueue<Long> getSafeguard() {

        if (isNull(safeguard)) safeguard = new CircularFifoQueue<>(Test.timeSafeguard.tests());
        return safeguard;
    }

    /**
     * Retrieves the title for the current test.
     *
     * @return the title string for the current test
     */
    @SuppressWarnings("NestedConditionalExpression")
    private String getTitle() {

        return b(q(parameters.getTest()), null == getCurrent() ? StringUtils.EMPTY : b(s(_BULLT_), b(c(TEST), s(getCurrent(),
                null == core.getTotalTests() ? StringUtils.EMPTY : b(s(_SLASH_, core.getTotalTests()),
                        percentString(getCurrent() - 1, core.getTotalTests()))))));
    }

    /**
     * Processes the test history by adding a new test history entry, resetting the current test history, and test scenarios.
     */
    private void history() {

        if (history.isEmpty()) history.add(c(STOPPED));
        logDebug(b(c(PERSISTING), c(TEST), HISTORY, OF, yb(s1(history.size(), ELEMENT))));
        statistics.history().add(history);
        history = new ArrayList<>(1);
        scenarios = new ArrayList<>(1);
    }

    /**
     * Logs the information about the current test execution.
     * <p>
     * This method logs details about the current test status and a completion mark
     * (e.g., DONE) at a specific log level. It utilizes methods from the underlying
     * framework, including getting the status of the current test and formatting the
     * title of the test execution.
     */
    private void info() {

        log(Level.none, core.getTest().getStatus(), false);
        log(Level.none, b(bb(getTitle()), DONE), false);
    }

    /**
     * Shows the information about the current state of the Repeater object by logging the details.
     */
    private void show() {

        if (isTrace()) {
            final String length = s(scenarios.size());
            final String scenarios = replace(this.scenarios.toString(), GREEN_CHECKMARK);
            final String history = replace(this.history.toString(), RED_QUESTION_MARK);
            logTrace(gray(b(length, history, scenarios)));
        }
    }

    /**
     * Processes the test time by calculating the time duration of a test and
     * logging it to the execution object. It also updates the statistics object
     * with the test time.
     */
    private void time() {

        final long testTime = isNull(startedAt) ? 0 : Routines.msSinceStart() - startedAt;
        startedAt = null;
        log(b(c(TEST), TIME, yb(t(testTime))));
        statistics.times().add(testTime);
        getSafeguard().add(testTime);
    }

    /**
     * Executes the safeguards for the Repeater object.
     * This method calculates the total time of the safeguards and checks if it is less
     * than the specified test time safeguard.
     */
    private void timeSafeguard() {

        final long time = getSafeguard().stream().mapToLong(Long::longValue).sum();
        final int tests = min(statistics.history().size(), Test.timeSafeguard.tests());
        logTrace(a(s(TIME, c(SAFEGUARD)), Arrays.toString(safeguard.toArray())));
        logDebug(b(c(LAST), yb(s1(tests, TEST)), TOOK, yb(t(time))));
        if (tests == Test.timeSafeguard.tests()) if (time < Test.timeSafeguard.milliseconds())
            logEvent(S0, b(c(TESTS), EXECUTION, TIME, TOO, s(LOW, _COMMA_), b(s(time), IS, LESS, THAN,
                    s(Test.timeSafeguard.milliseconds()))));
    }

    /**
     * Sets the title on the dashboard.
     * <p>
     * This method sets the title of the dashboard to the title of the current test. If the core's dashboard is not null,
     * it calls the setTitle method of the dashboard object and passes the title of the current test.
     */
    private void title() {

        if (isNotNull(core.getDashboard())) {
            core.getDashboard().setTitle(getTitle());
        }
    }

    /**
     * Verifies the scenarios history safeguard.
     *
     * @throws SafeguardException if the history size is greater than the specified depth
     */
    private void validate() throws SafeguardException {

        final int size = history.size();
        final int depth = historyDepth;
        if (size > depth)
            throw new SafeguardException(b(c(SCENARIOS), EXECUTION, HISTORY, TOO, LONG), b(s(size), IS, MORE, THAN, s(depth)));
    }
}