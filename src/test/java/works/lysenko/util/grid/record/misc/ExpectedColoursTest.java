package works.lysenko.util.grid.record.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpectedColoursTest {

    @Test
    void testExpectedColoursInstantiation() {
        ExpectedColours ec = new ExpectedColours("red", "green");
        assertEquals("red", ec.todo());
        assertEquals("green", ec.done());
    }
}
