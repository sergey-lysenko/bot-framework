package works.lysenko.base.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.Base;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesLoader;
import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.parameters.PropertyValidator;
import works.lysenko.util.prop.PropEnum;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.chrs.__.BE;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.___.RAW;
import static works.lysenko.util.chrs.___.RUN;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.FORM;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.JSON;
import static works.lysenko.util.chrs.____.SAVE;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.chrs.____.VIEW;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.l;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.N.NOT_FOUND;
import static works.lysenko.util.lang.word.C.CANNOT;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.D.DESCRIPTION;
import static works.lysenko.util.lang.word.E.ENTRY;
import static works.lysenko.util.lang.word.F.FIXED;
import static works.lysenko.util.lang.word.I.ISSUE;
import static works.lysenko.util.lang.word.I.ISSUES;
import static works.lysenko.util.lang.word.K.KNOWN;
import static works.lysenko.util.lang.word.R.READING;
import static works.lysenko.util.lang.word.R.RELOAD;
import static works.lysenko.util.lang.word.S.SAVED;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.SELECTION;
import static works.lysenko.util.lang.word.S.SEVERITY;
import static works.lysenko.util.lang.word.S.SUCCESSFULLY;
import static works.lysenko.util.lang.word.S.SUITE;
import static works.lysenko.util.lang.word.T.TITLE;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.lang.word.V.VERIFICATION;
import static works.lysenko.util.lang.word.V.VERIFIED;
import static works.lysenko.util.lang.word.W.WARNING;
import static works.lysenko.util.spec.Layout.Files.FIXED_ISSUES;
import static works.lysenko.util.spec.Layout.Files.FIXED_ISSUES_;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES_;
import static works.lysenko.util.spec.Layout.Parts.SUITE_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Editor panel for managing {@code known/issues.json} and {@code known/fixed.issues.json} within the GUI.
 */
public final class KnownIssuesPanel extends JPanel {

    static {
        ControlPanel.initializeLookAndFeel();
    }

    private static final int PANEL_GAP = 10;
    private static final int FORM_INSET = 4;
    private static final int BTN_HGAP = 8;
    private static final int BTN_VGAP = 4;
    private static final int COL_SCENARIO = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_LINK = 3;
    private static final int COL_PATTERN = 4;
    private static final int COL_RESOLVED_AT = 5;
    private static final int COL_VERIFIED_IN_RUN = 6;
    private static final int COL_DECREASE = 7;

    private static final Color COLOR_PROMOTE = new Color(0xC0, 0x26, 0xD3);
    private static final Color COLOR_VERIFY = new Color(0x02, 0x84, 0xC7);

    private static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font FONT_ACTION_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private static final String WORD_EMPTY = works.lysenko.util.lang.word.E.EMPTY;
    private static final String WORD_ERROR = works.lysenko.util.lang.word.E.ERROR;
    private static final String WORD_LINK = "Link";
    private static final String WORD_PATTERN = "Pattern";
    private static final String WORD_RESOLVED = "Resolved";
    private static final String WORD_DECREASE = "Decrease";
    private static final String WORD_PROMOTE = "Promote";
    private static final String WORD_PROMOTED = "Promoted";
    private static final String WORD_PREPARE = "Prepare";
    private static final String WORD_SCHEMA = "schema";
    private static final String WORD_GENERATED_AUTO = "Generated automatically";

    private static final String ISSUES_JSON_FILE = s(ISSUES, _DOT_, JSON);
    private static final String FIXED_ISSUES_JSON_FILE = s(FIXED, _DOT_, ISSUES_JSON_FILE);
    private static final String MODE_ACTIVE = b("Active Issues", s(OPN_BRK, ISSUES_JSON_FILE, CLS_BRK));
    private static final String MODE_FIXED = b(c(FIXED), c(ISSUES), s(OPN_BRK, FIXED_ISSUES_JSON_FILE, CLS_BRK));

    private static final String COL_SCENARIO_NAME = c(SCENARIO);
    private static final String COL_TITLE_NAME = c(TITLE);
    private static final String COL_DESCRIPTION_NAME = c(DESCRIPTION);
    private static final String COL_RESOLVED_AT_NAME = b(WORD_RESOLVED, c(AT));
    private static final String COL_VERIFIED_IN_RUN_NAME = b(c(VERIFIED), c(IN), c(RUN));

    private static final String CARD_FORM = u(FORM);
    private static final String CARD_RAW = u(RAW);

    private static final String LABEL_DECREASE_TO_WARNING = b(WORD_DECREASE, SEVERITY, TO, WARNING);
    private static final String LABEL_PROMOTE_TO_FIXED = b(WORD_PROMOTE, TO, c(FIXED));
    private static final String TITLE_ISSUE_DETAILS = "Issue Details";
    private static final String LABEL_NEW = c(NEW);
    private static final String LABEL_SAVE_ENTRY = b(c(SAVE), c(ENTRY));
    private static final String LABEL_DELETE_ENTRY = b(c(DELETE), c(ENTRY));
    private static final String LABEL_RELOAD = c(RELOAD);
    private static final String LABEL_SAVE_FILE = b(c(SAVE), c(FILE));
    private static final String LABEL_RAW_JSON_VIEW = b(c(RAW), u(JSON), c(VIEW));
    private static final String LABEL_PREPARE_KNOWN = b(WORD_PREPARE, c(KNOWN), c(ISSUES), VERIFICATION);
    private static final String REGRESSION_SUITE = "Regression Suite";
    private static final String LABEL_PREPARE_REGRESSION = b(WORD_PREPARE, REGRESSION_SUITE);

