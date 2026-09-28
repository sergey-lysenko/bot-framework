package works.lysenko.base.exec;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.execution._Scenarios;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.tree.Scenario;

import java.util.ArrayDeque;

import static works.lysenko.Base.core;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.assertEqualsSilent;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.S.SCENARIOS_STACK;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.F.FAILURES;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.U.UNEXPECTED;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * The Scenarios class provides functionality for managing and interacting
 * with a stack-based execution mechanism that handles various scenarios. It
 * supports operations like pushing, popping, querying the stack's state, and
 * maintaining minimal depth information for the active scenarios.
 */
public class Scenarios implements _Scenarios {

    private final ArrayDeque<_Scenario> stack = new ArrayDeque<>(0);
    private Integer minDepth = null;


    public final int depth() {

        return stack.size();
    }

    public final _Scenario current() {

        return stack.isEmpty() ? null : stack.peek();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final String currentString(final boolean clearStack) {

        // Returns scenario name; clears stack if requested
        if (stack.isEmpty()) {
            logDebug(b(SCENARIOS_STACK, s(EMPTY, _DOT_), c(NO), FAILURES), true);
            return null;
        } else {
            logDebug(b(SCENARIOS_STACK, (stack.toString()).replace(Scenario.root, StringUtils.EMPTY)), true);
            String s = null;
            if (isNotNull(stack.peek())) {
                s = stack.peek().getShortName();
            }
            if (clearStack) stack.clear();
            return s;
        }
    }

    public final Integer minDepth() {

        return minDepth;
    }

    public final void minDepth(final Integer minDepth) {

        this.minDepth = minDepth;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public final void pop(final _Scenario scenario) throws SafeguardException {

        final _Scenario[] currentArray = stack.toArray(new _Scenario[1]);
        if (isNotNull(scenario)) {
            // Validates a scenario; adds to history; removes from stack
            if (isNotNull(currentArray[0])) {
                assertEqualsSilent(currentArray[0].getName(), scenario.getName(), b(c(UNEXPECTED), SCENARIO));
                core.getTest().repeater().addToHistory(scenario);
                stack.pop();
            } else {
                // exec.logEvent(S2, b(c(NO), SCENARIOS, IN, STACK));
            }
        }
    }

    public final void push(final _Scenario scenario) {

        stack.push(scenario);
        core.getTest().repeater().addToScenarios(scenario);
    }
}
