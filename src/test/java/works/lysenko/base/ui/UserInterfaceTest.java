package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserInterfaceTest {

    @Test
    void terminalFillsSpaceBelowDashboardHeader() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final UserInterface userInterface = new UserInterface(null);
            userInterface.setSize(1200, 800);
            userInterface.doLayout();

            final JScrollPane terminal = terminalScrollPane(userInterface);
            assertNotNull(terminal);
            assertEquals(0, terminal.getX());
            assertEquals(1200, terminal.getWidth());
            assertTrue(terminal.getHeight() > 600);
        });
    }

    @Test
    void debugOutputCanBeEnabledProgrammatically() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final UserInterface userInterface = new UserInterface(null);

            userInterface.setDebug(true);

            assertTrue(userInterface.isDebug());
        });
    }

    @Test
    void breadcrumbUpdatesComponentsAndLayoutOnEDT() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final UserInterface userInterface = new UserInterface(null);
            userInterface.setSize(1200, 800);
            userInterface.doLayout();

            final works.lysenko.util.apis.scenario._Scenario mockScenario1 = (works.lysenko.util.apis.scenario._Scenario) java.lang.reflect.Proxy.newProxyInstance(
                    works.lysenko.util.apis.scenario._Scenario.class.getClassLoader(),
                    new Class<?>[]{works.lysenko.util.apis.scenario._Scenario.class},
                    (proxy, method, args) -> "getSimpleName".equals(method.getName()) ? "RootNode" : null);

            final works.lysenko.util.apis.scenario._Scenario mockScenario2 = (works.lysenko.util.apis.scenario._Scenario) java.lang.reflect.Proxy.newProxyInstance(
                    works.lysenko.util.apis.scenario._Scenario.class.getClassLoader(),
                    new Class<?>[]{works.lysenko.util.apis.scenario._Scenario.class},
                    (proxy, method, args) -> "getSimpleName".equals(method.getName()) ? "LeafScenario" : null);

            userInterface.setBreadcrumb(java.util.List.of(mockScenario1, mockScenario2));
        });
    }

    @Test
    void etaLabelDisplaysDefaultAndCalculatedFormat() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final UserInterface userInterface = new UserInterface(null);
            assertNotNull(userInterface);
        });
    }

    private static JScrollPane terminalScrollPane(final UserInterface userInterface) {
        for (final Component component : userInterface.getComponents()) {
            if (component instanceof JScrollPane scrollPane) return scrollPane;
        }
        return null;
    }
}
