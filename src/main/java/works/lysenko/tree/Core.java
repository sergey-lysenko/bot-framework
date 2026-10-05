package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebElement;
import works.lysenko.util.apis.action.Verifies;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.sets.SortedScenario;
import works.lysenko.util.prop.core.Fits;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Level;

import java.awt.image.BufferedImage;
import java.util.Set;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.*;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
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
import static works.lysenko.util.prop.tree.Scenario.root;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * Core class represents a base abstract class for scenarios in the testing framework.
 */
@SuppressWarnings({"DesignForExtension", "MethodWithMultipleReturnPoints", "RedundantThrows"})
public abstract class Core extends Root implements _Scenario, Verifies {

    /**
     * weight defined in code
     */
    private Fraction codeWeight = null;

    private long startedAt = ZERO;
    private _Scenario parentScenario = null;

    /**
     * Default constructor
     */
    protected Core() {

    }

    /**
     * @param weight redefined directly in code - overrides other weights
     */
    protected Core(final Fraction weight) {

        this();
        codeWeight = weight;
    }

    @Override
    public void action() throws SafeguardException {

        verify();
    }

    @Override
    public int calculateCombinations(final boolean onlyConfigured) {

        final boolean isDownstream = (Include.downstream) && ZERO < weightDownstream().doubleValue();
        final boolean isUpstream = (Include.upstream) && ZERO < weightUpstream().doubleValue();
        final boolean isConfigured = (isNotNull(weightConfigured())) && ZERO < weightConfigured().doubleValue();
        if (onlyConfigured) return (isNotNull(weightConfigured())) && (isDownstream || isUpstream || isConfigured) ? ONE : ZERO;
        return ONE;
    }

    /**
     * @return logical level of the current scenario (number of ascendants)
     */
    public int depth() {

        return gauge() - exec.scenarios().minDepth();
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    @Override
    public void finals() throws SafeguardException {
        // logDebug(3, FINALS_NO_IMPLEMENTATION);
    }

    @Override
    public boolean fits() {

        return Fits.byDefault;
    }

    public Fraction weightCoded() {

        return codeWeight;
    }

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

    @Override
    public final Fraction weightDownstream() {

        return downstreamWeight(this);
    }

    @Override
    public String getName() {

        return getClass().getName();
    }

    public String getParentScenarioName(final _Scenario scenario) {

        return capitalize(substringAfterLast(scenario.getClass().getPackageName(), s(_DOT_)));
    }

    public final _Scenario getParentScenario() {

        return parentScenario;
    }

    public final void setParentScenario(final _Scenario parentScenario) {

        this.parentScenario = parentScenario;
    }

    @Override
    public String getShortName() {

        if (!isFromMainTree()) return getClass().getName();
        return getClass().getName().replace(s(root, _DOT_), EMPTY);
    }

    @Override
    public String getSimpleName() {

        return getClass().getSimpleName();
    }

    @Override
    public String info() {

        return EMPTY;
    }

    @Override
    public boolean isExecutable() {

        return (isNotNull(weightConfigured()));
    }

    @Override
    public boolean isFromMainTree() {

        return getClass().getPackageName().startsWith(root);
    }

    public boolean isOk() throws SafeguardException {

        if (areFailingEvents()) return false;
        startedAt = msSinceStart();
        exec.scenarios().push(this);
        exec.logEmptyLine();
        log(Level.none, s(ansi(b(s(type().tag()), getShortName()), BLUE_BOLD_BRIGHT), e(s(_COLON_)),
                core.getResults().count(this)), false);
        return true;
    }

    @Override
    public Set<_Scenario> list() {

        final Set<_Scenario> scenarios = new SortedScenario();
        scenarios.add(this);
        return scenarios;
    }

    public void stopTest(final Severity severity) {

        final String explanation = (null == severity) ? DOTS : b(DUE_TO, severity.type().getString(), EVENT);
        log(rb(b(c(STOPPING), explanation)));
        exec.stopTests();
    }

    @Override
    public ScenarioType type() {

        return null;
    }

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

        if (!core.getStopFlag() && core.getResults().areFailingEvents()) stopTest(core.getResults().getGreatestSeverity());
        return core.getResults().areFailingEvents();
    }

    /**
     * Marks the completion of the scenario and logs the relevant information.
     */
    protected final void done() throws SafeguardException {

        final long runtime = msSinceStart() - startedAt;
        final String info = info();
        log(Level.none, b(false, bb(s(q(getShortName()))), // Scenario
                (null == info || info.isEmpty()) ? EMPTY : yb(info), b(DONE, IN), yb(t(runtime))), false);
        exec.scenarios().pop(this);
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