package works.lysenko.base.ui;

import works.lysenko.base.Parameters;
import works.lysenko.base.output.TreeTracker;
import works.lysenko.base.parameters.Gui;
import works.lysenko.base.parameters.PropertiesPanel;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.scenario._Scenario;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
        } catch (final Exception ignored) {}
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
        } catch (final Exception ignored) {}
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
    private final CardLayout treeProgressionLayout;
    private final JPanel treeProgressionPanel;
    private final ImagePanel treeImagePanel;
    private final JViewport treeViewport;
    private final CountDownLatch runLatch = new CountDownLatch(1);

    public static ControlPanel getInstance() {
        return instance;
    }

    public ControlPanel(final Parameters parameters, final Gui guiInstance) {
        applyMenuShortcutKeyMask();
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
        final JPanel guiBox = gui.parametersPanel();
        guiBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final JPanel coveragePanel = gui.createCoverageEstimatePanel();
        propertiesPanel = new PropertiesPanel(parameters.getTest(), parameters.isHeadless(), parameters.isAllLeafs(), parameters.getAllLeafsCount(), coveragePanel);
        
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guiBox, propertiesPanel);
        // Properties panel width / Gui panel width = 1.618  =>  Gui = 1 / 2.618 ≈ 0.3819
        splitPane.setDividerLocation(0.382); 
        
        configPanel.add(splitPane, BorderLayout.CENTER);

        // Run Button Panel
        final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));

        final JButton cancelButton = new JButton(" Cancel ");
        cancelButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        cancelButton.addActionListener(e -> cancelRun());

        final JButton runButton = new JButton("  Start Test  ");
        runButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        runButton.setBackground(new Color(0x16, 0xA3, 0x4A));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> startRun());

        runPanel.add(cancelButton);
        runPanel.add(runButton);
        configPanel.add(runPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Configuration", configPanel);

        // 2. Execution Tab (Dashboard)
        dashboardPanel = new UserInterface(null); // will be refactored to JPanel
        tabbedPane.addTab("Execution", dashboardPanel);

        // 3. Tree Progression Tab
        treeProgressionLayout = new CardLayout();
        treeProgressionPanel = new JPanel(treeProgressionLayout);

        final JPanel placeholderPanel = new JPanel(new GridBagLayout());
        final JLabel placeholderLabel = new JLabel("No tree progression frame available yet.");
        placeholderLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(0x94, 0xA3, 0xB8));
        placeholderPanel.add(placeholderLabel);

        treeImagePanel = new ImagePanel();
        treeImagePanel.setBackground(new Color(0x0F, 0x17, 0x2A));
        final JScrollPane treeScrollPane = new JScrollPane(treeImagePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        treeScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        treeViewport = treeScrollPane.getViewport();
        treeViewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent event) {

                fitTreeImageToViewport();
            }
        });

        treeProgressionPanel.add(placeholderPanel, "PLACEHOLDER");
        treeProgressionPanel.add(treeScrollPane, "IMAGE");

        tabbedPane.addTab("Tree Progression", treeProgressionPanel);

        // 4. Known Issues Tab
        final KnownIssuesPanel knownIssuesPanel = new KnownIssuesPanel();
        tabbedPane.addTab("Known Issues", knownIssuesPanel);

        final Image initialFrame = TreeTracker.getLatestFrameImage();
        if (null != initialFrame) {
            setTreeProgression(initialFrame);
        }

        add(tabbedPane);
    }

    public static void applyDarkTheme() {
        // Disabled theme modifications to prevent com.apple.laf.AquaMenuPainter$RecyclableBorder.get() NPE on macOS.
        // The default native Look and Feel will be used.
    }

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

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public ImagePanel getTreeImagePanel() {
        return treeImagePanel;
    }

    @Override
    public void setTreeProgression(final Image image) {
        if (null == treeProgressionPanel) return;
        final Runnable updateRunnable = () -> {
            if (null == image) {
                treeProgressionLayout.show(treeProgressionPanel, "PLACEHOLDER");
            } else {
                treeImagePanel.setImage(image);
                fitTreeImageToViewport();
                treeProgressionLayout.show(treeProgressionPanel, "IMAGE");
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            updateRunnable.run();
        } else {
            SwingUtilities.invokeLater(updateRunnable);
        }
    }

    private void fitTreeImageToViewport() {

        treeImagePanel.fitTo(treeViewport.getExtentSize());
    }
}
