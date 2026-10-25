package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebElement;
import works.lysenko.base.output.AllLeafCompletions;
import works.lysenko.base.output.EtaDebug;
import works.lysenko.base.output.TreeTracker;
import works.lysenko.util.apis.action.Verifies;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.sets.SortedScenario;
import works.lysenko.util.prop.core.*;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.spec.Level;

import java.awt.image.BufferedImage;
import java.util.Set;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.*;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.properties;
import static works.lysenko.base.core.Routines.msSinceStart;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.____.DONE;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.assertTrue;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.core.Weights.configuredWeightValue;
import static works.lysenko.util.func.imgs.Grid.test;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.grid.validation.Check.image;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.U.UI_VERIFICATION_FAILED_IN;
import static works.lysenko.util.lang.word.C.CONTENT;
import static works.lysenko.util.lang.word.E.EVENT;
import static works.lysenko.util.lang.word.S.STOPPING;
import static works.lysenko.util.prop.core.Mode.*;
import static works.lysenko.util.prop.tree.Scenario.root;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * Core class represents a base abstract class for scenarios in the testing framework.
 * It provides common properties, execution lifecycle management, weight calculation,
 * status tracking, and scenario hierarchy navigation.
 */
@SuppressWarnings({"DesignForExtension", "MethodWithMultipleReturnPoints", "RedundantThrows"})
public abstract class Core extends Root implements _Scenario, Verifies {

    /**
     * Weight defined directly in code that overrides other configured weights.
     */
    private Fraction codeWeight = null;

    /**
     * Timestamp in milliseconds since start when this scenario execution began.
     */
    private long startedAt = ZERO;

    /**
     * Parent scenario in the execution tree hierarchy.
     */
    private _Scenario parentScenario = null;

    /**
     * Indicates whether this scenario has failed.
     */
    private boolean failed = false;

    /**
     * Indicates whether a child scenario under this node has failed.
     */
    private boolean childFailed = false;

    /**
     * Indicates whether this scenario is exhausted (no further executions remaining).
     */
    private boolean exhausted = false;

    /**
     * Counter tracking the number of times this scenario was selected in a stale state.
     */
    private int staleCount = 0;

    /**
     * Default constructor for Core scenario.
     */
    protected Core() {

    }

    /**
     * Constructor specifying a code-defined weight.
     *
     * @param weight weight defined directly in code - overrides other weights
     */
    protected Core(final Fraction weight) {

        this();
        codeWeight = weight;
    }

    /**
     * Performs the scenario action by executing verification.
     *
     * @throws SafeguardException if verification fails or safeguard check triggers
     */
    @Override
    public void action() throws SafeguardException {

        verify();
    }

    /**
     * Calculates the total number of execution combinations available in this scenario.
     *
     * @param onlyConfigured true to count only configured scenarios, false for all
     * @return 1 if combinations exist, or 0 otherwise
     */
    @Override
    public int calculateCombinations(final boolean onlyConfigured) {

        final boolean isDownstream = Boolean.TRUE.equals(Include.downstream) && ZERO < weightDownstream().doubleValue();
        final boolean isUpstream = Boolean.TRUE.equals(Include.upstream) && ZERO < weightUpstream().doubleValue();
        final boolean isConfigured = (isNotNull(weightConfigured())) && ZERO < weightConfigured().doubleValue();
        if (onlyConfigured) return (isNotNull(weightConfigured())) && (isDownstream || isUpstream || isConfigured) ? ONE : ZERO;
        return ONE;
    }

    /**
     * Calculates the logical depth level of the current scenario relative to the minimum depth.
     *
     * @return logical level of the current scenario (number of ascendants)
     */
    public int depth() {

        return gauge() - exec.scenarios().minDepth();
    }

    /**
     * Performs finalization steps for the scenario upon completion.
     * Default implementation does nothing and can be overridden by subclasses.
     *
     * @throws SafeguardException if finalization encounters a safeguard condition
     */
    @SuppressWarnings("NoopMethodInAbstractClass")
    @Override
    public void finals() throws SafeguardException {
        // logDebug(3, FINALS_NO_IMPLEMENTATION);
    }

    /**
     * Checks if this scenario fits current execution constraints.
     *
     * @return true if default fit condition passes, false otherwise
     */
    @Override
    public boolean fits() {

        return Fits.byDefault;
    }

    /**
     * Retrieves the weight defined directly in code for this scenario.
     *
     * @return the code weight, or null if not explicitly set
     */
    public Fraction weightCoded() {

        return codeWeight;
    }

