package works.lysenko.base.ui;

import works.lysenko.base.parameters.PropertyHelp;
import works.lysenko.base.parameters.PropertyValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.DE;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.ALL;
import static works.lysenko.util.chrs.___.DOTS;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.KEY;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.PART;
import static works.lysenko.util.chrs.____.SAVE;
import static works.lysenko.util.chrs.____.THIS;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.A.AVAILABLE;
import static works.lysenko.util.lang.word.C.CLEAR;
import static works.lysenko.util.lang.word.C.CONFIG;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.E.ENTRY;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.I.INCLUDE;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.S.SEARCH;
import static works.lysenko.util.lang.word.S.SEE;
import static works.lysenko.util.lang.word.S.SELECT;
import static works.lysenko.util.lang.word.S.SUITES;
import static works.lysenko.util.lang.word.W.WEIGHT;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_PARTS_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Reusable panel presenting a searchable, filterable property table with an edit form
 * and optional property help pane.
 */
public final class PropertyTablePanel extends JPanel {

    private static final int TABLE_ROW_HEIGHT = 20;
    private static final int SEARCH_FIELD_COLUMNS = 12;
    private static final int SEARCH_EXTRA_STRUT = 12;
    private static final int LAYOUT_GAP = 8;
    private static final int COMPACT_GAP = 6;
    private static final int SMALL_GAP = 4;
    private static final int TINY_GAP = 2;
    private static final double HELP_SPLIT_RATIO = 0.78;
    private static final int PARTS_DIALOG_WIDTH = 380;
    private static final int PARTS_DIALOG_HEIGHT = 240;

    private static final Color COLOR_VALIDATION_ERROR = new Color(0xDC, 0x26, 0x26);
    private static final Color COLOR_PROPERTY_HINT = new Color(0x25, 0x63, 0xEB);
    private static final Color COLOR_ERROR_BG = new Color(0xFE, 0xF2, 0xF2);

    private static final Font FONT_ERROR = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Font FONT_HINT = new Font(Font.SANS_SERIF, Font.ITALIC, 11);
    private static final Font FONT_PARTS_BTN = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    private static final String DOT_INCLUDE = s(_DOT_, INCLUDE);
    private static final String COMMA = s(_COMMA_);
    private static final String UI_TEXT_FIELD_BG = "TextField.background";
    private static final String WARNING_PREFIX = "⚠️ ";
    private static final String LABEL_SELECT_PARTS = b(c(SELECT), s(c(PART), S, DOTS));
    private static final String LABEL_NEW_ENTRY = b(c(NEW), c(ENTRY));
    private static final String LABEL_SAVE_ENTRY = b(c(SAVE), c(ENTRY));
    private static final String LABEL_DELETE_ENTRY = b(c(DELETE), c(ENTRY));
    private static final String LABEL_SEARCH = s(c(SEARCH), _COLON_);
    private static final String LABEL_CLEAR = c(CLEAR);
    private static final String LABEL_KEY_PROPERTY = s(b(c(KEY), s(_SLASH_), c(PROPERTY)), _COLON_);
    private static final String LABEL_VALUE_WEIGHT = s(b(c(VALUE), s(_SLASH_), c(WEIGHT)), _COLON_);
    private static final String LABEL_SELECT_ALL = b(c(SELECT), c(ALL));
    private static final String LABEL_DESELECT_ALL = b(s(c(DE), SELECT), c(ALL));
    private static final String TITLE_PROPERTY_DETAILS = "Property Details";
    private static final String HELP_WORD = "help";
    private static final String TITLE_HELP = c(HELP_WORD);
    private static final String MSG_SELECT_HELP = s(b(c(SELECT), s(A), PROPERTY, TO, SEE, HELP_WORD), _DOT_);
    private static final String MSG_NO_HELP = s(b(c(NO), HELP_WORD, IS, AVAILABLE, FOR, THIS, PROPERTY), _DOT_);
    private static final String MSG_NO_PARTS_FOUND = s(b(c(NO), TEST_PROPERTIES_PARTS_EXTENSION, s(FILE, S), FOUND), _DOT_);
    private static final String TITLE_NO_PARTS_FOUND = b(c(NO), s(c(PART), S), c(FOUND));
    private static final String TITLE_SELECT_PARTS_DIALOG = b(c(SELECT), TEST_PROPERTIES_PARTS_EXTENSION, s(c(FILE), S), TO, c(INCLUDE));
    private static final String ALLOWED_PREFIX = s(_SPACE_, c(ALLOWED), _COLON_, _SPACE_);

