package works.lysenko.util.data.type;
import works.lysenko.testing.SelectionFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.test.Repeater;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test class for {@link LogRecord}.
 */
@SuppressWarnings({"removal", "deprecation"})
class LogRecordTest {

    private Core previousCore;
    private Repeater repeater;
    private Field tcField;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        final Core core = (Core) SelectionFixture.allocate(Core.class);
        final works.lysenko.base.Test testInstance = (works.lysenko.base.Test) SelectionFixture.allocate(works.lysenko.base.Test.class);
        repeater = (Repeater) SelectionFixture.allocate(Repeater.class);

        final Field repField = works.lysenko.base.Test.class.getDeclaredField("repeater");
        repField.setAccessible(true);
        repField.set(testInstance, repeater);

        final Field tf = Core.class.getDeclaredField("test");
        tf.setAccessible(true);
        tf.set(core, testInstance);

        tcField = Repeater.class.getDeclaredField("testsCount");
        tcField.setAccessible(true);

        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
    }

    @Test
    void testBlankWidthMatchesCurrentNumberOfTests() throws Exception {
        // At test 25
        tcField.set(repeater, 25);
        final LogRecord testRecord = new LogRecord(25, 1000L, null);
        assertEquals("[25]", testRecord.renderTest(1), "Test 25 should be 2 chars wide");

        // Blank line during limbo after test 25 (test == null)
        final LogRecord blankRecord = new LogRecord(null, 1000L, null);
        assertEquals("[  ]", blankRecord.renderTest(1),
                "Blank width should match length of current test count (25 -> 2 spaces)");

        // When test count grows to 100
        tcField.set(repeater, 100);
        assertEquals("[   ]", blankRecord.renderTest(1),
                "Blank width should expand to 3 spaces for test count 100");

        // When test count is 5
        tcField.set(repeater, 5);
        assertEquals("[ ]", blankRecord.renderTest(1),
                "Blank width should be 1 space for single-digit test count");
    }
}