    /**
     * Calculates the configured weight of this scenario based on properties or code weight.
     *
     * @return the configured weight Fraction, or null if disabled/unconfigured
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    @Override
    public Fraction weightConfigured() {

        final String defaultWeight = Scenario.defaultWeight;
        if (isNotNull(codeWeight)) return codeWeight;
        if (isNull(properties)) return fr(defaultWeight);
        final String weightText = configuredWeightValue(this);
        if (s(_DASH_).equals(weightText)) return null;
        return fr(weightText);
    }

    /**
     * Calculates downstream weight contribution for this scenario.
     *
     * @return downstream weight Fraction
     */
    @Override
    public final Fraction weightDownstream() {

        return downstreamWeight(this);
    }

    /**
     * Retrieves the fully qualified name of the scenario class.
     *
     * @return fully qualified scenario class name
     */
    @Override
    public String getName() {

        return getClass().getName();
    }

    /**
     * Retrieves the simple name of the parent package of a given scenario.
     *
     * @param scenario the target scenario
     * @return parent scenario package name after last dot
     */
    public String getParentScenarioName(final _Scenario scenario) {

        return substringAfterLast(scenario.getClass().getPackageName(), s(_DOT_));
    }

    /**
     * Retrieves the parent scenario instance.
     *
     * @return parent scenario, or null if root
     */
    public final _Scenario getParentScenario() {

        return parentScenario;
    }

    /**
     * Sets the parent scenario instance for this scenario.
     *
     * @param parentScenario parent scenario instance
     */
    public final void setParentScenario(final _Scenario parentScenario) {

        this.parentScenario = parentScenario;
    }

    /**
     * Retrieves the short name of this scenario stripped of the root package prefix.
     *
     * @return short scenario name
     */
    @Override
    public String getShortName() {

        if (!isFromMainTree()) return getClass().getName();
        return getClass().getName().replace(s(root, _DOT_), EMPTY);
    }

    /**
     * Retrieves the simple class name of this scenario.
     *
     * @return simple class name
     */
    @Override
    public String getSimpleName() {

        return getClass().getSimpleName();
    }

    /**
     * Retrieves the current stale selection count.
     *
     * @return number of times this scenario was stale
     */
    @Override
    public int getStaleCount() {

        return staleCount;
    }

    /**
     * Increments the stale count by one.
     */
    @Override
    public void incrementStaleCount() {

        staleCount++;
    }

    /**
     * Resets the stale count to zero.
     */
    @Override
    public void resetStaleCount() {

        staleCount = 0;
    }

    /**
     * Checks if this scenario is exhausted.
     *
     * @return true if exhausted, false otherwise
     */
    @Override
    public boolean isExhausted() {

        return exhausted;
    }

    /**
     * Marks this scenario as exhausted.
     */
    @Override
    public void markAsExhausted() {

        this.exhausted = true;
    }

    /**
     * Retrieves the current execution status of this scenario.
     *
     * @return ExecutionStatus enum representing current status
     */
    @Override
    public ExecutionStatus getExecutionStatus() {

        if (failed) return ExecutionStatus.FAILED;
        if (childFailed) return ExecutionStatus.CHILD_FAILED;
        if (null != core && null != core.getResults()) {
            final _Result res = core.getResults().getResult(this);
            if (null != res) return res.status();
        }
        return ExecutionStatus.UNVISITED;
    }

    /**
     * Checks if this scenario has failed.
     *
     * @return true if failed, false otherwise
     */
    @Override
    public boolean hasFailed() {

        return failed;
    }

    /**
     * Checks if a child scenario has failed.
     *
     * @return true if child failed, false otherwise
     */
    @Override
    public boolean hasChildFailed() {

        return childFailed;
    }

    /**
     * Marks this scenario as failed and updates execution results.
     */
    @Override
    public void markAsFailed() {

        this.failed = true;
        if (null != core && null != core.getResults()) {
            final _Result res = core.getResults().getResult(this);
            if (null != res) {
                res.updateStatus(ExecutionStatus.FAILED);
            }
        }
    }

    /**
     * Marks child failure status on this scenario and updates execution results.
     */
    @Override
    public void markAsChildFailed() {

        this.childFailed = true;
        if (null != core && null != core.getResults()) {
            final _Result res = core.getResults().getResult(this);
            if (null != res) {
                res.updateStatus(ExecutionStatus.CHILD_FAILED);
            }
        }
    }

    /**
     * Returns additional information string for this scenario.
     *
     * @return information string (empty by default)
     */
    @Override
    public String info() {

        return EMPTY;
    }

    /**
     * Checks if this scenario is eligible for execution.
     *
     * @return true if executable, false otherwise
     */
    @Override
    public boolean isExecutable() {

        return !failed && !exhausted && (isNotNull(weightConfigured()));
    }

