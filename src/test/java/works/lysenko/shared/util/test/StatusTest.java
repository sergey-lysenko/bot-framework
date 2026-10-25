package works.lysenko.shared.util.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusTest {

    @Test
    void testGet() {
        final Status status = new Status();
        assertNotNull(status.get());
    }
}
