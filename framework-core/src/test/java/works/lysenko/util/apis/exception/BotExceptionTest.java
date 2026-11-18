package works.lysenko.util.apis.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.exception.checked.BotException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("BotException Class Tests")
class BotExceptionTest {

    @Test
    @DisplayName("Verify BotException constructors and getData")
    void testBotExceptionData() {
        RuntimeException cause = new RuntimeException("root cause");
        BotException exWithoutData = new BotException("msg", cause);
        assertEquals("msg", exWithoutData.getMessage());
        assertEquals(cause, exWithoutData.getCause());
        assertNull(exWithoutData.getData());

        Object extraData = "extra payload";
        BotException exWithData = new BotException("msg2", cause, extraData);
        assertEquals("msg2", exWithData.getMessage());
        assertEquals(extraData, exWithData.getData());
    }
}
