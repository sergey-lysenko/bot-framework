package works.lysenko.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgendaTest {

    @Test
    void testInstantiation() {
        final Agenda agenda = new Agenda();
        assertNotNull(agenda);
    }

    @Test
    void testWorkflowField() {
        assertNotNull(Agenda.workflow);
    }
}
