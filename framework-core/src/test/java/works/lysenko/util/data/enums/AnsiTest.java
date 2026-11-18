package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiTest {

    @Test
    void testSingleLineAnsiFormatting() {
        final String input = "Hello World";
        final String expected = "\033[1;31mHello World\033[0m";
        assertEquals(expected, Ansi.ansi(input, Ansi.RED_BOLD));
    }

    @Test
    void testMultiLineAnsiFormattingWrapsEachLine() {
        final String input = "Line 1\nLine 2\nLine 3";
        final String expected = "\033[1;31mLine 1\033[0m\n\033[1;31mLine 2\033[0m\n\033[1;31mLine 3\033[0m";
        assertEquals(expected, Ansi.ansi(input, Ansi.RED_BOLD));
    }

    @Test
    void testNullInputHandling() {
        assertNull(Ansi.ansi(null, Ansi.RED_BOLD));
        assertEquals("Text", Ansi.ansi("Text", null));
    }

    @Test
    void testRemoveANSICodes() {
        final String colored = "\033[1;31mHello World\033[0m";
        assertEquals("Hello World", Ansi.removeANSICodes(colored));
    }

    @Test
    void testColorHelpers() {
        assertEquals("\033[1;32mTest\033[0m", Ansi.gb("Test"));
        assertEquals("\033[1;31mTest\033[0m", Ansi.rb("Test"));
        assertEquals("\033[1;33mTest\033[0m", Ansi.yb("Test"));
        assertEquals("\033[1;35mTest\033[0m", Ansi.mb("Test"));
    }

    @Test
    void testConditionalColorHelpers() {
        assertEquals("\033[1;31mTest\033[0m", Ansi.rb(true, "Test"));
        assertEquals("Test", Ansi.rb(false, "Test"));
        assertEquals("\033[1;33mTest\033[0m", Ansi.yb(true, "Test"));
        assertEquals("Test", Ansi.yb(false, "Test"));
    }
}