    private static final String TOOLTIP_PROMOTE =
            "Move selected active issue to fixed.issues.json with current timestamp";
    private static final String TOOLTIP_PREPARE_KNOWN =
            "Generates KnownIssues.suite from issues.json and switches to Run Setup tab";
    private static final String TOOLTIP_PREPARE_REGRESSION =
            "Generates FixedIssues.suite from fixed.issues.json and switches to Run Setup tab";

    private static final String MSG_SCENARIO_EMPTY = s(b(c(SCENARIO), CANNOT, BE, WORD_EMPTY), _DOT_);
    private static final String TITLE_VALIDATION_ERROR = b(c(VALIDATION), c(WORD_ERROR));
    private static final String MSG_SELECT_ACTIVE_PROMOTE =
            "Select an active issue row to promote to fixed.issues.json.";
    private static final String TITLE_NO_SELECTION = b(c(NO), c(SELECTION));
    private static final String TITLE_PROMOTED_TO_FIXED = b(WORD_PROMOTED, TO, c(FIXED));
    private static final String TITLE_ERROR = c(WORD_ERROR);
    private static final String TITLE_FILE_ERROR = b(c(FILE), c(WORD_ERROR));
    private static final String TITLE_SAVED = c(SAVED);
    private static final String TITLE_SAVE_ERROR = b(c(SAVE), c(WORD_ERROR));
    private static final String TITLE_SUCCESS = "Success";

    private static final String KEY_RESOLVED_AT = s(l(WORD_RESOLVED), c(AT));
    private static final String MSG_PROMOTED_PREFIX = s(b(WORD_PROMOTED, ISSUE, TO), _SPACE_);
    private static final String MSG_RESOLVED_AT_INFIX = s(_SPACE_, OPN_BRK, KEY_RESOLVED_AT, _COLON_, _SPACE_);
    private static final String MSG_PROMOTED_SUFFIX = s(CLS_BRK, _DOT_);
    private static final String MSG_ERR_PROMOTING = s(b(c(WORD_ERROR), "promoting", ISSUE, TO, FIXED), _COLON_, _SPACE_);
    private static final String MSG_ERR_READING = s(b(c(WORD_ERROR), READING, FILE), _COLON_, _SPACE_);
    private static final String MSG_SAVED_PREFIX = s(b(c(ISSUES), SAVED, SUCCESSFULLY, TO), _SPACE_);
    private static final String MSG_ERR_SAVING = s(b(c(WORD_ERROR), "saving file"), _COLON_, _SPACE_);
    private static final String MSG_NOT_FOUND_SUFFIX = s(_SPACE_, NOT_FOUND);
    private static final String MSG_GENERATED_WITH = s(SUITE_PROPERTIES_EXTENSION, _SPACE_, "generated", _SPACE_, WITH, _SPACE_);
    private static final String MSG_SCENARIOS_SUFFIX = s(_SPACE_, SCENARIOS, _DOT_);
    private static final String MSG_ERR_PREPARING = s(b(c(WORD_ERROR), "preparing"), _SPACE_);
    private static final String MSG_SUITE_COLON = s(_SPACE_, SUITE, _COLON_, _SPACE_);

    private static final String SUITE_KNOWN_ISSUES = s(c(KNOWN), c(ISSUES));
    private static final String SUITE_FIXED_ISSUES = s(c(FIXED), c(ISSUES));
    private static final String HEADER_KNOWN_ISSUES =
            s(_NUMBR_, _SPACE_, b(WORD_GENERATED_AUTO, FROM, c(KNOWN), c(ISSUES)), _LFD_);
    private static final String HEADER_FIXED_ISSUES =
            s(_NUMBR_, _SPACE_, b(WORD_GENERATED_AUTO, FROM, c(FIXED), c(ISSUES),
                    s(OPN_BRK, REGRESSION_SUITE, CLS_BRK)), _LFD_);

    private final DefaultTableModel tableModel;
    private final JTextField scenarioField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JTextField linkField = new JTextField();
    private final JTextField patternField = new JTextField();
    private final JTextArea rawJsonArea = new JTextArea();
    private final PanelControls controls;