    private PartsMode partsMode = PartsMode.CONFIG_ONLY;

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final SearchControls search;
    private final FormControls form;
    private final HelpControls help;
    private final int keyColIdx;
    private final int valColIdx;

    /**
     * Constructs a new PropertyTablePanel.
     *
     * @param tableModel           the tableModel
     * @param keyColIdx            the keyColIdx
     * @param valColIdx            the valColIdx
     * @param showHelp             the showHelp
     * @param extraSearchComponent the extraSearchComponent
     */
    public PropertyTablePanel(final DefaultTableModel tableModel, final int keyColIdx, final int valColIdx,
                              final boolean showHelp, final JComponent extraSearchComponent) {
        this.tableModel = tableModel;
        this.keyColIdx = keyColIdx;
        this.valColIdx = valColIdx;
        setLayout(new BorderLayout(LAYOUT_GAP, LAYOUT_GAP));

        table = new JTable(tableModel);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        sorter = new TableRowSorter<>(tableModel);
        configureColumnComparators();
        table.setRowSorter(sorter);

        search = new SearchControls(extraSearchComponent, this::applySearchFilter);
        add(search.getTopContainer(), BorderLayout.PAGE_START);

        form = new FormControls(this::showPartsSelectionDialog, this::performLiveValidation);
        help = new HelpControls();

        table.getSelectionModel().addListSelectionListener(e -> onRowSelectionChanged(e, showHelp));
        tableModel.addTableModelListener(this::onTableModelChanged);

        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(new EmptyBorder(0, LAYOUT_GAP, 0, LAYOUT_GAP));

        final JPanel tableAndForm = new JPanel(new BorderLayout());
        tableAndForm.add(tableScrollPane, BorderLayout.CENTER);
        tableAndForm.add(form.buildPanel(this::clearForm), BorderLayout.PAGE_END);

        if (showHelp) {
            final JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableAndForm, help.buildPanel());
            content.setResizeWeight(HELP_SPLIT_RATIO);
            content.setDividerLocation(HELP_SPLIT_RATIO);
            content.setBorder(null);
            add(content, BorderLayout.CENTER);
        } else {
            add(tableAndForm, BorderLayout.CENTER);
        }
    }

    private void configureColumnComparators() {
        final Comparator<String> caseComparator = Comparator.nullsFirst(Comparator.naturalOrder());
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (String.class.equals(tableModel.getColumnClass(i))) {
                sorter.setComparator(i, caseComparator);
            }
        }
    }

    private void onRowSelectionChanged(final ListSelectionEvent e, final boolean showHelp) {
        if (!e.getValueIsAdjusting()) {
            populateFormFromSelectedRow();
            if (showHelp) {
                updateHelpPanel();
            }
        }
    }

    private void onTableModelChanged(final TableModelEvent e) {
        if (!form.isUpdatingForm()) {
            final int row = e.getFirstRow();
            if (0 <= row && row < tableModel.getRowCount()) {
                populateFormFromSelectedRow();
            }
        }
    }

    void setFormSaveAction(final ActionListener listener) {
        form.addSaveActionListener(listener);
    }

    void setFormDeleteAction(final ActionListener listener) {
        form.addDeleteActionListener(listener);
    }

    void setSearchAndFiltersVisible(final boolean visible) {
        search.setVisible(visible);
    }

    /**
     * Returns the underlying property table.
     *
     * @return the property table
     */
    public JTable getTable() {
        return table;
    }

    /**
     * Returns the search text field.
     *
     * @return the search text field
     */
    public JTextField getSearchField() {
        return search.getSearchField();
    }

    AutoCompleteComboBox getKeyField() {
        return form.getKeyField();
    }

    JTextField getValueField() {
        return form.getValueField();
    }

    /**
     * Returns the help title label.
     *
     * @return the help title label
     */
    public JLabel getHelpTitle() {
        return help.getHelpTitle();
    }

    /**
     * Returns the help description text area.
     *
     * @return the help description text area
     */
    public JTextArea getHelpText() {
        return help.getHelpText();
    }

    private void applySearchFilter() {
        final String text = search.getSearchField().getText().strip();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            final Pattern pattern = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
            sorter.setRowFilter(new PropertyRowFilter(pattern, keyColIdx));
        }
    }

    void performLiveValidation() {
        final String key = form.getKeyText();
        final String val = form.getValueText();
        final String lowerKey = key.toLowerCase(Locale.ROOT);
        final boolean isInclude = lowerKey.startsWith(DOT_INCLUDE) || Objects.equals(INCLUDE, lowerKey);
        form.setSelectPartsVisible(isInclude);

        final List<String> allowedValues = PropertyValidator.validValues(key);
        if (allowedValues.isEmpty()) {
            form.hideHint();
        } else {
            form.showHint(s(ALLOWED_PREFIX, String.join(COMMA_SPACE, allowedValues)));
        }

        final String err = key.isEmpty() ? null : PropertyValidator.validateProperty(key, val, null);
        if (isNotNull(err)) {
            form.showValidationError(s(WARNING_PREFIX, err));
        } else {
            form.clearValidationError();
        }
    }

    private static void scanPartFiles(final Path baseDir, final Collection<? super String> relativePaths) {
        if (!isNull(baseDir) && Files.exists(baseDir) && Files.isDirectory(baseDir)) {
            try (final var stream = Files.walk(baseDir)) {
                stream.filter(PropertyTablePanel::isPartFile)
                        .forEach(p -> collectPartFile(baseDir, p, relativePaths));
            } catch (final IOException _) {
                // Ignore unreadable directories when scanning optional part files.
            }
        }
    }

    private static boolean isPartFile(final Path path) {
        return path.toString().endsWith(TEST_PROPERTIES_PARTS_EXTENSION) && Files.isRegularFile(path);
    }

    private static void collectPartFile(final Path baseDir, final Path partPath,
                                        final Collection<? super String> relativePaths) {
        final Path relative = baseDir.relativize(partPath);
        String relStr = relative.toString().replace(REW_SOL, _SLASH_);
        if (relStr.endsWith(TEST_PROPERTIES_PARTS_EXTENSION)) {
            relStr = relStr.substring(0, relStr.length() - TEST_PROPERTIES_PARTS_EXTENSION.length());
        }
        relativePaths.add(relStr);
    }

    /**
     * Sets the parts scanning mode used by the "Select Parts..." dialog.
     *
     * @param mode the parts mode
     */
    public void setPartsMode(final PartsMode mode) {
        partsMode = isNotNull(mode) ? mode : PartsMode.CONFIG_ONLY;
    }

    PartsMode getPartsMode() {
        return partsMode;
    }

    private void showPartsSelectionDialog() {
        final Set<String> relevantParts = collectRelevantParts();
        if (relevantParts.isEmpty()) {
            JOptionPane.showMessageDialog(this, MSG_NO_PARTS_FOUND, TITLE_NO_PARTS_FOUND,
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            final Set<String> currentlySelected = parseSelectedParts(form.getValueText());
            final Collection<JCheckBox> allCheckBoxes = new ArrayList<>(relevantParts.size());
            final JComponent contentComponent = createTabPanel(relevantParts, currentlySelected, allCheckBoxes);
            contentComponent.setPreferredSize(new Dimension(PARTS_DIALOG_WIDTH, PARTS_DIALOG_HEIGHT));

            final int res = JOptionPane.showConfirmDialog(
                    this,
                    contentComponent,
                    TITLE_SELECT_PARTS_DIALOG,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (JOptionPane.OK_OPTION == res) {
                final List<String> chosen = allCheckBoxes.stream()
                        .filter(AbstractButton::isSelected)
                        .map(AbstractButton::getText)
                        .toList();
                form.setValueText(String.join(COMMA, chosen));
                performLiveValidation();
            }
        }
    }

    private Set<String> collectRelevantParts() {
        final Set<String> relevantParts = new TreeSet<>();
        if (PartsMode.CONFIG_ONLY == partsMode) {
            scanPartFiles(Path.of(_CONFIG_), relevantParts);
            if (relevantParts.isEmpty()) {
                scanPartFiles(Path.of(CONFIG), relevantParts);
            }
        } else {
            scanPartFiles(Path.of(_SUITES_), relevantParts);
            if (relevantParts.isEmpty()) {
                scanPartFiles(Path.of(SUITES), relevantParts);
            }
        }
        return relevantParts;
    }

    private static Set<String> parseSelectedParts(final String currentVal) {
        final Set<String> selected;
        if (currentVal.isEmpty()) {
            selected = new HashSet<>(0);
        } else {
            final String[] parts = currentVal.split(COMMA);
            selected = HashSet.newHashSet(parts.length);
            for (final String part : parts) {
                selected.add(part.strip());
            }
        }
        return selected;
    }

    private static JPanel createTabPanel(final Collection<String> parts, final Collection<String> selected,
                                         final Collection<? super JCheckBox> globalList) {
        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
        final Collection<JCheckBox> localList = new ArrayList<>(parts.size());

        for (final String part : parts) {
            final JCheckBox checkBox = createPartCheckBox(part, selected.contains(part));
            localList.add(checkBox);
            globalList.add(checkBox);
            listPanel.add(checkBox);
        }

        return wrapPartsPanel(listPanel, localList);
    }

    private static JCheckBox createPartCheckBox(final String part, final boolean isSelected) {
        final JCheckBox checkBox = new JCheckBox(part);
        checkBox.setSelected(isSelected);
        return checkBox;
    }

    private static JPanel wrapPartsPanel(final JPanel listPanel, final Iterable<? extends AbstractButton> localList) {
        final JScrollPane scrollPane = new JScrollPane(listPanel);
        final JPanel panel = new JPanel(new BorderLayout(0, COMPACT_GAP));

        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, SMALL_GAP, TINY_GAP));
        final JButton selectAllBtn = new JButton(LABEL_SELECT_ALL);
        selectAllBtn.addActionListener(_ -> localList.forEach(cb -> cb.setSelected(true)));
        final JButton deselectAllBtn = new JButton(LABEL_DESELECT_ALL);
        deselectAllBtn.addActionListener(_ -> localList.forEach(cb -> cb.setSelected(false)));
        toolbar.add(selectAllBtn);
        toolbar.add(deselectAllBtn);

        panel.add(toolbar, BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    void clearForm() {
        table.clearSelection();
        form.clear();
        performLiveValidation();
    }

    private void populateFormFromSelectedRow() {
        final int viewRow = table.getSelectedRow();
        if (0 <= viewRow) {
            final int modelRow = table.convertRowIndexToModel(viewRow);
            if (0 <= modelRow && modelRow < tableModel.getRowCount()) {
                form.populate(
                        (String) tableModel.getValueAt(modelRow, keyColIdx),
                        (String) tableModel.getValueAt(modelRow, valColIdx)
                );
                performLiveValidation();
            }
        } else {
            form.enableKeyField();
        }
    }

    private void updateHelpPanel() {
        final int viewRow = table.getSelectedRow();
        if (0 > viewRow) {
            help.reset();
        } else {
            final int modelRow = table.convertRowIndexToModel(viewRow);
            final String key = (String) tableModel.getValueAt(modelRow, keyColIdx);
            final String description = PropertyHelp.getDescription(key);
            help.show(key, description);
        }
    }

    /**
     * Defines which directory hierarchy is scanned for {@code .part} files.
     */
    public enum PartsMode {
        /** Scan configuration {@code .part} files under {@code config/}. */
        CONFIG_ONLY,
        /** Scan suite {@code .part} files under {@code suites/}. */
        SUITES_ONLY
    }

    private record SimpleDocumentListener(Runnable onChange) implements DocumentListener {

        @Override
        public void insertUpdate(final DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            onChange.run();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            onChange.run();
        }
    }

    private static final class PropertyRowFilter extends RowFilter<DefaultTableModel, Integer> {

        private final Pattern pattern;
        private final int keyColIdx;

        private PropertyRowFilter(final Pattern pattern, final int keyColIdx) {
            this.pattern = pattern;
            this.keyColIdx = keyColIdx;
        }

        @Override
        public boolean include(final Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            final Matcher matcher = pattern.matcher(EMPTY);
            boolean matched = false;
            for (int column = 0; column < entry.getValueCount() && !matched; column++) {
                if (matcher.reset(String.valueOf(entry.getValue(column))).find()) {
                    matched = true;
                }
            }
            if (!matched) {
                final String key = String.valueOf(entry.getValue(keyColIdx));
                final String description = PropertyHelp.getDescription(key);
                matched = null != description && matcher.reset(description).find();
            }
            return matched;
        }
    }

    private static final class SearchControls {

        private static final Insets COMPACT_INSETS = new Insets(TINY_GAP, COMPACT_GAP, TINY_GAP, COMPACT_GAP);
        private static final String[] QUICK_FILTERS = {
                ".data", ".grid", ".logs", ".math", ".test", ".tree",
                ".webd", ".mobi", ".swipes", ".progression", ".screenshots"
        };

        private final JTextField searchField = new JTextField(SEARCH_FIELD_COLUMNS);
        private final JPanel topContainer = new JPanel(new BorderLayout());

        private SearchControls(final JComponent extraSearchComponent, final Runnable onFilterChanged) {
            final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, LAYOUT_GAP, LAYOUT_GAP));
            searchPanel.add(new JLabel(LABEL_SEARCH));
            final JButton clearBtn = new JButton(LABEL_CLEAR);
            clearBtn.setMargin(COMPACT_INSETS);
            clearBtn.addActionListener(_ -> searchField.setText(EMPTY));
            searchPanel.add(searchField);
            searchPanel.add(clearBtn);
            if (null != extraSearchComponent) {
                searchPanel.add(Box.createHorizontalStrut(SEARCH_EXTRA_STRUT));
                searchPanel.add(extraSearchComponent);
            }
            topContainer.add(searchPanel, BorderLayout.PAGE_START);
            topContainer.add(createQuickFiltersPanel(), BorderLayout.PAGE_END);
            searchField.getDocument().addDocumentListener(new SimpleDocumentListener(onFilterChanged));
        }

        private JPanel createQuickFiltersPanel() {
            final JPanel quickFiltersPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, SMALL_GAP, SMALL_GAP));
            for (final String filter : QUICK_FILTERS) {
                quickFiltersPanel.add(createQuickFilterButton(filter));
            }
            return quickFiltersPanel;
        }

        private JButton createQuickFilterButton(final String filter) {
            final JButton filterBtn = new JButton(filter);
            filterBtn.setMargin(COMPACT_INSETS);
            filterBtn.addActionListener(_ -> searchField.setText(filter));
            return filterBtn;
        }

        private JPanel getTopContainer() {
            return topContainer;
        }

        private JTextField getSearchField() {
            return searchField;
        }

        private void setVisible(final boolean visible) {
            topContainer.setVisible(visible);
        }
    }

    private static final class FormControls {

        private final AutoCompleteComboBox keyField = new AutoCompleteComboBox(PropertyValidator.getAllKnownProperties());
        private final JTextField valueField = new JTextField();
        private final JButton selectPartsBtn = new JButton(LABEL_SELECT_PARTS);
        private final JLabel liveValidationErrorLabel = new JLabel();
        private final JLabel propertyHintLabel = new JLabel();
        private final JButton saveEntryBtn = new JButton(LABEL_SAVE_ENTRY);
        private final JButton deleteEntryBtn = new JButton(LABEL_DELETE_ENTRY);
        private boolean updatingForm;

        private FormControls(final Runnable onSelectParts, final Runnable onFieldChange) {
            updatingForm = false;
            liveValidationErrorLabel.setForeground(COLOR_VALIDATION_ERROR);
            liveValidationErrorLabel.setFont(FONT_ERROR);
            liveValidationErrorLabel.setVisible(false);
            propertyHintLabel.setForeground(COLOR_PROPERTY_HINT);
            propertyHintLabel.setFont(FONT_HINT);
            propertyHintLabel.setVisible(false);

            selectPartsBtn.setFont(FONT_PARTS_BTN);
            selectPartsBtn.setVisible(false);
            selectPartsBtn.addActionListener(_ -> onSelectParts.run());

            final DocumentListener listener = new SimpleDocumentListener(onFieldChange);
            final JTextComponent editor = (JTextComponent) keyField.getEditor().getEditorComponent();
            editor.getDocument().addDocumentListener(listener);
            valueField.getDocument().addDocumentListener(listener);
        }

        private JPanel buildPanel(final Runnable onClear) {
            final JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createTitledBorder(TITLE_PROPERTY_DETAILS));
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(SMALL_GAP, SMALL_GAP, SMALL_GAP, SMALL_GAP);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.0;
            formPanel.add(new JLabel(LABEL_KEY_PROPERTY), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            formPanel.add(keyField, gbc);
            row++;

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.0;
            formPanel.add(new JLabel(LABEL_VALUE_WEIGHT), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            final JPanel valueContainer = new JPanel(new BorderLayout(COMPACT_GAP, 0));
            valueContainer.add(valueField, BorderLayout.CENTER);
            valueContainer.add(selectPartsBtn, BorderLayout.LINE_END);
            formPanel.add(valueContainer, gbc);
            row++;

            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.weightx = 1.0;
            final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            infoPanel.add(liveValidationErrorLabel);
            infoPanel.add(propertyHintLabel);
            formPanel.add(infoPanel, gbc);
            row++;

            final JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, LAYOUT_GAP, SMALL_GAP));
            final JButton newEntryBtn = new JButton(LABEL_NEW_ENTRY);
            newEntryBtn.addActionListener(_ -> onClear.run());
            formButtons.add(newEntryBtn);
            formButtons.add(saveEntryBtn);
            formButtons.add(deleteEntryBtn);

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 2;
            formPanel.add(formButtons, gbc);
            return formPanel;
        }

        private void addSaveActionListener(final ActionListener listener) {
            saveEntryBtn.addActionListener(listener);
        }

        private void addDeleteActionListener(final ActionListener listener) {
            deleteEntryBtn.addActionListener(listener);
        }

        private AutoCompleteComboBox getKeyField() {
            return keyField;
        }

        private JTextField getValueField() {
            return valueField;
        }

        private String getKeyText() {
            return keyField.getText().strip();
        }

        private String getValueText() {
            return valueField.getText().strip();
        }

        private void setValueText(final String value) {
            valueField.setText(value);
        }

        private void setSelectPartsVisible(final boolean visible) {
            selectPartsBtn.setVisible(visible);
        }

        private void showHint(final String text) {
            propertyHintLabel.setText(text);
            propertyHintLabel.setVisible(true);
        }

        private void hideHint() {
            propertyHintLabel.setVisible(false);
        }

        private void showValidationError(final String text) {
            liveValidationErrorLabel.setText(text);
            liveValidationErrorLabel.setVisible(true);
            valueField.setBackground(COLOR_ERROR_BG);
        }

        private void clearValidationError() {
            liveValidationErrorLabel.setVisible(false);
            valueField.setBackground(UIManager.getColor(UI_TEXT_FIELD_BG));
        }

        private void clear() {
            keyField.setEnabled(true);
            keyField.setText(EMPTY);
            valueField.setText(EMPTY);
        }

        private void enableKeyField() {
            keyField.setEnabled(true);
        }

        private boolean isUpdatingForm() {
            return updatingForm;
        }

        private void populate(final String key, final String value) {
            updatingForm = true;
            try {
                keyField.setText(key);
                valueField.setText(value);
                keyField.setEnabled(false);
            } finally {
                updatingForm = false;
            }
        }
    }

    private static final class HelpControls {

        private final JLabel helpTitle = new JLabel(MSG_SELECT_HELP);
        private final JTextArea helpText = new JTextArea();

        private JPanel buildPanel() {
            helpText.setEditable(false);
            helpText.setLineWrap(true);
            helpText.setWrapStyleWord(true);
            helpText.setFocusable(false);
            helpText.setOpaque(false);
            final JPanel helpPanel = new JPanel(new BorderLayout(SMALL_GAP, SMALL_GAP));
            helpPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(TITLE_HELP),
                    new EmptyBorder(SMALL_GAP, LAYOUT_GAP, LAYOUT_GAP, LAYOUT_GAP)));
            helpPanel.add(helpTitle, BorderLayout.PAGE_START);
            helpPanel.add(new JScrollPane(helpText), BorderLayout.CENTER);
            return helpPanel;
        }

        private JLabel getHelpTitle() {
            return helpTitle;
        }

        private JTextArea getHelpText() {
            return helpText;
        }

        private void reset() {
            helpTitle.setText(MSG_SELECT_HELP);
            helpText.setText(EMPTY);
        }

        private void show(final String key, final String description) {
            helpTitle.setText(key);
            helpText.setText((null == description || description.isBlank()) ? MSG_NO_HELP : description);
            helpText.setCaretPosition(0);
        }
    }
}
