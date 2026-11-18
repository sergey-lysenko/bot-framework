package works.lysenko.util.data.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreCaseStringTest {

    @Test
    void testCompareIgnoreCase() {
        final IgnoreCaseString comparator = new IgnoreCaseString();

        assertEquals(0, comparator.compare("abc", "ABC"));
        assertEquals(0, comparator.compare("hello", "hello"));
        assertTrue(comparator.compare("abc", "DEF") < 0);
        assertTrue(comparator.compare("XYZ", "abc") > 0);
    }
}
