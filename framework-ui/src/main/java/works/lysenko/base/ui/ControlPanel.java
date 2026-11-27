package works.lysenko.base.ui;

import works.lysenko.base.output.ScenarioTracker;
import works.lysenko.base.output.TreeTracker;
import works.lysenko.base.parameters.Gui;
import works.lysenko.base.parameters.PropertiesPanel;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis.parameters._TestParameters;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.data.enums.ExitCode;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static works.lysenko.util.chrs.__.CO;
import static works.lysenko.util.chrs.__.EC;
import static works.lysenko.util.chrs.__.EL;
import static works.lysenko.util.chrs.__.EX;
import static works.lysenko.util.chrs.__.MB;
import static works.lysenko.util.chrs.__.NI;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.NT;
import static works.lysenko.util.chrs.__.OG;
import static works.lysenko.util.chrs.__.PR;
import static works.lysenko.util.chrs.__.RE;
import static works.lysenko.util.chrs.__.RO;
import static works.lysenko.util.chrs.__.SS;
import static works.lysenko.util.chrs.__.UP;
import static works.lysenko.util.chrs.__.US;
import static works.lysenko.util.chrs.___.ION;
import static works.lysenko.util.chrs.___.PAN;
import static works.lysenko.util.chrs.___.RUN;
import static works.lysenko.util.chrs.___.SET;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.AVAILABLE;
import static works.lysenko.util.lang.word.C.CANCEL;
import static works.lysenko.util.lang.word.C.CONFIG;
import static works.lysenko.util.lang.word.E.EDITOR;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.F.FRAME;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.K.KNOWN;
import static works.lysenko.util.lang.word.L.LAYOUT;
import static works.lysenko.util.lang.word.P.PREVIEW;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.START;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.T.TREE;
import static works.lysenko.util.lang.word.Y.YET;
import static works.lysenko.util.spec.Symbols.L;
import static works.lysenko.util.spec.Symbols._AMPRS_;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The ControlPanel class.
 */
public final class ControlPanel extends JFrame implements _Dashboard {

    private static final String NIMBUS_LAF = s(c(NI), MB, US);
    private static final String PROGRESSION = s(PR, OG, RE, SS, ION);
    private static final String TITLE_CONTROL_PANEL = b(s(c(CO), NT, RO, L), s(c(PAN), EL));
    private static final String TAB_CONFIG_TESTS = b(c(CONFIG), s(_AMPRS_), c(TESTS));
    private static final String TAB_RUN_SETUP = b(c(RUN), s(c(SET), UP));
    private static final String TAB_EXECUTION = c(EXECUTION);
    private static final String TAB_TREE_PROGRESSION = b(c(TREE), c(PROGRESSION));
    private static final String TAB_SCENARIO_PROGRESSION = b(c(SCENARIO), c(PROGRESSION));
    private static final String TAB_TREE_LAYOUT_EDITOR = b(c(TREE), c(LAYOUT), c(EDITOR));
    private static final String TAB_KNOWN_ISSUES = b(c(KNOWN), c(ISSUES));
    private static final String LABEL_CANCEL = s(_SPACE_, c(CANCEL), _SPACE_);
    private static final String LABEL_START_EXEC = s(_SPACE_, _SPACE_, b(c(START), s(_AMPRS_), s(c(EX), EC)), _SPACE_, _SPACE_);
    private static final String LABEL_START_TREE = s(_SPACE_, _SPACE_, b(c(START), s(_AMPRS_), c(TREE)), _SPACE_, _SPACE_);
    private static final String EMPTY_TREE_MSG = s(b(c(NO), TREE, PROGRESSION, FRAME, AVAILABLE, YET), _DOT_);
    private static final String EMPTY_SCENARIO_MSG = s(b(c(NO), SCENARIO, PROGRESSION, FRAME, AVAILABLE, YET), _DOT_);
    private static final String TREE_PREVIEW_ID = d(TREE, PROGRESSION, PREVIEW);
    private static final String SCENARIO_PREVIEW_ID = d(SCENARIO, PROGRESSION, PREVIEW);
    private static final String[] FOCUS_INPUT_MAP_KEYS = {
            "TextField.focusInputMap",
            "TextArea.focusInputMap",
            "TextPane.focusInputMap",
            "EditorPane.focusInputMap",
            "PasswordField.focusInputMap",
            "FormattedTextField.focusInputMap"
    };
    private static final int WINDOW_WIDTH = 1600;
    private static final int WINDOW_HEIGHT = 900;
    private static final double SPLIT_DIVIDER_RATIO = 0.382;
    private static final int BUTTON_FONT_SIZE = 13;
    private static final int GUI_BORDER_PADDING = 10;
    private static final int RUN_PANEL_HGAP = 12;
    private static final int RUN_PANEL_VGAP = 10;
    private static final int RUN_SETUP_TAB_INDEX = 1;
    private static final int EXEC_TAB_INDEX = 2;
    private static final int TREE_TAB_INDEX = 3;
    private static final Color START_BUTTON_COLOR = new Color(0x16, 0xA3, 0x4A);

