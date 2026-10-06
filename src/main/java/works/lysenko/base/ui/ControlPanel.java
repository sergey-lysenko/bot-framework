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
        applyDarkTheme();
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

        final JPanel coveragePanel = gui.createCoverageEstimatePanel();
        propertiesPanel = new PropertiesPanel(parameters.getTest(), parameters.isHeadless(), parameters.isAllLeafs(), parameters.getAllLeafsCount(), coveragePanel);
        
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guiBox, propertiesPanel);
        // Properties panel width / Gui panel width = 1.618  =>  Gui = 1 / 2.618 ≈ 0.3819
        splitPane.setDividerLocation(0.382); 
        
        configPanel.add(splitPane, BorderLayout.CENTER);

        // Run Button Panel
        final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        final JButton runButton = new JButton("  Run / Continue  ");
        runButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        runButton.setBackground(new Color(0x16, 0xA3, 0x4A));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> startRun());
        runPanel.add(runButton);
        configPanel.add(runPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Configuration", configPanel);

        // 2. Execution Tab (Dashboard)
        dashboardPanel = new UserInterface(null); // will be refactored to JPanel
        tabbedPane.addTab("Execution", dashboardPanel);

        add(tabbedPane);
    }

    public static void applyDarkTheme() {
        try {
            for (final UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (final Exception ignored) {
        }

        final Color bgMain = new Color(0xF1, 0xF5, 0xF9);
        final Color bgCard = new Color(0xFF, 0xFF, 0xFF);
        final Color bgInput = new Color(0xFF, 0xFF, 0xFF);
        final Color textPrimary = new Color(0x0F, 0x17, 0x2A);
        final Color accentBlue = new Color(0x02, 0x84, 0xC7);
        final Color gridBorder = new Color(0xCB, 0xD5, 0xE1);

        UIManager.put("control", bgMain);
        UIManager.put("info", bgCard);
        UIManager.put("nimbusBase", bgMain);
        UIManager.put("nimbusBlueGrey", new Color(0xE2, 0xE8, 0xF0));
        UIManager.put("nimbusFocus", accentBlue);
        UIManager.put("nimbusLightBackground", bgInput);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusSelectionBackground", new Color(0x02, 0x84, 0xC7));

        UIManager.put("Panel.background", bgMain);
        UIManager.put("Table.background", bgInput);
        UIManager.put("Table.foreground", textPrimary);
        UIManager.put("Table.gridColor", gridBorder);
        UIManager.put("TableHeader.background", new Color(0xE2, 0xE8, 0xF0));
        UIManager.put("TableHeader.foreground", textPrimary);
        UIManager.put("TableHeader.font", new Font(Font.SANS_SERIF, Font.BOLD, 12));
        UIManager.put("TabbedPane.background", bgMain);
        UIManager.put("TabbedPane.foreground", textPrimary);
        UIManager.put("TextField.background", bgInput);
        UIManager.put("TextField.foreground", textPrimary);
        UIManager.put("TextField.caretForeground", textPrimary);
        UIManager.put("Label.foreground", textPrimary);
        UIManager.put("Button.background", new Color(0xE2, 0xE8, 0xF0));
        UIManager.put("Button.foreground", textPrimary);
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
        tabbedPane.setEnabledAt(0, false);
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
    public void setPause(boolean active) {
        dashboardPanel.setPause(active);
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

    @Override
    public void setDebug(boolean active) {
        dashboardPanel.setDebug(active);
    }
}
