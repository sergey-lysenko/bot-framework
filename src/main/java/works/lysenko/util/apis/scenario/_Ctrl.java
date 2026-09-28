package works.lysenko.util.apis.scenario;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;

/**
 * This interface represents a controller that manages a set of scenarios.
 * It allows adding, appending, and executing scenarios, as well as calculating
 * weights and counts related to the scenarios.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "unused"})
public interface _Ctrl {

    /**
     * Executes one of the defined scenarios.
     *
     * <p>As the first step, a copy of the scenarios list is created, which is later referenced as a list of candidates. Then,
     * {@link org.apache.commons.math3.distribution.EnumeratedDistribution} is used to choose a scenario based on weight
     * coefficients.
     *
     * <p>If the sum of weights of all scenarios is zero, no scenario will be selected and no action will be performed
     * except posting the error message to the test log.
     *
     * <p>Negative weight of any scenario is considered a critical exception and leads to the stop of the test.
     *
     * <p>Afterward, the {@link _Scenario#fits()} method of the selected scenario is called to verify compliance with the
     * scenario's prerequisites. In case of a false result, a warning
     * message is posted to the test log and the scenario is removed from the list of candidates.
     *
     * <p>Selection is stopped in one of the following cases:
     * <ul>
     *     <li>{@link _Scenario#fits()} returns {@code true}</li>
     *     <li>There were more selection attempts than the configured retries amount</li>
     *     <li>The list of candidates becomes empty (because all original candidates were removed due to their
     *     {@link _Scenario#fits()} returning {@code false})</li>
     * </ul>
     *
     * <p>Failure to select a scenario due to insufficient prerequisites is not considered exceptional. In this case, parent
     * scenario execution is counted in (as long as its {@link
     *  _Scenario#action()} had already been executed before the attempt of nested scenario selection).
     *      @return {@code true} if the execution was successful, {@code false} otherwise
     *      @throws SafeguardException if an exception occurs during the execution of a safeguard@throws SafeguardException
     *      if an exception occurs during the execution of a safeguard.
     */
    @SuppressWarnings({"UnusedReturnValue", "BooleanMethodNameMustStartWithQuestion"})
    boolean exec() throws SafeguardException;

    /**
     * Retrieves the number of retries for the current selection process.
     *
     * @return The number of retries remaining for the current selection process.
     */
    int getAttempts();

    /**
     * Calculate number of possible execution paths
     *
     * @param onlyConfigured defines whether to include only scenarios configured to
     *                       start, or all available ones
     * @return number of possible execution paths
     */
    int getPathsCount(boolean onlyConfigured);

    /**
     * Retrieves the list of scenarios along with their weights.
     *
     * @return The list of scenarios along with their weights.
     */
    _Pool getPool();

    /**
     * Retrieves the list of scenarios along with their weights.
     *
     * @return The list of scenarios along with their weights.
     */
    List<KeyValue<_Scenario, Fraction>> getWeightedList();
}
