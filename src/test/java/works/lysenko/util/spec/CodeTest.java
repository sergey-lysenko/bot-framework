package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CodeTest {

    @Test
    void testCodeSnippets() {
        assertNotNull(Code.JS_FIND_CODE);
        assertEquals("arguments[0].scrollIntoView(true);", Code.JS_FIND_CODE);
        assertNotNull(Code.AUTOMATOR_CODE_PART1);
        assertNotNull(Code.AUTOMATOR_CODE_PART2);
        assertNotNull(Code.AUTOMATOR_CODE_PART3);
        assertNotNull(new Code());
    }
}

