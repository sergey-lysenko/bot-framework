package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScenarioTypeTest {

    @Test
    void testTag() {
        assertEquals("◆", ScenarioType.LEAF.tag());
        assertEquals("▷", ScenarioType.NODE.tag());
        assertEquals("◼", ScenarioType.MONO.tag());
        assertEquals("●", ScenarioType.CORE.tag());
    }
}