    /**
     * Checks if this scenario class resides under the main tree root package.
     *
     * @return true if scenario belongs to main tree, false otherwise
     */
    @Override
    public boolean isFromMainTree() {

        return getClass().getPackageName().startsWith(root);
    }

    /**
     * Prepares scenario execution and logs status.
     *
     * @return true if preparation succeeded and execution can proceed, false otherwise
     * @throws SafeguardException if safeguard checks fail
     */
    public boolean isOk() throws SafeguardException {

        if (isNotNull(core) && core.getStopFlag()) return false;
        if (areFailingEvents()) return false;
        startedAt = msSinceStart();
        if (isNotNull(exec)) {
            exec.logEmptyLine();
            exec.scenarios().push(this);
        }
        if (ScenarioType.LEAF == type() || ScenarioType.MONO == type()) {
            EtaDebug.startCycle(getShortName());
        }
        log(Level.none, s(ansi(b(s(type().tag()), getShortName()), BLUE_BOLD_BRIGHT), e(s(_COLON_)),
                core.getResults().count(this)), false);
        if (isNotNull(core) && isNotNull(core.getTest()) && isNotNull(core.getTest().repeater())) {
            core.getTest().repeater().addToScenarios(this);
        }
        TreeTracker.onNode();
        return true;
    }

    /**
     * Lists this scenario as a single-element scenario set.
     *
     * @return set containing this scenario instance
     */
    @Override
    public Set<_Scenario> list() {

        final Set<_Scenario> scenarios = new SortedScenario();
        scenarios.add(this);
        return scenarios;
    }

    /**
     * Requests test execution stop due to specified severity.
     *
     * @param severity severity level causing the stop
     */
    public void stopTest(final Severity severity) {

        final String explanation = (null == severity) ? DOTS : b(DUE_TO, severity.type().getString(), EVENT);
        log(rb(b(c(STOPPING), explanation)));
        exec.stopTests();
    }

    /**
     * Returns the scenario type.
     *
     * @return null by default in Core, overridden by concrete Leaf/Node subclasses
     */
    @Override
    public ScenarioType type() {

        return null;
    }

    /**
     * Default verification method for UI content.
     *
     * @return true if verification succeeds
     * @throws SafeguardException if verification fails
     */
    @SuppressWarnings("DesignForExtension")
    public boolean verify() throws SafeguardException {

        final WebElement element = find(c(CONTENT));
        final BufferedImage image = makeScreenshot(element);
        final boolean asserted = test(image(image, getShortName())).isPassed();
        assertTrue(asserted, b(UI_VERIFICATION_FAILED_IN, getShortName()));
        return true;
    }

    /**
     * Determines if there are failing events in the scenario.
     *
     * @return true if there are failing events, false otherwise
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean areFailingEvents() {

        if (!resilient && !core.getStopFlag() && core.getResults().areFailingEvents()) stopTest(core.getResults().getGreatestSeverity());
        return !resilient && core.getResults().areFailingEvents();
    }

    /**
     * Marks the completion of the scenario and logs the relevant information.
     *
     * @throws SafeguardException if completion logging encounters errors
     */
    protected final void done() throws SafeguardException {

        final long runtime = msSinceStart() - startedAt;
        final String info = info();
        log(Level.none, b(false, bb(s(q(getShortName()))), // Scenario
                (null == info || info.isEmpty()) ? EMPTY : yb(info), b(DONE, IN), yb(t(runtime))), false);
        if (ScenarioType.LEAF == type() || ScenarioType.MONO == type()) {
            EtaDebug.endCycle(getShortName());
            AllLeafCompletions.checkAndRecord(msSinceStart(), this);
            if (Scenario.branchStalePenalty) {
                _Scenario curr = this;
                while (isNotNull(curr)) {
                    curr.resetStaleCount();
                    curr = curr.getParentScenario();
                }
            }
        }
        exec.scenarios().pop(this);
    }

    /**
     * Checks if all leafs mode is active.
     *
     * @return true if all leafs mode enabled, false otherwise
     */
    private static boolean isAllLeafsMode() {

        if (isNotNull(parameters)) return parameters.isAllLeafs();
        final Integer count = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()) || (isNotNull(count) && ONE < count);
    }

    /**
     * Calculates the logical level of the current scenario by counting the number of ancestors in the scenario hierarchy.
     *
     * @return the logical level of the current scenario
     */
    private int gauge() {

        final int g = getName().split("\\.").length;
        if (isNull(exec.scenarios().minDepth())) exec.scenarios().minDepth(g);
        return g;
    }
}
