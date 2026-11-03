package works.lysenko.base.responses;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Store for managing expected browser responses during test execution.
 */
public class ExpectedResponsesStore {

    private final List<ExpectedResponse> responses;

    public ExpectedResponsesStore() {
        this.responses = ExpectedResponsesLoader.loadDefault();
    }

    public ExpectedResponsesStore(final List<ExpectedResponse> responses) {
        this.responses = (null != responses) ? responses : new ArrayList<>(0);
    }

    public List<ExpectedResponse> getResponses() {
        return responses;
    }

    /**
     * Checks if a log record matches any expected response for a scenario name.
     *
     * @param scenarioName current scenario name
     * @param lr           log record
     * @return true if expected
     */
    public boolean isExpected(final String scenarioName, final _LogRecord lr) {
        if (null == responses || responses.isEmpty() || null == lr) return false;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenarioName)
                    && ExpectedResponsesMatcher.matchesPatternForRecord(er.pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a log record matches any expected response for a scenario instance.
     *
     * @param scenario scenario instance
     * @param lr       log record
     * @return true if expected
     */
    public boolean isExpected(final _Scenario scenario, final _LogRecord lr) {
        if (null == responses || responses.isEmpty() || null == scenario || null == lr) return false;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenario)
                    && ExpectedResponsesMatcher.matchesPatternForRecord(er.pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a log record matches any expected response for a tree NodeData.
     *
     * @param node tree node
     * @param lr   log record
     * @return true if expected
     */
    public boolean isExpected(final NodeData node, final _LogRecord lr) {
        if (null == responses || responses.isEmpty() || null == node || null == lr) return false;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesNodeScenario(er.scenario(), node)
                    && ExpectedResponsesMatcher.matchesPatternForRecord(er.pattern(), lr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given scenario and execution result match any expected response pattern.
     *
     * @param scenario the scenario
     * @param result   the execution result
     * @return true if the scenario's result events match an expected response pattern
     */
    public boolean matchesNode(final _Scenario scenario, final _Result result) {
        if (null == responses || responses.isEmpty() || null == scenario || null == result) return false;
        if (null == result.getEvents() || result.getEvents().isEmpty()) return false;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenario)
                    && ExpectedResponsesMatcher.matchesPattern(er.pattern(), result)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given tree node's events match any expected response pattern for that node.
     *
     * @param node the tree node
     * @return true if the node's result events match an expected response pattern
     */
    public boolean matchesNode(final NodeData node) {
        if (null == responses || responses.isEmpty() || null == node || null == node.result()) return false;
        if (null == node.result().getEvents() || node.result().getEvents().isEmpty()) return false;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesNodeScenario(er.scenario(), node)
                    && ExpectedResponsesMatcher.matchesPattern(er.pattern(), node.result())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all matching expected responses for a log record in a scenario.
     *
     * @param scenarioName current scenario name
     * @param lr           log record
     * @return list of matching expected responses
     */
    public List<ExpectedResponse> getForRecord(final String scenarioName, final _LogRecord lr) {
        final List<ExpectedResponse> result = new ArrayList<>();
        if (null == responses || responses.isEmpty() || null == lr) return result;
        for (final ExpectedResponse er : responses) {
            if (ExpectedResponsesMatcher.matchesScenario(er.scenario(), scenarioName)
                    && ExpectedResponsesMatcher.matchesPatternForRecord(er.pattern(), lr)) {
                result.add(er);
            }
        }
        return result;
    }
}
