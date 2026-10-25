package works.lysenko.util.func.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssertionsTest {

    @BeforeEach
    void setUp() {
        Base.core = null;
        Base.exec = null;
    }

    @AfterEach
    void tearDown() {
        Base.core = null;
        Base.exec = null;
    }

    @Test
    void testAssertionsInstantiation() {
        Assertions assertions = new Assertions();
        assertNotNull(assertions);
    }

    @Test
    void testAssertEqualsAndNotNull() {
        assertDoesNotThrow(() -> Assertions.assertEquals("a", "a", "msg"));
        assertDoesNotThrow(() -> Assertions.assertNotNull("a", "msg"));
    }
}
