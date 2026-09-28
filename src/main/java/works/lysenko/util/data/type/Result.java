package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
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
 * Result of {@link _Scenario} execution
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"FieldNotUsedInToString", "ClassWithoutNoArgConstructor", "ParameterHidesMemberVariable", "AutoBoxing",
        "NestedMethodCall",
        "ImplicitNumericConversion", "LocalVariableHidesMemberVariable"})
public class Result implements _Result {

    /**
     * type of the Scenario
     */
    private final ScenarioType scenarioType;
    private final List<_LogRecord> events;
    private final Fraction configuredWeight;
    private final Fraction upstreamWeight;
    private final Fraction downstreamWeight;
    private int instances = 0;
    private int executions = 0;

    /**
     * @param scenario Scenario for this Result
     */
    public Result(final _Scenario scenario) {

        scenarioType = scenario.type();
        configuredWeight = scenario.weightConfigured();
        upstreamWeight = scenario.weightUpstream();
        downstreamWeight = scenario.weightDownstream();
        events = new ArrayList<>(0);
    }

    /**
     * Represents a result of a scenario.
     *
     * @param scenarioType     The type of the scenario.
     * @param configuredWeight The configured weight of the scenario.
     * @param upstreamWeight   The weight of the scenario's upstream path.
     * @param downstreamWeight The weight of the scenario's downstream path.
     * @param events           The list of log records associated with this scenario.
     * @param executions       The number of executions of the scenario.
     * @param instances        The number of instances of the scenario.
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
    }

    @Override
    public final void addEvent(final _LogRecord lr) {

        events.add(lr);
    }

    public final Fraction getConfiguredWeight() {

        return configuredWeight;
    }

    public final Fraction getDownstreamWeight() {

        return downstreamWeight;
    }

    public final List<_LogRecord> getEvents() {

        return Collections.unmodifiableList(events);
    }

    public final int getExecutions() {

        return executions;
    }

    public final int getInstances() {

        return instances;
    }

    public final ScenarioType getScenarioType() {

        return scenarioType;
    }

    public final Fraction getUpstreamWeight() {

        return upstreamWeight;
    }

    @Override
    public final void tick() {

        ++executions;
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