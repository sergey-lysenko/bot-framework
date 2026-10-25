package works.lysenko.util.data.records.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.core._Status;
import works.lysenko.util.apis.scenario._Scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("test.Workflow Record Tests")
class WorkflowTest {

    @Test
    @DisplayName("Should store and retrieve workflow classes and collections")
    void testRecordFields() {
        final Class<? extends Runnable> preflight = DummyRunnable.class;
        @SuppressWarnings("unchecked")
        final Class<? extends _Status> status = (Class<? extends _Status>) (Class<?>) _Status.class;
        @SuppressWarnings("unchecked")
        final Class<? extends _Scenario> exceptional = (Class<? extends _Scenario>) (Class<?>) _Scenario.class;
        final Class<? extends Runnable> postflight = DummyRunnable.class;
        @SuppressWarnings("unchecked")
        final Collection<Class<? extends _PropEnum>> additional = List.of((Class<? extends _PropEnum>) (Class<?>) _PropEnum.class);

        final Workflow workflow = new Workflow(preflight, status, exceptional, postflight, additional);

        assertEquals(preflight, workflow.preflight());
        assertEquals(status, workflow.status());
        assertEquals(exceptional, workflow.exceptional());
        assertEquals(postflight, workflow.postflight());
        assertEquals(additional, workflow.additional());
    }

    @Test
    @DisplayName("Should handle null classes")
    void testNullFields() {
        final Workflow workflow = new Workflow(null, null, null, null, null);

        assertNull(workflow.preflight());
        assertNull(workflow.status());
        assertNull(workflow.exceptional());
        assertNull(workflow.postflight());
        assertNull(workflow.additional());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Workflow w1 = new Workflow(DummyRunnable.class, null, null, null, null);
        final Workflow w2 = new Workflow(DummyRunnable.class, null, null, null, null);
        final Workflow w3 = new Workflow(null, null, null, null, null);

        assertEquals(w1, w2);
        assertEquals(w1.hashCode(), w2.hashCode());
        assertNotEquals(w1, w3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final Workflow workflow = new Workflow(null, null, null, null, null);
        assertNotNull(workflow.toString());
    }

    private static class DummyRunnable implements Runnable {
        @Override
        public void run() {
        }
    }
}
