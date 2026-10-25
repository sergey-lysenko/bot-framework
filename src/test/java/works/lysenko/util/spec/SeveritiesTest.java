package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SeveritiesTest {

    @Test
    void testSeveritiesConstants() {
        assertNotNull(Severities.externalEvent);
        assertNotNull(new Severities());
    }
}

