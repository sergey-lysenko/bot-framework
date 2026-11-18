package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseTest {

    @Test
    void capitalizesStringsStartingWithLetters() {

        assertEquals("Example", Case.c("example"));
        assertEquals("Example", Case.c("Example"));
    }

    @Test
    void rejectsStringsStartingWithDigits() {

        assertThrows(IllegalArgumentException.class, () -> Case.c("4example"));
    }

    @Test
    void lowerCasesStrings() {

        assertEquals("example", Case.l("EXAMPLE"));
        assertEquals("example", Case.l("EXAMPLE", false));
        assertEquals("example", Case.l("example", true));
    }

    @Test
    void upperCasesStrings() {

        assertEquals("EXAMPLE", Case.u("example"));
        assertEquals("EXAMPLE", Case.u("EXAMPLE"));
    }
}

