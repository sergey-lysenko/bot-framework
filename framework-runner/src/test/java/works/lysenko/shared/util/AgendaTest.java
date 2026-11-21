package works.lysenko.shared.util;

import org.junit.jupiter.api.Test;
import works.lysenko.base.parameters.PropertyValidator;
import works.lysenko.shared.util.spec.PropEnum;
import works.lysenko.shared.util.test.Postflight;
import works.lysenko.shared.util.test.Preflight;
import works.lysenko.shared.util.test.Pretest;
import works.lysenko.shared.util.test.Specifics;
import works.lysenko.shared.util.test.Status;
import works.lysenko.util.data.records.test.Workflow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaTest {

    @Test
    void workflowWiresStandardLifecycleHooksAndDiscoversSharedPropEnum() {
        assertNotNull(Agenda.workflow);
        assertEquals(Preflight.class, Agenda.workflow.preflight());
        assertEquals(Pretest.class, Agenda.workflow.pretest());
        assertEquals(Status.class, Agenda.workflow.status());
        assertEquals(Specifics.class, Agenda.workflow.exceptional());
        assertEquals(Postflight.class, Agenda.workflow.postflight());
        assertTrue(Agenda.workflow.additional().contains(PropEnum.class));

        final Workflow legacy = new Workflow(
                Preflight.class,
                Status.class,
                Specifics.class,
                Postflight.class,
                List.of(PropEnum.class));
        assertNull(legacy.pretest());
        assertEquals(Preflight.class, legacy.preflight());
        assertEquals(Postflight.class, legacy.postflight());

        PropertyValidator.resetCache();
        assertTrue(PropertyValidator.isKnownConfigProperty(".dummy"));
    }
}
