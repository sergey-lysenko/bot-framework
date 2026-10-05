package works.lysenko.base.ui;

import works.lysenko.base.Parameters;
import works.lysenko.base.parameters.Gui;
import works.lysenko.base.parameters.PropertiesPanel;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ControlPanel extends JFrame implements _Dashboard {

    private static ControlPanel instance;

    private final JTabbedPane tabbedPane;
    private final Gui gui;
    private final PropertiesPanel propertiesPanel;
    private UserInterface dashboardPanel;
    private final CountDownLatch runLatch = new CountDownLatch(1);

    public static ControlPanel getInstance() {
        return instance;
    }

    public ControlPanel(final Parameters parameters, final Gui guiInstance) {
        instance = this;
        setTitle("Control Panel");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // 1. Configuration Tab
        this.gui = guiInstance;
        final JPanel configPanel = new JPanel(new BorderLayout());
        
        // Remove old dialogueBox dependencies in Gui if possible, or just use it
        final JPanel guiBox = gui.dialogueBox();
        guiBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        propertiesPanel = new PropertiesPanel(parameters.getTest(), parameters.isHeadless(), parameters.isAllLeafs(), parameters.getAllLeafsCount());
        
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guiBox, propertiesPanel);
        // Properties panel width / Gui panel width = 1.618  =>  Gui = 1 / 2.618 ≈ 0.3819
        splitPane.setDividerLocation(0.382); 
        
        configPanel.add(splitPane, BorderLayout.CENTER);

        // Run Button Panel
        final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton runButton = new JButton("Run / Continue");
        runButton.addActionListener(e -> startRun());
        runPanel.add(runButton);
        configPanel.add(runPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Configuration", configPanel);

        // 2. Execution Tab (Dashboard)
        dashboardPanel = new UserInterface(null); // will be refactored to JPanel
        tabbedPane.addTab("Execution", dashboardPanel);

        add(tabbedPane);
    }

    public void displayAndWait() {
        setVisible(true);
        try {
            runLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startRun() {
        gui.propagateUserInput();
        propertiesPanel.applyChanges();
        tabbedPane.setSelectedIndex(1); // Switch to execution tab
        runLatch.countDown();
    }

    // Delegate _Dashboard methods to dashboardPanel
    @Override
    public void setBreadcrumb(List<? extends _Scenario> scenarios) {
        dashboardPanel.setBreadcrumb(scenarios);
    }

    @Override
    public void setInfo(String info, Telemetry telemetry) {
        dashboardPanel.setInfo(info, telemetry);
    }

    @Override
    public boolean isHalt() {
        return dashboardPanel.isHalt();
    }

    @Override
    public void setHalt(boolean halt) {
        dashboardPanel.setHalt(halt);
    }

    @Override
    public boolean isPause() {
        return dashboardPanel.isPause();
    }

    @Override
    public boolean isStop() {
        return dashboardPanel.isStop();
    }

    @Override
    public void setStop(boolean stop) {
        dashboardPanel.setStop(stop);
    }

    @Override
    public boolean isDebug() {
        return dashboardPanel.isDebug();
    }
}
