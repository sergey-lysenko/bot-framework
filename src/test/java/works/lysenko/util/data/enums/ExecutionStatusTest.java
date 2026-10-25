package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionStatusTest {

    @Test
    void testCodeAndColorHex() {
        assertEquals("passed", ExecutionStatus.PASSED.code());
        assertEquals("#22c55e", ExecutionStatus.PASSED.colorHex());
    }
}