    /**
     * Constructs a new KnownIssuesPanel.
     */
    public KnownIssuesPanel() {
        setLayout(new BorderLayout(PANEL_GAP, PANEL_GAP));
        setBorder(BorderFactory.createEmptyBorder(PANEL_GAP, PANEL_GAP, PANEL_GAP, PANEL_GAP));

        tableModel = new ReadOnlyTableModel();
        controls = new PanelControls(tableModel, this::onTableSelectionChanged, this::promoteSelectedToFixed);

        final JPanel formPanel = buildFormPanel();
        final JPanel formViewPanel = new JPanel(new BorderLayout(0, PANEL_GAP));
        formViewPanel.add(new JScrollPane(controls.getTable()), BorderLayout.CENTER);
        formViewPanel.add(formPanel, BorderLayout.PAGE_END);

        ContextMenu.attach(
                scenarioField,
                titleField,
                descriptionField,
                linkField,
                patternField,
                controls.getResolvedAtField(),
                controls.getVerifiedInRunField(),
                rawJsonArea
        );

        rawJsonArea.setFont(FONT_MONO);
        controls.initCards(formViewPanel, new JScrollPane(rawJsonArea));
        add(controls.getCenterPanel(), BorderLayout.CENTER);

        final JPanel toolbar = buildToolbar();
        add(toolbar, BorderLayout.PAGE_START);

        controls.updateModeControls();
        loadFromFile();
    }

    private JPanel buildFormPanel() {
        final JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(TITLE_ISSUE_DETAILS));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(FORM_INSET, FORM_INSET, FORM_INSET, FORM_INSET);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(formPanel, gbc, row, s(COL_SCENARIO_NAME, _COLON_), scenarioField);
        row++;
        addFormRow(formPanel, gbc, row, s(COL_TITLE_NAME, _COLON_), titleField);
        row++;
        addFormRow(formPanel, gbc, row, s(COL_DESCRIPTION_NAME, _COLON_), descriptionField);
        row++;
        addFormRow(formPanel, gbc, row, s(WORD_LINK, _COLON_), linkField);
        row++;
        addFormRow(formPanel, gbc, row, s(WORD_PATTERN, _COLON_), patternField);
        row++;
        addFormRow(formPanel, gbc, row, s(COL_RESOLVED_AT_NAME, _COLON_), controls.getResolvedAtField());
        row++;
        addFormRow(formPanel, gbc, row, s(COL_VERIFIED_IN_RUN_NAME, _COLON_), controls.getVerifiedInRunField());
        row++;

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        formPanel.add(controls.getDecreaseToWarningBox(), gbc);
        row++;

