package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.E.EVENT;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Represents the execution result of a {@(_Scenario)}, tracking execution status, events, weights, and instance counts.
 * Implements the {@_Result} interface.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"FieldNotUsedInToString", "ClassWithoutNoArgConstructor", "ParameterHidesMemberVariable", "AutoBoxing",
        "NestedMethodCall",
        "ImplicitNumericConversion", "LocalVariableHidesMemberVariable"})
public class Result implements _Result {

    /** The type of the scenario. */
    private final ScenarioType scenarioType;
    /** The list of log records/events associated with this result. */
    private final List<_LogRecord> events;
    /** The configured weight of the scenario. */
    private final Fraction configuredWeight;
    /** The upstream weight of the scenario. */
    private final Fraction upstreamWeight;
    /** The downstream weight of the scenario. */
    private final Fraction downstreamWeight;
    /** The current execution status. */
    private ExecutionStatus status = ExecutionStatus.UNVISITED;
    /** Number of instances of the scenario. */
    private int instances = 0;
    /** Number of executions of the scenario. */
    private int executions = 0;

    /**
     * Constructs a Result object for the specified scenario.
     *
     * @param scenario the scenario for this result
     */
    public Result(final _Scenario scenario) {

        scenarioType = scenario.type();
        configuredWeight = scenario.weightConfigured();
        upstreamWeight = scenario.weightUpstream();
        downstreamWeight = scenario.weightDownstream();
        events = new ArrayList<>(0);
        if (scenario != null && scenario.hasFailed()) {
            status = ExecutionStatus.FAILED;
        }
    }

    /**
     * Constructs a Result object with explicitly specified parameters.
     *
     * @param scenarioType     the type of the scenario
     * @param configuredWeight the configured weight of the scenario
     * @param upstreamWeight   the upstream weight of the scenario
     * @param downstreamWeight the downstream weight of the scenario
     * @param events           the list of log records associated with this result
     * @param executions       the number of executions
     * @param instances        the number of instances
     */
    @SuppressWarnings({"AssignmentOrReturnOfFieldWithMutableType", "ConstructorWithTooManyParameters"})
    public Result(final ScenarioType scenarioType, final Fraction configuredWeight, final Fraction upstreamWeight,
                  final Fraction downstreamWeight, final List<_LogRecord> events, final int executions, final int instances) {

        this.scenarioType = scenarioType;
        this.configuredWeight = configuredWeight;
        this.upstreamWeight = upstreamWeight;
        this.downstreamWeight = downstreamWeight;
        this.events = events;
        this.executions = executions;
        this.instances = instances;
        if (executions > 0) {
            status = ExecutionStatus.PASSED;
        }
        if (events != null) {
            for (final _LogRecord lr : events) {
                if (lr != null && lr.data() instanceof works.lysenko.util.apis.data._Event event) {
                    final works.lysenko.util.data.enums.Severity sev = event.severity();
                    if (sev == works.lysenko.util.data.enums.Severity.S0 || sev == works.lysenko.util.data.enums.Severity.S1) {
                        updateStatus(ExecutionStatus.FAILED);
                    } else if (sev == works.lysenko.util.data.enums.Severity.S2) {
                        updateStatus(ExecutionStatus.WARNING);
                    }
                }
            }
        }
    }

    @Override
    public final ExecutionStatus status() {

        return status;
    }

    @Override
    public final void updateStatus(final ExecutionStatus newStatus) {

        if (null == newStatus) return;
        if (status == ExecutionStatus.FAILED) return;
        if (status == ExecutionStatus.CHILD_FAILED && newStatus != ExecutionStatus.FAILED) return;
        if (status == ExecutionStatus.WARNING && (newStatus != ExecutionStatus.FAILED && newStatus != ExecutionStatus.CHILD_FAILED)) return;
        status = newStatus;
    }

    @Override
    public final void addEvent(final _LogRecord lr) {

        events.add(lr);
        if (null != lr && lr.data() instanceof works.lysenko.util.apis.data._Event event) {
            final works.lysenko.util.data.enums.Severity sev = event.severity();
            if (sev == works.lysenko.util.data.enums.Severity.S0 || sev == works.lysenko.util.data.enums.Severity.S1) {
                updateStatus(ExecutionStatus.FAILED);
            } else if (sev == works.lysenko.util.data.enums.Severity.S2) {
                updateStatus(ExecutionStatus.WARNING);
            }
        }
    }

    /**
     * Returns the configured weight.
     *
     * @return the Fraction representing configured weight
     */
    public final Fraction getConfiguredWeight() {

        return configuredWeight;
    }

    /**
     * Returns the downstream weight.
     *
     * @return the Fraction representing downstream weight
     */
    public final Fraction getDownstreamWeight() {

        return downstreamWeight;
    }

    /**
     * Returns an unmodifiable list of log record events.
     *
     * @return unmodifiable list of {@_LogRecord}
     */
    public final List<_LogRecord> getEvents() {

        return Collections.unmodifiableList(events);
    }

    /**
     * Returns the number of executions.
     *
     * @return execution count
     */
    public final int getExecutions() {

        return executions;
    }

    /**
     * Returns the number of instances.
     *
     * @return instance count
     */
    public final int getInstances() {

        return instances;
    }

    /**
     * Returns the scenario type.
     *
     * @return the ScenarioType
     */
    public final ScenarioType getScenarioType() {

        return scenarioType;
    }

    /**
     * Returns the upstream weight.
     *
     * @return the Fraction representing upstream weight
     */
    public final Fraction getUpstreamWeight() {

        return upstreamWeight;
    }

    @Override
    public final void tick() {

        ++executions;
        if (status == ExecutionStatus.UNVISITED) {
            updateStatus(ExecutionStatus.PASSED);
        }
    }

    @SuppressWarnings("NestedConditionalExpression")
    @Override
    public final String toString() {

        final double epsilon = 1.0E-10;
        final String weight = (null == configuredWeight) ? s(EMP_SET) : s((0.0 < downstreamWeight.doubleValue() &&
                        epsilon < Math.abs(configuredWeight.doubleValue() - downstreamWeight.doubleValue()) ?
                        s(e(ROUND, ts(downstreamWeight)), e(s(RHT_ARR))) : EMPTY),
                e(SQUARE, ts(configuredWeight)),
                (0.0 < upstreamWeight.doubleValue() ? s(e(s(LFT_ARR)), e(ROUND, ts(upstreamWeight))) : EMPTY)
        );
        final String perInstances = (0 < instances) ? s(_COLON_, instances + 1) : EMPTY; // plus original one
        final String events = (getEvents().isEmpty()) ?
                EMPTY : e(true, e(ROUND, s1(getEvents().size(), EVENT)));
        return b(s(weight), s(executions, perInstances, events));
    }
}
