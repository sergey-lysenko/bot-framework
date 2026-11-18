package works.lysenko.util.data.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTest {

    @Test
    void testCompare() {
        final Text comparator = new Text();

        assertEquals(0, comparator.compare("abc", "abc"));
        assertTrue(comparator.compare("abc", "def") < 0);
        assertTrue(comparator.compare("def", "abc") > 0);
        assertTrue(comparator.compare("ABC", "abc") < 0);
    }
}