        final JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, BTN_HGAP, BTN_VGAP));
        final JButton newBtn = new JButton(LABEL_NEW);
        newBtn.addActionListener(_ -> clearForm());

        final JButton saveEntryBtn = new JButton(LABEL_SAVE_ENTRY);
        saveEntryBtn.addActionListener(_ -> saveCurrentFormEntry());

        final JButton deleteEntryBtn = new JButton(LABEL_DELETE_ENTRY);
        deleteEntryBtn.addActionListener(_ -> deleteSelectedEntry());

        formButtons.add(newBtn);
        formButtons.add(saveEntryBtn);
        formButtons.add(deleteEntryBtn);
        formButtons.add(controls.getPromoteToFixedBtn());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        formPanel.add(formButtons, gbc);
        return formPanel;
    }

    private JPanel buildToolbar() {
        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, BTN_HGAP, BTN_VGAP));
        controls.getModeSelector().addActionListener(_ -> onModeChanged());

        final JButton reloadBtn = new JButton(LABEL_RELOAD);
        reloadBtn.addActionListener(_ -> loadFromFile());

        final JButton saveFileBtn = new JButton(LABEL_SAVE_FILE);
        saveFileBtn.addActionListener(_ -> saveToFile(true));

        final JToggleButton toggleViewBtn = new JToggleButton(LABEL_RAW_JSON_VIEW);
        toggleViewBtn.addActionListener(_ -> toggleView(toggleViewBtn));

        final JButton verifyKnownBtn = new JButton(LABEL_PREPARE_KNOWN);
        verifyKnownBtn.setFont(FONT_ACTION_BOLD);
        verifyKnownBtn.setForeground(COLOR_VERIFY);
        verifyKnownBtn.setToolTipText(TOOLTIP_PREPARE_KNOWN);
        verifyKnownBtn.addActionListener(_ -> prepareKnownIssuesVerification());

        final JButton prepareRegressionBtn = new JButton(LABEL_PREPARE_REGRESSION);
        prepareRegressionBtn.setFont(FONT_ACTION_BOLD);
        prepareRegressionBtn.setForeground(COLOR_PROMOTE);
        prepareRegressionBtn.setToolTipText(TOOLTIP_PREPARE_REGRESSION);
        prepareRegressionBtn.addActionListener(_ -> prepareRegressionSuite());

        toolbar.add(controls.getModeSelector());
        toolbar.add(reloadBtn);
        toolbar.add(saveFileBtn);
        toolbar.add(toggleViewBtn);
        toolbar.add(verifyKnownBtn);
        toolbar.add(prepareRegressionBtn);
        return toolbar;
    }

    private void onModeChanged() {
        controls.updateModeControls();
        loadFromFile();
    }

    private void toggleView(final AbstractButton toggleViewBtn) {
        if (toggleViewBtn.isSelected()) {
            syncTableToRawJson();
            controls.showCard(CARD_RAW);
        } else {
            syncRawJsonToTable();
            controls.showCard(CARD_FORM);
        }
    }

    private void onTableSelectionChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            populateFormFromSelectedRow();
        }
    }

    private static void addFormRow(final JPanel panel, final GridBagConstraints gbc, final int row,
                                   final String label, final JTextField textField) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(textField, gbc);
    }

    private void clearForm() {
        controls.clearSelectionAndFixedFields();
        scenarioField.setText(EMPTY);
        titleField.setText(EMPTY);
        descriptionField.setText(EMPTY);
        linkField.setText(EMPTY);
        patternField.setText(EMPTY);
    }

    private void populateFormFromSelectedRow() {
        final int selectedRow = controls.getSelectedRow();
        if (0 <= selectedRow && selectedRow < tableModel.getRowCount()) {
            scenarioField.setText(strAt(selectedRow, COL_SCENARIO));
            titleField.setText(strAt(selectedRow, COL_TITLE));
            descriptionField.setText(strAt(selectedRow, COL_DESCRIPTION));
            linkField.setText(strAt(selectedRow, COL_LINK));
            patternField.setText(strAt(selectedRow, COL_PATTERN));
            controls.populateFixedFields(
                    strAt(selectedRow, COL_RESOLVED_AT),
                    strAt(selectedRow, COL_VERIFIED_IN_RUN),
                    Boolean.parseBoolean(strAt(selectedRow, COL_DECREASE))
            );
        }
    }

    private String strAt(final int row, final int col) {
        final Object v = tableModel.getValueAt(row, col);
        return (null == v) ? EMPTY : v.toString();
    }

    private void saveCurrentFormEntry() {
        final String scenario = scenarioField.getText().strip();
        if (scenario.isEmpty()) {
            JOptionPane.showMessageDialog(this, MSG_SCENARIO_EMPTY, TITLE_VALIDATION_ERROR, JOptionPane.ERROR_MESSAGE);
        } else {
            final Object[] rowData = buildFormRowData(scenario);
            final int selectedRow = controls.getSelectedRow();
            if (0 <= selectedRow && selectedRow < tableModel.getRowCount()) {
                final int columnCount = rowData.length;
                for (int col = 0; col < columnCount; col++) {
                    tableModel.setValueAt(rowData[col], selectedRow, col);
                }
            } else {
                tableModel.addRow(rowData);
                final int lastRow = tableModel.getRowCount() - 1;
                controls.selectRow(lastRow);
            }
        }
    }

    private Object[] buildFormRowData(final String scenario) {
        final boolean fixed = controls.isFixedMode();
        return new Object[]{
                scenario,
                titleField.getText().strip(),
                descriptionField.getText().strip(),
                linkField.getText().strip(),
                patternField.getText().strip(),
                fixed ? controls.getResolvedAtText() : EMPTY,
                fixed ? controls.getVerifiedInRunText() : EMPTY,
                String.valueOf(!fixed && controls.isDecreaseSelected())
        };
    }

    private void deleteSelectedEntry() {
        final int selectedRow = controls.getSelectedRow();
        if (0 <= selectedRow && selectedRow < tableModel.getRowCount()) {
            tableModel.removeRow(selectedRow);
            clearForm();
        }
    }

    private void promoteSelectedToFixed() {
        if (!controls.isFixedMode()) {
            final int selectedRow = controls.getSelectedRow();
            if (0 > selectedRow || selectedRow >= tableModel.getRowCount()) {
                JOptionPane.showMessageDialog(this, MSG_SELECT_ACTIVE_PROMOTE, TITLE_NO_SELECTION,
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                executePromoteSelectedRow(selectedRow);
            }
        }
    }

    private void executePromoteSelectedRow(final int selectedRow) {
        try {
            if (isRawViewActive()) {
                syncRawJsonToTable();
            }
            final KnownIssue promoted = new KnownIssue(
                    strAt(selectedRow, COL_SCENARIO).strip(),
                    strAt(selectedRow, COL_TITLE).strip(),
                    strAt(selectedRow, COL_DESCRIPTION).strip(),
                    strAt(selectedRow, COL_LINK).strip(),
                    strAt(selectedRow, COL_PATTERN).strip(),
                    false,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                    EMPTY
            );

            final File fixedFile = IssueJsonSerializer.resolveFile(FIXED_ISSUES_, FIXED_ISSUES);
            IssueJsonSerializer.appendAndWriteFixedIssue(fixedFile, promoted);

            tableModel.removeRow(selectedRow);
            clearForm();
            saveToFile(false);

            JOptionPane.showMessageDialog(this,
                    s(MSG_PROMOTED_PREFIX, fixedFile.getPath(), MSG_RESOLVED_AT_INFIX,
                            promoted.resolvedAt(), MSG_PROMOTED_SUFFIX),
                    TITLE_PROMOTED_TO_FIXED, JOptionPane.INFORMATION_MESSAGE);
        } catch (final IOException ex) {
            JOptionPane.showMessageDialog(this, sn(MSG_ERR_PROMOTING, ex.getMessage()),
                    TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private File resolveTargetFile() {
        return controls.isFixedMode()
                ? IssueJsonSerializer.resolveFile(FIXED_ISSUES_, FIXED_ISSUES)
                : IssueJsonSerializer.resolveFile(KNOWN_ISSUES_, KNOWN_ISSUES);
    }

    private void loadFromFile() {
        tableModel.setRowCount(0);
        clearForm();
        rawJsonArea.setText(EMPTY);

        final File target = resolveTargetFile();
        if (target.exists()) {
            try (final InputStream is = Files.newInputStream(target.toPath())) {
                final List<KnownIssue> issues = controls.isFixedMode()
                        ? KnownIssuesLoader.loadFixedFromStream(is)
                        : KnownIssuesLoader.loadFromStream(is);
                appendIssuesToTable(issues);
                rawJsonArea.setText(Files.readString(target.toPath(), StandardCharsets.UTF_8));
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(this, sn(MSG_ERR_READING, e.getMessage()),
                        TITLE_FILE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void appendIssuesToTable(final Iterable<KnownIssue> issues) {
        issues.forEach(this::addIssueRow);
    }

    private void addIssueRow(final KnownIssue ki) {
        tableModel.addRow(new Object[]{
                ki.scenario(),
                ki.title(),
                ki.description(),
                ki.link(),
                ki.pattern(),
                ki.resolvedAt(),
                ki.verifiedInRun(),
                String.valueOf(ki.decreaseToWarning())
        });
    }

    private void saveToFile(final boolean showMessage) {
        final File target = resolveTargetFile();
        try {
            if (isRawViewActive()) {
                syncRawJsonToTable();
            }
            final boolean fixed = controls.isFixedMode();
            final String json = IssueJsonSerializer.writeIssuesToFile(target, collectTableRows(fixed), fixed);
            rawJsonArea.setText(json);
            if (showMessage) {
                JOptionPane.showMessageDialog(this, s(MSG_SAVED_PREFIX, target.getPath()),
                        TITLE_SAVED, JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(this, sn(MSG_ERR_SAVING, e.getMessage()),
                    TITLE_SAVE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isRawViewActive() {
        return rawJsonArea.isShowing();
    }

    private Collection<KnownIssue> collectTableRows(final boolean fixed) {
        return IntStream.range(0, tableModel.getRowCount())
                .mapToObj(i -> rowToKnownIssue(i, fixed))
                .toList();
    }

    private KnownIssue rowToKnownIssue(final int rowIndex, final boolean fixed) {
        return new KnownIssue(
                strAt(rowIndex, COL_SCENARIO).strip(),
                strAt(rowIndex, COL_TITLE).strip(),
                strAt(rowIndex, COL_DESCRIPTION).strip(),
                strAt(rowIndex, COL_LINK).strip(),
                strAt(rowIndex, COL_PATTERN).strip(),
                !fixed && Boolean.parseBoolean(strAt(rowIndex, COL_DECREASE)),
                fixed ? strAt(rowIndex, COL_RESOLVED_AT).strip() : EMPTY,
                fixed ? strAt(rowIndex, COL_VERIFIED_IN_RUN).strip() : EMPTY
        );
    }

    private void syncTableToRawJson() {
        try {
            final boolean fixed = controls.isFixedMode();
            rawJsonArea.setText(IssueJsonSerializer.serialize(collectTableRows(fixed), fixed));
        } catch (final JsonProcessingException _) {
            // Keep existing raw editor content when serialization fails.
        }
    }

    private void syncRawJsonToTable() {
        try {
            final String json = rawJsonArea.getText();
            tableModel.setRowCount(0);
            clearForm();
            if (isNotNull(json) && !json.isBlank()) {
                final InputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                final List<KnownIssue> issues = controls.isFixedMode()
                        ? KnownIssuesLoader.loadFixedFromStream(bais)
                        : KnownIssuesLoader.loadFromStream(bais);
                appendIssuesToTable(issues);
            }
        } catch (final RuntimeException _) {
            // Ignore malformed JSON while switching views.
        }
    }

    private void prepareKnownIssuesVerification() {
        if (!controls.isFixedMode()) {
            if (isRawViewActive()) {
                syncRawJsonToTable();
            }
            saveToFile(false);
        }
        prepareVerificationSuite(IssueJsonSerializer.resolveFile(KNOWN_ISSUES_, KNOWN_ISSUES),
                SUITE_KNOWN_ISSUES, HEADER_KNOWN_ISSUES);
    }

    private void prepareRegressionSuite() {
        if (controls.isFixedMode()) {
            if (isRawViewActive()) {
                syncRawJsonToTable();
            }
            saveToFile(false);
        }
        prepareVerificationSuite(IssueJsonSerializer.resolveFile(FIXED_ISSUES_, FIXED_ISSUES),
                SUITE_FIXED_ISSUES, HEADER_FIXED_ISSUES);
    }

    private void prepareVerificationSuite(final File target, final String suiteName, final String headerComment) {
        try {
            if (target.exists()) {
                final int scenarioCount = VerificationSuiteBuilder.generateSuite(target, suiteName, headerComment);
                final ControlPanel controlPanel = ControlPanel.getInstance();
                if (null != controlPanel) {
                    controlPanel.getGui().selectTestSuite(suiteName);
                    controlPanel.getTabbedPane().setSelectedIndex(1);
                }
                JOptionPane.showMessageDialog(this,
                        s(suiteName, MSG_GENERATED_WITH, scenarioCount, MSG_SCENARIOS_SUFFIX),
                        TITLE_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, s(target.getName(), MSG_NOT_FOUND_SUFFIX),
                        TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        } catch (final IOException ex) {
            JOptionPane.showMessageDialog(this,
                    sn(MSG_ERR_PREPARING, suiteName, MSG_SUITE_COLON, ex.getMessage()),
                    TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class ReadOnlyTableModel extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNS = {
                COL_SCENARIO_NAME,
                COL_TITLE_NAME,
                COL_DESCRIPTION_NAME,
                WORD_LINK,
                WORD_PATTERN,
                COL_RESOLVED_AT_NAME,
                COL_VERIFIED_IN_RUN_NAME,
                WORD_DECREASE
        };

        private ReadOnlyTableModel() {
            super(COLUMNS, 0);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }

    private static final class PanelControls {

        private final JComboBox<String> modeSelector = new JComboBox<>(new String[]{MODE_ACTIVE, MODE_FIXED});
        private final JTable table;
        private final JTextField resolvedAtField = new JTextField();
        private final JTextField verifiedInRunField = new JTextField();
        private final JCheckBox decreaseToWarningBox = new JCheckBox(LABEL_DECREASE_TO_WARNING);
        private final JButton promoteToFixedBtn = new JButton(LABEL_PROMOTE_TO_FIXED);
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel centerPanel = new JPanel(cardLayout);

        private PanelControls(final DefaultTableModel tableModel,
                              final javax.swing.event.ListSelectionListener selectionListener,
                              final Runnable onPromote) {
            table = new JTable(tableModel);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(selectionListener);

            promoteToFixedBtn.setForeground(COLOR_PROMOTE);
            promoteToFixedBtn.setToolTipText(TOOLTIP_PROMOTE);
            promoteToFixedBtn.addActionListener(_ -> onPromote.run());
        }

        private void initCards(final JComponent formView, final JComponent rawView) {
            centerPanel.add(formView, CARD_FORM);
            centerPanel.add(rawView, CARD_RAW);
        }

        private void showCard(final String cardName) {
            cardLayout.show(centerPanel, cardName);
        }

        private boolean isFixedMode() {
            return Objects.equals(MODE_FIXED, modeSelector.getSelectedItem());
        }

        private void updateModeControls() {
            final boolean fixed = isFixedMode();
            resolvedAtField.setEnabled(fixed);
            verifiedInRunField.setEnabled(fixed);
            decreaseToWarningBox.setEnabled(!fixed);
            if (fixed) {
                decreaseToWarningBox.setSelected(false);
            }
            promoteToFixedBtn.setEnabled(!fixed);
        }

        private void clearSelectionAndFixedFields() {
            table.clearSelection();
            resolvedAtField.setText(EMPTY);
            verifiedInRunField.setText(EMPTY);
            decreaseToWarningBox.setSelected(false);
        }

        private void populateFixedFields(final String resolvedAt, final String verifiedInRun, final boolean decrease) {
            resolvedAtField.setText(resolvedAt);
            verifiedInRunField.setText(verifiedInRun);
            decreaseToWarningBox.setSelected(!isFixedMode() && decrease);
        }

        private int getSelectedRow() {
            return table.getSelectedRow();
        }

        private void selectRow(final int row) {
            table.setRowSelectionInterval(row, row);
        }

        private String getResolvedAtText() {
            return resolvedAtField.getText().strip();
        }

        private String getVerifiedInRunText() {
            return verifiedInRunField.getText().strip();
        }

        private boolean isDecreaseSelected() {
            return decreaseToWarningBox.isSelected();
        }

        private JComboBox<String> getModeSelector() {
            return modeSelector;
        }

        private JTable getTable() {
            return table;
        }

        private JTextField getResolvedAtField() {
            return resolvedAtField;
        }

        private JTextField getVerifiedInRunField() {
            return verifiedInRunField;
        }

        private JCheckBox getDecreaseToWarningBox() {
            return decreaseToWarningBox;
        }

        private JButton getPromoteToFixedBtn() {
            return promoteToFixedBtn;
        }

        private JPanel getCenterPanel() {
            return centerPanel;
        }
    }

    private static final class IssueJsonSerializer {

        private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        private static final String GROUP_SEP = s(VRT_BAR, VRT_BAR);
        private static final String KEY_SCHEMA = s(_DOLLAR, WORD_SCHEMA);
        private static final String SCHEMA_PATH = s(_DOT_, _SLASH_, ISSUES, _DOT_, WORD_SCHEMA, _DOT_, JSON);
        private static final int ROOT_ENTRIES = 2;
        private static final int INITIAL_FIXED_CAPACITY = 16;

        private static File resolveFile(final String srcPath, final String rootPath) {
            final File srcFile = new File(srcPath);
            final File rootFile = new File(rootPath);
            return (!srcFile.exists() && rootFile.exists()) ? rootFile : srcFile;
        }

        private static void writeFileEnsuringParent(final Path path, final CharSequence content) throws IOException {
            final Path parent = path.getParent();
            if (null != parent) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }

        private static void appendAndWriteFixedIssue(final File fixedFile, final KnownIssue promoted)
                throws IOException {
            final Collection<KnownIssue> fixedList = new ArrayList<>(INITIAL_FIXED_CAPACITY);
            if (fixedFile.exists()) {
                try (final InputStream is = Files.newInputStream(fixedFile.toPath())) {
                    fixedList.addAll(KnownIssuesLoader.loadFixedFromStream(is));
                }
            }
            fixedList.add(promoted);
            writeFileEnsuringParent(fixedFile.toPath(), serialize(fixedList, true));
        }

        private static String writeIssuesToFile(final File target, final Collection<KnownIssue> issues,
                                                final boolean fixedMode) throws IOException {
            final String json = serialize(issues, fixedMode);
            writeFileEnsuringParent(target.toPath(), json);
            return json;
        }

        private static String serialize(final Collection<KnownIssue> issues, final boolean fixedMode)
                throws JsonProcessingException {
            final Map<String, IssueGroup> groups = new LinkedHashMap<>(issues.size());
            for (final KnownIssue ki : issues) {
                addIssueToGroups(groups, ki, fixedMode);
            }

            final List<Map<String, Object>> serialized = groups.values().stream()
                    .map(group -> group.toJsonMap(fixedMode))
                    .toList();

            final Map<String, Object> root = new LinkedHashMap<>(ROOT_ENTRIES);
            root.put(KEY_SCHEMA, SCHEMA_PATH);
            root.put(ISSUES, serialized);
            return MAPPER.writeValueAsString(root);
        }

        private static void addIssueToGroups(final Map<? super String, IssueGroup> groups, final KnownIssue ki,
                                             final boolean fixedMode) {
            final String scVal = ki.scenario().strip();
            final String titleVal = ki.title().strip();
            final String descVal = ki.description().strip();
            final String linkVal = ki.link().strip();
            final String patternVal = ki.pattern().strip();
            final String resolvedAtVal = ki.resolvedAt().strip();
            final String verifiedInRunVal = ki.verifiedInRun().strip();
            final boolean decrease = !fixedMode && ki.decreaseToWarning();

            final String groupKey = s(titleVal, GROUP_SEP, linkVal, GROUP_SEP, patternVal, GROUP_SEP, descVal,
                    GROUP_SEP, resolvedAtVal, GROUP_SEP, verifiedInRunVal, GROUP_SEP, decrease);

            final IssueGroup existing = groups.get(groupKey);
            if (null == existing) {
                final IssueGroup created = new IssueGroup(titleVal, descVal, linkVal, patternVal,
                        decrease, resolvedAtVal, verifiedInRunVal);
                created.addInitialScenarios(scVal);
                groups.put(groupKey, created);
            } else {
                existing.mergeScenarios(scVal);
            }
        }
    }

    private static final class IssueGroup {

        private static final String COMMA = s(_COMMA_);
        private static final String KEY_LINK = l(WORD_LINK);
        private static final String KEY_PATTERN = l(WORD_PATTERN);
        private static final String KEY_DECREASE = b(_DASH_, l(WORD_DECREASE), TO, WARNING);
        private static final String KEY_VERIFIED_IN_RUN = s(VERIFIED, c(IN), c(RUN));
        private static final int ITEM_CAPACITY = 8;

        private final SequencedCollection<String> scenarios = new ArrayList<>(2);
        private final String title;
        private final String description;
        private final String link;
        private final String pattern;
        private final boolean decrease;
        private final String resolvedAt;
        private final String verifiedInRun;
        private boolean singleEntry;

        private IssueGroup(final String title, final String description, final String link,
                           final String pattern, final boolean decrease, final String resolvedAt,
                           final String verifiedInRun) {
            this.title = title;
            this.description = description;
            this.link = link;
            this.pattern = pattern;
            this.decrease = decrease;
            this.resolvedAt = resolvedAt;
            this.verifiedInRun = verifiedInRun;
            singleEntry = true;
        }

        private void addInitialScenarios(final String rawScenario) {
            if (rawScenario.contains(COMMA)) {
                addSplitTokens(rawScenario);
            } else if (!rawScenario.isBlank()) {
                scenarios.add(rawScenario);
            }
        }

        private void mergeScenarios(final String rawScenario) {
            singleEntry = false;
            if (rawScenario.contains(COMMA)) {
                addSplitTokens(rawScenario);
            } else {
                addIfPresentAndUnique(rawScenario);
            }
        }

        private void addSplitTokens(final String rawScenario) {
            for (final String token : rawScenario.split(COMMA)) {
                addIfPresentAndUnique(token.strip());
            }
        }

        private void addIfPresentAndUnique(final String value) {
            if (!value.isBlank() && !scenarios.contains(value)) {
                scenarios.add(value);
            }
        }

        private Map<String, Object> toJsonMap(final boolean fixedMode) {
            final Map<String, Object> item = new LinkedHashMap<>(ITEM_CAPACITY);
            if (1 < scenarios.size()) {
                item.put(SCENARIOS, scenarios);
            } else if (1 == scenarios.size()) {
                item.put(SCENARIO, scenarios.getFirst());
            } else if (singleEntry) {
                item.put(SCENARIO, EMPTY);
            }
            item.put(TITLE, title);
            item.put(DESCRIPTION, description);
            item.put(KEY_LINK, link);
            item.put(KEY_PATTERN, pattern);
            if (decrease) {
                item.put(KEY_DECREASE, Boolean.TRUE);
            }
            if (fixedMode && !resolvedAt.isEmpty()) {
                item.put(KEY_RESOLVED_AT, resolvedAt);
            }
            if (fixedMode && !verifiedInRun.isEmpty()) {
                item.put(KEY_VERIFIED_IN_RUN, verifiedInRun);
            }
            return item;
        }
    }

    private static final class VerificationSuiteBuilder {

        private static final int INITIAL_SUITE_CAPACITY = 256;
        private static final String DOT = s(_DOT_);
        private static final String DEFAULT_PACKAGE_PREFIX = s(Base.class.getPackageName(), _DOT_);
        private static final String SUITE_EXHAUSTIVE_LINE = s(PropEnum._TEST_EXHAUSTIVE.getPropertyName(), _EQUAL_, TRUE);
        private static final String SUITE_PASSES_LINE = s(PropEnum._TEST_EXHAUSTIVE_PASSES.getPropertyName(), _EQUAL_, _1_);
        private static final String SUITE_RESILIENT_LINE = s(PropEnum._TEST_RESILIENT_MODE.getPropertyName(), _EQUAL_, TRUE);
        private static final String SUITE_WEIGHT_ONE = s(_EQUAL_, _1_);

        private static int generateSuite(final File target, final String suiteName, final String headerComment)
                throws IOException {
            final List<KnownIssue> issues;
            try (final InputStream is = Files.newInputStream(target.toPath())) {
                issues = new KnownIssuesStore(is).getIssues();
            }

            final Map<String, String> canonical = collectCanonicalScenarios(PropertyValidator.getAllKnownProperties());
            final Set<String> matchingScenarios = findMatchingScenarios(issues, canonical.values());

            final Path suitePath = Path.of(_SUITES_, s(suiteName, SUITE_PROPERTIES_EXTENSION));
            final String content = buildSuiteContent(headerComment, matchingScenarios);
            IssueJsonSerializer.writeFileEnsuringParent(suitePath, content);
            return matchingScenarios.size();
        }

        private static Map<String, String> collectCanonicalScenarios(final Collection<String> allProperties) {
            final Map<String, String> canonical = new LinkedHashMap<>(allProperties.size());
            for (final String prop : allProperties) {
                if (!prop.isEmpty() && _DOT_ != prop.charAt(0)) {
                    putPreferredCanonical(canonical, prop);
                }
            }
            return canonical;
        }

        private static void putPreferredCanonical(final Map<? super String, String> canonical, final String prop) {
            final String lowerKey = prop.toLowerCase(Locale.ROOT);
            final String existing = canonical.get(lowerKey);
            if (null == existing) {
                canonical.put(lowerKey, prop);
            } else if (Character.isUpperCase(existing.charAt(0)) && Character.isLowerCase(prop.charAt(0))) {
                canonical.put(lowerKey, prop);
            }
        }

        private static Set<String> findMatchingScenarios(final Iterable<KnownIssue> issues,
                                                         final Iterable<String> canonicalValues) {
            final Set<String> matching = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final String prefix = resolvePackagePrefix();
            for (final KnownIssue ki : issues) {
                final String pattern = ki.scenario();
                if (null != pattern && !pattern.isBlank()) {
                    collectMatchesForPattern(pattern, prefix, canonicalValues, matching);
                }
            }
            return matching;
        }

        private static String resolvePackagePrefix() {
            final String rootPackage = PropEnum._TEST_ROOT.get();
            final boolean hasCustomRoot = null != rootPackage && !rootPackage.isEmpty() && !Objects.equals(DOT, rootPackage);
            return hasCustomRoot ? s(rootPackage, _DOT_) : DEFAULT_PACKAGE_PREFIX;
        }

        private static void collectMatchesForPattern(final String pattern, final String prefix,
                                                     final Iterable<String> canonicalValues,
                                                     final Collection<? super String> matching) {
            for (final String prop : canonicalValues) {
                final String fqn = s(prefix, prop);
                final int lastDot = prop.lastIndexOf(_DOT_);
                final String simpleName = (0 <= lastDot) ? prop.substring(lastDot + 1) : prop;
                if (KnownIssuesMatcher.matchesScenario(pattern, fqn, simpleName)) {
                    matching.add(prop);
                }
            }
        }

        private static String buildSuiteContent(final String headerComment, final Iterable<String> matchingScenarios) {
            final StringBuilder suiteContent = new StringBuilder(INITIAL_SUITE_CAPACITY);
            suiteContent.append(headerComment);
            suiteContent.append(SUITE_EXHAUSTIVE_LINE).append(_LFD_);
            suiteContent.append(SUITE_PASSES_LINE).append(_LFD_);
            suiteContent.append(SUITE_RESILIENT_LINE).append(_LFD_);

            for (final String scenario : matchingScenarios) {
                suiteContent.append(scenario).append(SUITE_WEIGHT_ONE).append(_LFD_);
            }
            return suiteContent.toString();
        }
    }
}
