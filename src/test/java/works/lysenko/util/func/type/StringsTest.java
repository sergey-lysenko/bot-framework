package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StringsTest {

    @Test
    void testStringsInstantiation() {
        Strings strings = new Strings();
        assertNotNull(strings);
    }
}
