package works.lysenko;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.base.Exec;
import works.lysenko.base.exec.Scenarios;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.data.enums.Severity;



import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BaseTest {

    private Exec previousExec;

    @BeforeEach
    void setUp() {
        previousExec = Base.exec;
        Base.exec = null;
    }

    @AfterEach
    void tearDown() {
        Base.exec = previousExec;
    }

    @Test
    void testExecRunnableTimes() {
        AtomicInteger counter = new AtomicInteger(0);
        Base.exec(counter::incrementAndGet, 3);
        assertEquals(3, counter.get());
    }

    @Test
    void testFailAndFalse() {
        assertFalse(Base.failAndFalse("test error message"));
    }

    @Test
    void testFailAndNull() {
        assertNull(Base.failAndNull("test error message"));
    }

    @Test
    void testSuccess() {
        assertTrue(Base.success("test success message"));
    }

    @Test
    void testNullExecAccessors() {
        assertNull(Base.getWebDriver());
        assertFalse(Base.isDebug());
        assertFalse(Base.isTrace());
    }

    @Test
    void testIsAllPresentWithEmptyList() {
        assertTrue(Base.isAllPresent(List.of()));
        assertTrue(Base.isAllPresent());
    }

    @Test
    void testLoggingMethodsWithNullExec() {
        assertDoesNotThrow(() -> {
            Base.log("Test log message");
            Base.log(0, "Test log with level", true);
            Base.logDebug("Test debug message");
            Base.logDebug("Test debug with stack", true);
            Base.logDebug(List.of("line1", "line2"));
            Base.logTrace("Test trace message");
            Base.logEmptyLine();
            Base.logEmptyLine(true);
            Base.logEmptyLine(true, true);
            Base.logEvent(Severity.S0, "Test severity event");
        });
    }

    @Test
    void testCurrentDepthWithMockExec() throws Exception {
        final Exec mockExec = SelectionFixture.allocate(Exec.class);


        final Field scenariosField = Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        final Scenarios scenarios = new Scenarios();
        scenariosField.set(mockExec, scenarios);

        Base.exec = mockExec;
        assertEquals(0, Base.currentDepth());
    }

    @Test
    void testExecZeroTimes() {
        AtomicInteger counter = new AtomicInteger(0);
        Base.exec(counter::incrementAndGet, 0);
        assertEquals(0, counter.get());
    }

    @Test
    void testLogEventRecursionGuard() {
        assertDoesNotThrow(() -> {
            Base.logEvent(Severity.S0, "Recursion test message");
        });
    }
}
