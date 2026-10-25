package works.lysenko.util.apis.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ExitCode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("_Test Interface Static Methods Tests")
class _TestStaticMethodsTest {

    @Test
    @DisplayName("Verify static printExitCode does not throw")
    void testPrintExitCode() {
        assertDoesNotThrow(() -> _Test.printExitCode(ExitCode.SUCCESS));
    }

    @Test
    @DisplayName("Verify static printThrowable does not throw")
    void testPrintThrowable() {
        assertDoesNotThrow(() -> _Test.printThrowable(new RuntimeException("test error")));
    }
}
