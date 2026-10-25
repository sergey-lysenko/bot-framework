package works.lysenko.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BotRunnerTest {

    @Test
    void testInstantiation() {
        final BotRunner runner = new BotRunner();
        assertNotNull(runner);
    }
}
