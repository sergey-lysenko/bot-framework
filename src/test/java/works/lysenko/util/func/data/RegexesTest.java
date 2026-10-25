package works.lysenko.util.func.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexesTest {

    @Test
    void testRegexesInstantiation() {
        Regexes regexes = new Regexes();
        assertNotNull(regexes);
    }

    @Test
    void testGroupsAndLine() {
        assertEquals("(abc)", Regexes.capturingGroup("abc"));
        assertEquals("(?:abc)", Regexes.nonCapturingGroup("abc"));
        assertTrue(Regexes.line("abc").startsWith("^"));
        assertTrue(Regexes.line("abc").endsWith("$"));
    }
}
