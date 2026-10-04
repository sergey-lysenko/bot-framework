package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseTest {

    @Test
    void capitalizesStringsStartingWithLetters() {

        assertEquals("Example", Case.c("example"));
    }

    @Test
    void rejectsStringsStartingWithDigits() {

        assertThrows(IllegalArgumentException.class, () -> Case.c("4example"));
    }
}
