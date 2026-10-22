package works.lysenko.util.call;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Exec;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.base.output.SequenceTracker;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.PropEnum;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Verifies how a scenario sequence script influences {@link Selector} candidate selection in both
 * {@code SUGGESTION} and {@code ORDER} modes.
 */
@SuppressWarnings({"removal", "deprecation"})
class SequenceSelectionTest {

    private static final String SEQUENCE = "SEQUENCE";

    private Core previousCore;
    private Exec previousExec;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private String previousSequence;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeTrue(null == System.getenv(SEQUENCE), "SEQUENCE environment variable overrides system properties");
        previousCore = Base.core;
        previousExec = Base.exec;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousSequence = System.getProperty(SEQUENCE);
        Base.parameters = null;
        Base.properties = null;
        events.clear();
        SequenceTracker.reset();

        final Unsafe unsafe = unsafe();
        final Core core = (Core) unsafe.allocateInstance(Core.class);
        set(Core.class, core, "results", new Results());
        set(Core.class, core, "logger", recordingLogger());
        Base.core = core;

        final Exec exec = (Exec) unsafe.allocateInstance(Exec.class);
        Base.exec = exec;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.exec = previousExec;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        if (null == previousSequence) System.clearProperty(SEQUENCE);
        else System.setProperty(SEQUENCE, previousSequence);
        SequenceTracker.reset();
    }

    @Test
    void scriptedScenarioIsSelectedAndScriptAdvances(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        final Leaf b = new LeafB();
        script(dir, b.getShortName() + "\n" + a.getShortName() + "\n");

        // Weighted selection would almost never pick B here; the script must win deterministically.
        for (int i = 0; i < 20; i++) {
            SequenceTracker.reset();
            System.setProperty(SEQUENCE, dir.resolve("script.sequence").toString());
            assertSame(b, select(candidates(kv(a, fr(1000.0)), kv(b, fr(0.001)))));
        }
        assertTrue(events.isEmpty(), "a successful scripted selection must not log events: " + events);
    }

    @Test
    void scriptIsConsumedStepByStep(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        final Leaf b = new LeafB();
        script(dir, a.getShortName() + "\n" + b.getShortName() + "\n");
        final List<KeyValue<_Scenario, Fraction>> candidates = candidates(kv(a, fr(1.0)), kv(b, fr(1.0)));

        assertSame(a, select(candidates));
        assertEquals(b.getShortName(), SequenceTracker.peekNextScenario());
        assertSame(b, select(candidates));
        assertFalse(SequenceTracker.hasActiveScript(), "script must be exhausted after the last step");
    }

    @Test
    void matchingIsCaseInsensitive(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        final Leaf b = new LeafB();
        script(dir, b.getShortName().toUpperCase() + "\n");

        assertSame(b, select(candidates(kv(a, fr(1000.0)), kv(b, fr(0.001)))));
    }

    @Test
    void scenarioCanBeMatchedByFullName(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        final Leaf b = new LeafB();
        script(dir, b.getName() + "\n");

        assertSame(b, select(candidates(kv(a, fr(1000.0)), kv(b, fr(0.001)))));
    }

    @Test
    void suggestionModeFallsBackToWeightedSelectionWithoutLoggingFailure(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        script(dir, "no.such.Scenario\n");

        assertSame(a, select(candidates(kv(a, fr(1.0)))));
        assertTrue(events.isEmpty(), "SUGGESTION mode must not report unavailable scenarios as events: " + events);
        assertEquals("no.such.Scenario", SequenceTracker.peekNextScenario(), "unavailable step must stay pending");
    }

    @Test
    void orderModeReportsFailureWithDefaultSeverityAndStillSelects(@TempDir final Path dir) throws Exception {
        setProperty(PropEnum._TREE_SEQUENCE_MODE, "ORDER");
        final Leaf a = new LeafA();
        script(dir, "no.such.Scenario\n");

        assertSame(a, select(candidates(kv(a, fr(1.0)))));
        assertEquals(1, events.size(), "exactly one failure event expected: " + events);
        assertTrue(events.get(0).startsWith(Severity.S0 + ":"), events.get(0));
        assertTrue(events.get(0).contains("no.such.Scenario"), events.get(0));
        assertTrue(events.get(0).contains("Sequence script order failed"), events.get(0));
    }

    @Test
    void orderModeUsesConfiguredFailureSeverity(@TempDir final Path dir) throws Exception {
        setProperty(PropEnum._TREE_SEQUENCE_MODE, "ORDER");
        setProperty(PropEnum._TREE_SEQUENCE_FAILURE_SEVERITY, "S2");
        final Leaf a = new LeafA();
        script(dir, "no.such.Scenario\n");

        select(candidates(kv(a, fr(1.0))));

        assertEquals(1, events.size(), String.valueOf(events));
        assertTrue(events.get(0).startsWith(Severity.S2 + ":"), events.get(0));
    }

    @Test
    void orderModeDoesNotFailWhenScriptedScenarioIsAvailable(@TempDir final Path dir) throws Exception {
        setProperty(PropEnum._TREE_SEQUENCE_MODE, "ORDER");
        final Leaf a = new LeafA();
        final Leaf b = new LeafB();
        script(dir, b.getShortName() + "\n");

        assertSame(b, select(candidates(kv(a, fr(1000.0)), kv(b, fr(0.001)))));
        assertTrue(events.isEmpty(), String.valueOf(events));
    }

    @Test
    void exhaustedScriptLeavesSelectionToWeights(@TempDir final Path dir) throws Exception {
        final Leaf a = new LeafA();
        script(dir, a.getShortName() + "\n");
        final List<KeyValue<_Scenario, Fraction>> candidates = candidates(kv(a, fr(1.0)));

        assertSame(a, select(candidates));
        assertFalse(SequenceTracker.hasActiveScript());
        assertSame(a, select(candidates), "weighted selection must keep working after the script is exhausted");
        assertTrue(events.isEmpty(), String.valueOf(events));
    }

    @Test
    void noScriptMeansPlainWeightedSelection() throws Exception {
        System.clearProperty(SEQUENCE);
        final Leaf a = new LeafA();

        assertSame(a, select(candidates(kv(a, fr(1.0)))));
        assertTrue(events.isEmpty(), String.valueOf(events));
    }

    // ---------------------------------------------------------------------------------------------

    private void script(final Path dir, final String content) throws Exception {
        final Path file = dir.resolve("script.sequence");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        System.setProperty(SEQUENCE, file.toString());
        SequenceTracker.reset();
    }

    @SafeVarargs
    private static List<KeyValue<_Scenario, Fraction>> candidates(final KeyValue<_Scenario, Fraction>... pairs) {
        return new ArrayList<>(List.of(pairs));
    }

    private static _Scenario select(final List<KeyValue<_Scenario, Fraction>> candidates) throws Exception {
        final Selector selector = new Selector(new Ctrl(null), 1);
        final Method method = Selector.class.getDeclaredMethod("selectAmongCandidates", java.util.Collection.class);
        method.setAccessible(true);
        try {
            return (_Scenario) method.invoke(selector, candidates);
        } catch (final InvocationTargetException e) {
            throw (e.getCause() instanceof Exception cause) ? cause : e;
        }
    }

    private void setProperty(final PropEnum property, final String value) throws Exception {
        final TestProperties testProperties = new TestProperties();
        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        final Properties props = null != Base.properties ? (Properties) theField.get(Base.properties) : new Properties();
        props.put(property.getPropertyName(), value);
        theField.set(testProperties, props);
        Base.properties = testProperties;
    }

    private _Logs recordingLogger() {
        return (_Logs) Proxy.newProxyInstance(_Logs.class.getClassLoader(), new Class<?>[]{_Logs.class}, (proxy, method, args) -> {
            if ("logEvent".equals(method.getName())) {
                events.add(args[0] + ": " + args[1]);
            }
            final Class<?> type = method.getReturnType();
            if (boolean.class == type) return false;
            if (int.class == type) return 0;
            return null;
        });
    }

    private static Unsafe unsafe() throws Exception {
        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    private static void set(final Class<?> owner, final Object target, final String name, final Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class LeafA extends Leaf {
        LeafA() {
            super(Fraction.ONE);
        }
    }

    private static final class LeafB extends Leaf {
        LeafB() {
            super(Fraction.ONE);
        }
    }
}
