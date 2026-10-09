package works.lysenko.base.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Editor panel for managing known_issues.json within the GUI.
 */
public class KnownIssuesPanel extends JPanel {

    static {
        ControlPanel.initializeLookAndFeel();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final String DEFAULT_FILE_PATH = "src/main/resources/known_issues.json";

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextField scenarioField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JTextField linkField = new JTextField();
    private final JTextField patternField = new JTextField();
    private final JTextArea rawJsonArea = new JTextArea();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(cardLayout);

    public KnownIssuesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final String[] columns = {"Scenario", "Title", "Description", "Link", "Pattern"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        final JScrollPane tableScrollPane = new JScrollPane(table);

        final JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Known Issue Details"));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Scenario:", scenarioField);
        addFormRow(formPanel, gbc, row++, "Title:", titleField);
        addFormRow(formPanel, gbc, row++, "Description:", descriptionField);
        addFormRow(formPanel, gbc, row++, "Link:", linkField);
        addFormRow(formPanel, gbc, row++, "Pattern:", patternField);

        final JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        final JButton newBtn = new JButton("New");
        newBtn.addActionListener(e -> clearForm());

        final JButton saveEntryBtn = new JButton("Save Entry");
        saveEntryBtn.addActionListener(e -> saveCurrentFormEntry());

        final JButton deleteEntryBtn = new JButton("Delete Entry");
        deleteEntryBtn.addActionListener(e -> deleteSelectedEntry());

        formButtons.add(newBtn);
        formButtons.add(saveEntryBtn);
        formButtons.add(deleteEntryBtn);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        formPanel.add(formButtons, gbc);

        final JPanel formViewPanel = new JPanel(new BorderLayout(0, 10));
        formViewPanel.add(tableScrollPane, BorderLayout.CENTER);
        formViewPanel.add(formPanel, BorderLayout.SOUTH);

        ContextMenu.attach(
                scenarioField,
                titleField,
                descriptionField,
                linkField,
                patternField,
                rawJsonArea
        );

        rawJsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        final JScrollPane jsonScrollPane = new JScrollPane(rawJsonArea);

        centerPanel.add(formViewPanel, "FORM");
        centerPanel.add(jsonScrollPane, "RAW");

        add(centerPanel, BorderLayout.CENTER);

        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        final JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> loadFromFile());

        final JButton saveFileBtn = new JButton("Save File");
        saveFileBtn.addActionListener(e -> saveToFile());

        final JToggleButton toggleViewBtn = new JToggleButton("Raw JSON View");
        toggleViewBtn.addActionListener(e -> {
            if (toggleViewBtn.isSelected()) {
                syncTableToRawJson();
                cardLayout.show(centerPanel, "RAW");
            } else {
                syncRawJsonToTable();
                cardLayout.show(centerPanel, "FORM");
            }
        });

        toolbar.add(reloadBtn);
        toolbar.add(saveFileBtn);
        toolbar.add(toggleViewBtn);

        add(toolbar, BorderLayout.NORTH);

        loadFromFile();
    }

    private static void addFormRow(final JPanel panel, final GridBagConstraints gbc, final int row, final String label, final JTextField textField) {
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
        table.clearSelection();
        scenarioField.setText("");
        titleField.setText("");
        descriptionField.setText("");
        linkField.setText("");
        patternField.setText("");
    }

    private void populateFormFromSelectedRow() {
        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            scenarioField.setText((String) tableModel.getValueAt(selectedRow, 0));
            titleField.setText((String) tableModel.getValueAt(selectedRow, 1));
            descriptionField.setText((String) tableModel.getValueAt(selectedRow, 2));
            linkField.setText((String) tableModel.getValueAt(selectedRow, 3));
            patternField.setText((String) tableModel.getValueAt(selectedRow, 4));
        }
    }

    private void saveCurrentFormEntry() {
        final String scenario = scenarioField.getText().trim();
        if (scenario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Scenario cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Object[] rowData = {
                scenario,
                titleField.getText().trim(),
                descriptionField.getText().trim(),
                linkField.getText().trim(),
                patternField.getText().trim()
        };

        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            for (int col = 0; col < rowData.length; col++) {
                tableModel.setValueAt(rowData[col], selectedRow, col);
            }
        } else {
            tableModel.addRow(rowData);
            table.setRowSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
        }
    }

    private void deleteSelectedEntry() {
        final int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            tableModel.removeRow(selectedRow);
            clearForm();
        }
    }

    private File resolveTargetFile() {
        final File srcFile = new File(DEFAULT_FILE_PATH);
        if (srcFile.exists()) return srcFile;
        final File rootFile = new File("known_issues.json");
        if (rootFile.exists()) return rootFile;
        return srcFile;
    }

    public void loadFromFile() {
        tableModel.setRowCount(0);
        clearForm();

        final File target = resolveTargetFile();
        if (target.exists()) {
            try {
                final KnownIssuesStore store = new KnownIssuesStore(Files.newInputStream(target.toPath()));
                for (final KnownIssue ki : store.getIssues()) {
                    tableModel.addRow(new Object[]{
                            ki.scenario(),
                            ki.title(),
                            ki.description(),
                            ki.link(),
                            ki.pattern()
                    });
                }
                rawJsonArea.setText(Files.readString(target.toPath(), StandardCharsets.UTF_8));
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void saveToFile() {
        final File target = resolveTargetFile();
        try {
            if (isRawViewActive()) {
                syncRawJsonToTable();
            }
            final String json = generateJsonString();
            if (isNotNull(target.getParentFile())) {
                target.getParentFile().mkdirs();
            }
            Files.writeString(target.toPath(), json, StandardCharsets.UTF_8);
            rawJsonArea.setText(json);
            JOptionPane.showMessageDialog(this, "Known issues saved successfully to " + target.getPath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isRawViewActive() {
        return rawJsonArea.isShowing();
    }

    private String generateJsonString() throws Exception {
        final List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final Map<String, String> item = new LinkedHashMap<>();
            item.put("scenario", (String) tableModel.getValueAt(i, 0));
            item.put("title", (String) tableModel.getValueAt(i, 1));
            item.put("description", (String) tableModel.getValueAt(i, 2));
            item.put("link", (String) tableModel.getValueAt(i, 3));
            item.put("pattern", (String) tableModel.getValueAt(i, 4));
            list.add(item);
        }

        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("issues", list);
        return MAPPER.writeValueAsString(root);
    }

    private void syncTableToRawJson() {
        try {
            rawJsonArea.setText(generateJsonString());
        } catch (final Exception ignored) {
        }
    }

    private void syncRawJsonToTable() {
        try {
            final String json = rawJsonArea.getText();
            if (isNotNull(json) && !json.isBlank()) {
                final KnownIssuesStore store = new KnownIssuesStore(new java.io.ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
                tableModel.setRowCount(0);
                for (final KnownIssue ki : store.getIssues()) {
                    tableModel.addRow(new Object[]{
                            ki.scenario(),
                            ki.title(),
                            ki.description(),
                            ki.link(),
                            ki.pattern()
                    });
                }
            }
        } catch (final Exception ignored) {
        }
    }
}
