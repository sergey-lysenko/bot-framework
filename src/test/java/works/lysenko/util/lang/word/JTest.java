package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JTest {

    @Test
    void testInstantiation() {
        J instance = new J();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertEquals("java", J.JAVA);
        assertNotNull(J.JUST);
        assertFalse(J.JUST.isEmpty());
        assertEquals("\n```", J.JAVA_END);
        assertEquals("\n```java\n", J.JAVA_START);
    }
}
