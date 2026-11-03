package works.lysenko.base.responses;

import works.lysenko.util.apis.log._LogRecord;

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
     * Checks if a log record matches any expected response for a scenario.
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
