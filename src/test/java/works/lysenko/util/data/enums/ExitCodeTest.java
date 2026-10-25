package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExitCodeTest {

    @Test
    void testGetCode() {
        assertEquals(0, ExitCode.SUCCESS.getCode());
        assertEquals(10, ExitCode.UNHANDLED_EXCEPTION.getCode());
    }
}