    static {
        initializeLookAndFeel();
    }

    private static final AtomicReference<ControlPanel> INSTANCE = new AtomicReference<>();

    private final JTabbedPane tabbedPane;
    private final Gui gui;
    private final PropertiesPanel propertiesPanel;
    private final UserInterface dashboardPanel;
    private final SvgPreviewPanel treePreview;
    private final SvgPreviewPanel scenarioPreview;
    private final CountDownLatch runLatch = new CountDownLatch(1);

    /**
     * Constructs a new ControlPanel.
     *
     * @param parameters  the parameters
     * @param guiInstance the guiInstance
     */
    public ControlPanel(final _TestParameters parameters, final Gui guiInstance) {
        applyMenuShortcutKeyMask();
        setTitle(TITLE_CONTROL_PANEL);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // 1. Config & Tests Tab (Micro-IDE)
        final ConfigTestsEditorPanel configTestsPanel = new ConfigTestsEditorPanel();
        tabbedPane.addTab(TAB_CONFIG_TESTS, configTestsPanel);

        // 2. Configuration Tab (Run Preparation)
        gui = guiInstance;
        final JPanel coveragePanel = gui.createCoverageEstimatePanel();
        propertiesPanel = new PropertiesPanel(
                parameters.getTest(),
                parameters.isHeadless(),
                parameters.isExhaustive(),
                parameters.getExhaustivePasses(),
                coveragePanel
        );
        tabbedPane.addTab(TAB_RUN_SETUP, createRunSetupPanel());

        // 3. Execution Tab (Dashboard)
        dashboardPanel = new UserInterface(null);
        tabbedPane.addTab(TAB_EXECUTION, dashboardPanel);

        // 4. Tree Progression Tab
        treePreview = new SvgPreviewPanel(EMPTY_TREE_MSG, TREE_PREVIEW_ID);
        tabbedPane.addTab(TAB_TREE_PROGRESSION, treePreview);

        // 5. Scenario Progression Tab
        scenarioPreview = new SvgPreviewPanel(EMPTY_SCENARIO_MSG, SCENARIO_PREVIEW_ID);
        tabbedPane.addTab(TAB_SCENARIO_PROGRESSION, scenarioPreview);

        // 6. Tree Layout Editor
        final TreeLayoutEditorPanel treeLayoutPanel = new TreeLayoutEditorPanel();
        tabbedPane.addTab(TAB_TREE_LAYOUT_EDITOR, treeLayoutPanel);

        // 7. Known Issues Tab
        final KnownIssuesPanel knownIssuesPanel = new KnownIssuesPanel();
        tabbedPane.addTab(TAB_KNOWN_ISSUES, knownIssuesPanel);

        tabbedPane.setSelectedIndex(RUN_SETUP_TAB_INDEX);
        tabbedPane.addChangeListener(_ -> onTabChanged(configTestsPanel, parameters));

        loadInitialPreviews();
        add(tabbedPane);
        INSTANCE.set(this);
    }

    /**
     * Initializes the Nimbus Look and Feel and maps menu shortcut key masks globally
     * for all standard Swing text components.
     */
    public static void initializeLookAndFeel() {
        try {
            if (null == UIManager.getLookAndFeel() || !Objects.equals(NIMBUS_LAF, UIManager.getLookAndFeel().getName())) {
                final Optional<String> nimbusClass = findNimbusClassName();
                if (nimbusClass.isPresent()) {
                    UIManager.setLookAndFeel(nimbusClass.get());
                }
            }
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException _) {
            // Fall back to the default Look and Feel when Nimbus is unavailable.
        }
        applyMenuShortcutKeyMask();
    }

    private static Optional<String> findNimbusClassName() {
        return Arrays.stream(UIManager.getInstalledLookAndFeels())
                .filter(info -> Objects.equals(NIMBUS_LAF, info.getName()))
                .map(UIManager.LookAndFeelInfo::getClassName)
                .findFirst();
    }

