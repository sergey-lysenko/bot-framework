package works.lysenko.util.apis.data;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.data.enums.ScenarioType;

import java.util.List;

/**
 * Provides Results
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Result {

    /**
     * @param lr log record to add as event
     */
    void addEvent(_LogRecord lr);

    /**
     * @return Weight of the Scenario defined in configuration
     */
    Fraction getConfiguredWeight();

    /**
     * @return Downstream weight of the Scenario
     */
    Fraction getDownstreamWeight();

    /**
     * @return unmodifiable copy of events
     */
    List<_LogRecord> getEvents();

    /**
     * @return number of Scenario executions
     */
    int getExecutions();

    /**
     * @return number of Scenario instances
     */
    int getInstances();

    /**
     * @return the scenario type
     */
    ScenarioType getScenarioType();

    /**
     * @return Upstream weight of the Scenario
     */
    Fraction getUpstreamWeight();

    /**
     * Increment the number of executions for a scenario.
     */
    void tick();
}