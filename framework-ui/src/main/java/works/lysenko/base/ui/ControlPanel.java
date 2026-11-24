package works.lysenko.base.ui;

import works.lysenko.base.Parameters;
import works.lysenko.base.output.ScenarioTracker;
import works.lysenko.base.output.TreeTracker;
import works.lysenko.base.parameters.Gui;
import works.lysenko.base.parameters.PropertiesPanel;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;

import static works.lysenko.util.func.type.Objects.isNotNull;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * The ControlPanel class.
 */
public class ControlPanel extends JFrame implements _Dashboard {

    static {
        initializeLookAndFeel();
    }

    /**
     * Initializes the Nimbus Look and Feel and maps menu shortcut key masks globally
     * for all standard Swing text components.
     */
    public static void initializeLookAndFeel() {
        try {
            if (UIManager.getLookAndFeel() == null || !"Nimbus".equals(UIManager.getLookAndFeel().getName())) {
                for (final UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            }
        } catch (final Exception _) {}
        applyMenuShortcutKeyMask();
    }

    /**
     * Maps the platform's default menu shortcut key mask (Command on macOS, Control on others)
     * globally in UIManager for all text editing actions (Copy, Paste, Cut, Select All).
     */
    public static void applyMenuShortcutKeyMask() {
        try {
            final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            final String[] mapKeys = {
                    "TextField.focusInputMap",
                    "TextArea.focusInputMap",
                    "TextPane.focusInputMap",
                    "EditorPane.focusInputMap",
                    "PasswordField.focusInputMap",
                    "FormattedTextField.focusInputMap"
            };
            for (final String mapKey : mapKeys) {
                final InputMap im = (InputMap) UIManager.get(mapKey);
                if (im != null) {
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask), DefaultEditorKit.copyAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask), DefaultEditorKit.pasteAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask), DefaultEditorKit.cutAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask), DefaultEditorKit.selectAllAction);
                }
            }
        } catch (final Exception _) {}
    }

    /**
     * Attaches a right-click context menu (Cut, Copy, Paste, Select All) to the given text components.
     *
     * @param components the text components to attach the context menu to
     */
    public static void addContextMenu(final JTextComponent... components) {
        ContextMenu.attach(components);
    }

    private static ControlPanel instance;

    private final JTabbedPane tabbedPane;
    private final Gui gui;
    private final PropertiesPanel propertiesPanel;
    private UserInterface dashboardPanel;
    private final SvgPreviewPanel treePreview;
    private final SvgPreviewPanel scenarioPreview;
    private final CountDownLatch runLatch = new CountDownLatch(1);

    /**
     * Performs get instance.
     * @return the get instance result
     */
    public static ControlPanel getInstance() {
        return instance;
    }

    /**
     * Constructs a new ControlPanel.
     * @param parameters the parameters
     * @param guiInstance the guiInstance
     */
    public ControlPanel(final Parameters parameters, final Gui guiInstance) {
        applyMenuShortcutKeyMask();
        instance = this;
        setTitle("Control Panel");
        setSize(1600, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // 1. Config & Tests Tab (Micro-IDE)
        final ConfigTestsEditorPanel configTestsEditorPanel = new ConfigTestsEditorPanel();
        tabbedPane.addTab("Config & Tests", configTestsEditorPanel);

        // 2. Configuration Tab (Run Preparation)
        this.gui = guiInstance;
        final JPanel configPanel = new JPanel(new BorderLayout());
        
        // Remove old dialogueBox dependencies in Gui if possible, or just use it
        final JPanel guiBox = gui.parametersPanel();
        guiBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final JPanel coveragePanel = gui.createCoverageEstimatePanel();
        propertiesPanel = new PropertiesPanel(parameters.getTest(), parameters.isHeadless(), parameters.isAllLeafs(), parameters.getAllLeafsExecutions(), coveragePanel);
        
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guiBox, propertiesPanel);
        // Properties panel width / Gui panel width = 1.618  =>  Gui = 1 / 2.618 ≈ 0.3819
        splitPane.setDividerLocation(0.382); 
        
        configPanel.add(splitPane, BorderLayout.CENTER);

        // Run Button Panel
        final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));

        final JButton cancelButton = new JButton(" Cancel ");
        cancelButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        cancelButton.addActionListener(_ -> cancelRun());

        final JButton startExecButton = new JButton("  Start & Exec  ");
        startExecButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        startExecButton.setBackground(new Color(0x16, 0xA3, 0x4A));
        startExecButton.setForeground(Color.WHITE);
        startExecButton.setFocusPainted(false);
        startExecButton.addActionListener(_ -> startRun(2));

        final JButton startTreeButton = new JButton("  Start & Tree  ");
        startTreeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        startTreeButton.setBackground(new Color(0x16, 0xA3, 0x4A));
        startTreeButton.setForeground(Color.WHITE);
        startTreeButton.setFocusPainted(false);
        startTreeButton.addActionListener(_ -> startRun(3));

        runPanel.add(cancelButton);
        runPanel.add(startExecButton);
        runPanel.add(startTreeButton);
        configPanel.add(runPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Run Setup", configPanel);

        // 3. Execution Tab (Dashboard)
        dashboardPanel = new UserInterface(null); // will be refactored to JPanel
        tabbedPane.addTab("Execution", dashboardPanel);

        // 4. Tree Progression Tab
        treePreview = new SvgPreviewPanel("No tree progression frame available yet.", "tree-progression-preview");
        tabbedPane.addTab("Tree Progression", treePreview);

        // 5. Scenario Progression Tab
        scenarioPreview = new SvgPreviewPanel("No scenario progression frame available yet.", "scenario-progression-preview");
        tabbedPane.addTab("Scenario Progression", scenarioPreview);

        // 6. Tree Layout Editor
        final TreeLayoutEditorPanel treeLayoutEditorPanel = new TreeLayoutEditorPanel();
        tabbedPane.addTab("Tree Layout Editor", treeLayoutEditorPanel);

        // 7. Known Issues Tab
        final KnownIssuesPanel knownIssuesPanel = new KnownIssuesPanel();
        tabbedPane.addTab("Known Issues", knownIssuesPanel);

        tabbedPane.setSelectedIndex(1); // Run Setup (2nd tab) is default on launch

        tabbedPane.addChangeListener(e -> {
            final int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 1) { // Switching to Run Setup tab
                configTestsEditorPanel.promptToSaveIfModified();
                if (isNotNull(gui)) {
                    gui.refreshTestDropdown();
                }
                if (isNotNull(propertiesPanel) && isNotNull(parameters)) {
                    propertiesPanel.reloadFromDisk(
                            parameters.getTest(),
                            parameters.isHeadless(),
                            parameters.isAllLeafs(),
                            parameters.getAllLeafsExecutions()
                    );
                }
            }
        });

        final String initialSvg = TreeTracker.getLatestFrameSvg();
        if (null != initialSvg) {
            setTreeProgressionSvg(initialSvg);
        } else {
            final Image initialFrame = TreeTracker.getLatestFrameImage();
            if (null != initialFrame) {
                setTreeProgression(initialFrame);
            }
        }
        final String initialScenarioSvg = ScenarioTracker.getLatestFrameSvg();
        if (null != initialScenarioSvg) {
            setScenarioProgressionSvg(initialScenarioSvg);
        }

        add(tabbedPane);
    }

    /**
     * Performs apply dark theme.
     */
    public static void applyDarkTheme() {
        // Disabled theme modifications to prevent com.apple.laf.AquaMenuPainter$RecyclableBorder.get() NPE on macOS.
        // The default native Look and Feel will be used.
    }

    /**
     * Performs display and wait.
     */
    public void displayAndWait() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setVisible(true));
        } else {
            setVisible(true);
        }
        try {
            runLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cancelRun() {
        dispose();
        works.lysenko.util.apis.test._Test.processCode(works.lysenko.util.data.enums.ExitCode.CLOSED_THROUGH_GUI);
    }

    private void startRun(final int targetTabIndex) {
        gui.propagateUserInput();
        propertiesPanel.applyChanges();
        tabbedPane.setSelectedIndex(targetTabIndex);
        tabbedPane.setEnabledAt(0, false);
        tabbedPane.setEnabledAt(1, false);
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

    /**
     * Performs get tabbed pane.
     * @return the get tabbed pane result
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Performs get gui.
     * @return the get gui result
     */
    public Gui getGui() {
        return gui;
    }

    /**
     * Performs get tree image panel.
     * @return the get tree image panel result
     */
    public ImagePanel getTreeImagePanel() {
        return treePreview.getImagePanel();
    }

    /**
     * Performs get scenario image panel.
     * @return the panel that paints the scenario progression frame
     */
    public ImagePanel getScenarioImagePanel() {
        return scenarioPreview.getImagePanel();
    }

    @Override
    public void setTreeProgression(final Image image) {
        treePreview.setImage(image);
    }

    @Override
    public void setTreeProgressionSvg(final String svgContent) {
        treePreview.setSvg(svgContent);
    }

    @Override
    public void setScenarioProgressionSvg(final String svgContent) {
        scenarioPreview.setSvg(svgContent);
    }
}