    /**
     * Maps the platform's default menu shortcut key mask (Command on macOS, Control on others)
     * globally in UIManager for all text editing actions (Copy, Paste, Cut, Select All).
     */
    private static void applyMenuShortcutKeyMask() {
        try {
            final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            for (final String mapKey : FOCUS_INPUT_MAP_KEYS) {
                final InputMap im = (InputMap) UIManager.get(mapKey);
                if (null != im) {
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask), DefaultEditorKit.copyAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask), DefaultEditorKit.pasteAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask), DefaultEditorKit.cutAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask), DefaultEditorKit.selectAllAction);
                }
            }
        } catch (final HeadlessException _) {
            // Headless environments do not provide a toolkit shortcut key mask.
        }
    }

    /**
     * Attaches a right-click context menu (Cut, Copy, Paste, Select All) to the given text components.
     *
     * @param components the text components to attach the context menu to
     */
    public static void addContextMenu(final JTextComponent... components) {
        ContextMenu.attach(components);
    }

    /**
     * Performs get instance.
     *
     * @return the get instance result
     */
    public static ControlPanel getInstance() {
        return INSTANCE.get();
    }

    /**
     * Performs apply dark theme.
     */
    public static void applyDarkTheme() {
        // Disabled theme modifications to prevent com.apple.laf.AquaMenuPainter$RecyclableBorder.get() NPE on macOS.
        // The default native Look and Feel will be used.
    }

    private JPanel createRunSetupPanel() {
        final JPanel configPanel = new JPanel(new BorderLayout());
        final JPanel guiBox = gui.parametersPanel();
        guiBox.setBorder(BorderFactory.createEmptyBorder(
                GUI_BORDER_PADDING, GUI_BORDER_PADDING, GUI_BORDER_PADDING, GUI_BORDER_PADDING));

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guiBox, propertiesPanel);
        // Properties panel width / Gui panel width = 1.618  =>  Gui = 1 / 2.618 ≈ 0.3819
        splitPane.setDividerLocation(SPLIT_DIVIDER_RATIO);

        configPanel.add(splitPane, BorderLayout.CENTER);
        configPanel.add(createRunPanel(), BorderLayout.PAGE_END);
        return configPanel;
    }

    private JPanel createRunPanel() {
        final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, RUN_PANEL_HGAP, RUN_PANEL_VGAP));

        final JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, BUTTON_FONT_SIZE));
        cancelButton.addActionListener(_ -> cancelRun());

        runPanel.add(cancelButton);
        runPanel.add(createStartButton(LABEL_START_EXEC, EXEC_TAB_INDEX));
        runPanel.add(createStartButton(LABEL_START_TREE, TREE_TAB_INDEX));
        return runPanel;
    }

    private JButton createStartButton(final String label, final int targetTabIndex) {
        final JButton button = new JButton(label);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, BUTTON_FONT_SIZE));
        button.setBackground(START_BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(_ -> startRun(targetTabIndex));
        return button;
    }

    private void onTabChanged(final ConfigTestsEditorPanel configTestsPanel, final _TestParameters parameters) {
        if (RUN_SETUP_TAB_INDEX == tabbedPane.getSelectedIndex()) {
            configTestsPanel.promptToSaveIfModified();
            gui.refreshTestDropdown();
            propertiesPanel.reloadFromDisk(
                    parameters.getTest(),
                    parameters.isHeadless(),
                    parameters.isExhaustive(),
                    parameters.getExhaustivePasses()
            );
        }
    }

    private void loadInitialPreviews() {
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
    }

    /**
     * Performs display and wait.
     */
    public void displayAndWait() {
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
        try {
            runLatch.await();
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private void cancelRun() {
        dispose();
        _Test.processCode(ExitCode.CLOSED_THROUGH_GUI);
    }

    private void startRun(final int targetTabIndex) {
        gui.propagateUserInput();
        propertiesPanel.applyChanges();
        tabbedPane.setSelectedIndex(targetTabIndex);
        tabbedPane.setEnabledAt(0, false);
        tabbedPane.setEnabledAt(RUN_SETUP_TAB_INDEX, false);
        runLatch.countDown();
    }

    // Delegate _Dashboard methods to dashboardPanel
    @Override
    public void setBreadcrumb(final List<? extends _Scenario> content) {
        dashboardPanel.setBreadcrumb(content);
    }

    @Override
    public void setInfo(final String message, final Telemetry telemetry) {
        dashboardPanel.setInfo(message, telemetry);
    }

    @Override
    public boolean isHalt() {
        return dashboardPanel.isHalt();
    }

    @Override
    public void setHalt(final boolean active) {
        dashboardPanel.setHalt(active);
    }

    @Override
    public boolean isPause() {
        return dashboardPanel.isPause();
    }

    @Override
    public void setPause(final boolean active) {
        dashboardPanel.setPause(active);
    }

    @Override
    public boolean isStop() {
        return dashboardPanel.isStop();
    }

    @Override
    public void setStop(final boolean active) {
        dashboardPanel.setStop(active);
    }

    @Override
    public boolean isDebug() {
        return dashboardPanel.isDebug();
    }

    @Override
    public void setDebug(final boolean active) {
        dashboardPanel.setDebug(active);
    }

    /**
     * Performs get tabbed pane.
     *
     * @return the get tabbed pane result
     */
    JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Performs get gui.
     *
     * @return the get gui result
     */
    Gui getGui() {
        return gui;
    }

    /**
     * Performs get tree image panel.
     *
     * @return the get tree image panel result
     */
    ImagePanel getTreeImagePanel() {
        return treePreview.getImagePanel();
    }

    /**
     * Performs get scenario image panel.
     *
     * @return the panel that paints the scenario progression frame
     */
    ImagePanel getScenarioImagePanel() {
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
