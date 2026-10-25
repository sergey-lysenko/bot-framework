package works.lysenko.util.call.selector;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.core.Safeguard;
import works.lysenko.util.prop.tree.Notify;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class UtilsTest {

    private SelectionFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = SelectionFixture.install();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    // ----------------------------------------------------------------------------- createReadableScenariosList

    @Test
    void emptyListIsRenderedAsPlaceholder() {
        final String text = Utils.createReadableScenariosList(new ArrayList<>(), true, false);

        assertTrue(text.toLowerCase().contains("list"), text);
        assertFalse(text.contains("Alpha"), text);
    }

    @Test
    void readableListContainsSimpleClassNamesWithoutWeightsOutsideDebug() {
        final String text = Utils.createReadableScenariosList(List.of(kv(alpha(), fr(1.0)), kv(beta(), fr(2.0))), true, false);

        assertTrue(text.contains("Alpha"), text);
        assertTrue(text.contains("Beta"), text);
        assertFalse(text.contains(":"), "weights must only be shown in debug mode: " + text);
    }

    @Test
    void zeroWeightScenariosAreOmittedUnlessRequested() {
        final List<KeyValue<_Scenario, Fraction>> list = List.of(kv(alpha(), fr(1.0)), kv(beta(), fr(0.0)));

        final String without = Utils.createReadableScenariosList(list, false, false);
        final String with = Utils.createReadableScenariosList(list, true, false);

        assertTrue(without.contains("Alpha"), without);
        assertFalse(without.contains("Beta"), without);
        assertTrue(with.contains("Alpha") && with.contains("Beta"), with);
    }

    @Test
    void debugModeAppendsWeights() {
        final String text = Utils.createReadableScenariosList(List.of(kv(alpha(), fr(1.0))), true, true);

        assertTrue(text.contains("Alpha:"), text);
    }

    @Test
    void nullWeightIsTreatedAsZero() {
        final List<KeyValue<_Scenario, Fraction>> list = List.of(kv(alpha(), null), kv(beta(), fr(1.0)));

        final String excluded = Utils.createReadableScenariosList(list, false, false);

        assertFalse(excluded.contains("Alpha"), excluded);
        assertTrue(excluded.contains("Beta"), excluded);
    }

    @Test
    void readableListIsSortedAlphabetically() {
        final String text = Utils.createReadableScenariosList(List.of(kv(beta(), fr(1.0)), kv(alpha(), fr(1.0))), true, false);

        assertTrue(text.indexOf("Alpha") < text.indexOf("Beta"), text);
    }

    // ----------------------------------------------------------------------------- getThePair

    @Test
    void pairIsFoundByIdentity() {
        final Leaf a = alpha();
        final Leaf b = beta();
        final KeyValue<_Scenario, Fraction> pa = kv(a, fr(1.0));
        final KeyValue<_Scenario, Fraction> pb = kv(b, fr(2.0));

        assertSame(pb, Utils.getThePair(b, List.of(pa, pb)));
        assertSame(pa, Utils.getThePair(a, List.of(pa, pb)));
    }

    @Test
    void missingPairIsNull() {
        final KeyValue<_Scenario, Fraction> pa = kv(alpha(), fr(1.0));

        assertNull(Utils.getThePair(beta(), List.of(pa)));
        assertNull(Utils.getThePair(alpha(), new ArrayList<>()), "equal-but-distinct instances must not match");
    }

    // ----------------------------------------------------------------------------- isStillSelectable

    @Test
    void selectionContinuesOnlyWhenNothingFitsAndAttemptsAndCandidatesRemain() {
        final List<KeyValue<_Scenario, Fraction>> some = List.of(kv(alpha(), fr(1.0)));
        final List<KeyValue<_Scenario, Fraction>> none = new ArrayList<>();

        assertTrue(Utils.isStillSelectable(false, 1, some));
        assertFalse(Utils.isStillSelectable(true, 1, some), "already fitting");
        assertFalse(Utils.isStillSelectable(false, 0, some), "no attempts left");
        assertFalse(Utils.isStillSelectable(false, 1, none), "no candidates left");
    }

    @Test
    void stopFlagEndsSelection() {
        fixture.core().setStopFlag(true);

        assertFalse(Utils.isStillSelectable(false, 5, List.of(kv(alpha(), fr(1.0)))));
    }

    // ----------------------------------------------------------------------------- notices

    @Test
    void notExecutableNoticeIsLoggedAtS3WithScenarioName() {
        Notify.notExecutable = true;
        final Leaf a = alpha();

        Utils.logScenarioNotExecutable(a);

        assertEquals(1, fixture.events.size(), String.valueOf(fixture.events));
        assertTrue(fixture.events.get(0).startsWith(Severity.S3 + ":"), fixture.events.get(0));
        assertTrue(fixture.events.get(0).contains(a.getName()), fixture.events.get(0));
        assertTrue(fixture.events.get(0).toLowerCase().contains("not executable"), fixture.events.get(0));
    }

    @Test
    void notExecutableNoticeCanBeDisabled() {
        Notify.notExecutable = false;

        Utils.logScenarioNotExecutable(alpha());

        assertTrue(fixture.events.isEmpty(), String.valueOf(fixture.events));
    }

    // ----------------------------------------------------------------------------- verifyScenarioSelection

    @Test
    void verifiedScenarioIsReturnedAndRecordedInSequence() throws Exception {
        final Leaf a = alpha();

        assertSame(a, Utils.verifyScenarioSelection(a));
    }

    @Test
    void scenarioNestedDeeperThanSafeguardIsRejected() {
        for (int i = 0; i <= Safeguard.scenarioDepth; i++) {
            Base.exec.scenarios().push(alpha());
        }

        final SafeguardException e = assertThrows(SafeguardException.class, () -> Utils.verifyScenarioSelection(alpha()));
        assertTrue(e.getMessage().toLowerCase().contains("too deep"), e.getMessage());
    }

    @Test
    void scenarioAtExactlyTheSafeguardDepthIsAccepted() throws Exception {
        for (int i = 0; i < Safeguard.scenarioDepth; i++) {
            Base.exec.scenarios().push(alpha());
        }
        final Leaf a = alpha();

        assertSame(a, Utils.verifyScenarioSelection(a));
    }

    @Test
    void logAccountingLogsWhenEnabled() {
        works.lysenko.util.prop.tree.Include.upstream = true;
        works.lysenko.util.prop.tree.Include.downstream = true;
        fixture.setProperty(works.lysenko.util.prop.PropEnum._LOGS_DEBUG, "true");
        assertDoesNotThrow(() -> Utils.logAccounting());
        works.lysenko.util.prop.tree.Include.upstream = false;
        works.lysenko.util.prop.tree.Include.downstream = false;
    }

    @Test
    void logDebugAndLogEventWorkAsExpected() {
        Utils.logDebug("test debug message");
        Utils.logEvent(Severity.S2, "test event message");
        assertTrue(fixture.events.stream().anyMatch(e -> e.contains("test event message")), String.valueOf(fixture.events));
    }

    @Test
    void recordInstantiatesAndHasToString() {
        final Utils u = new Utils();
        assertNotNull(u.toString());
    }

    // ----------------------------------------------------------------------------- helpers

    private static Leaf alpha() {
        return new Alpha();
    }

    private static Leaf beta() {
        return new Beta();
    }

    private static final class Alpha extends Leaf {
        Alpha() {
            super(Fraction.ONE);
        }
    }

    private static final class Beta extends Leaf {
        Beta() {
            super(Fraction.ONE);
        }
    }
}
