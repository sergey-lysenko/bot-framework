package works.lysenko.shared.util;

import org.junit.jupiter.api.Test;
import works.lysenko.base.parameters.PropertyValidator;
import works.lysenko.shared.util.spec.PropEnum;
import works.lysenko.shared.util.test.Postflight;
import works.lysenko.shared.util.test.Preflight;
import works.lysenko.shared.util.test.Specifics;
import works.lysenko.shared.util.test.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaTest {

    @Test
    void workflowWiresStandardLifecycleHooksAndDiscoversSharedPropEnum() {
        assertNotNull(Agenda.workflow);
        assertEquals(Preflight.class, Agenda.workflow.preflight());
        assertEquals(Status.class, Agenda.workflow.status());
        assertEquals(Specifics.class, Agenda.workflow.exceptional());
        assertEquals(Postflight.class, Agenda.workflow.postflight());
        assertTrue(Agenda.workflow.additional().contains(PropEnum.class));

        PropertyValidator.resetCache();
        assertTrue(PropertyValidator.isKnownConfigProperty(".dummy"));
    }
}
