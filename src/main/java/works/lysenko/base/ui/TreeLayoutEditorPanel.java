package works.lysenko.base.ui;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.tree.TreeLayoutComputer;
import works.lysenko.tree.Tree;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.func.core.ClassLoader;
import works.lysenko.util.func.type.Files;
import works.lysenko.util.prop.tree.VisualOffset;
import works.lysenko.util.prop.tree.VisualRowOffset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;

public class TreeLayoutEditorPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField = new JTextField(16);
    private final Map<String, _Scenario> scenarioMap = new TreeMap<>();
    private final Map<String, Integer> initialColumnOffsets = new TreeMap<>();
    private final Map<String, Integer> initialRowOffsets = new TreeMap<>();

    public TreeLayoutEditorPanel() {
        setLayout(new BorderLayout(8, 8));

        tableModel = new DefaultTableModel(new Object[]{"Scenario Class", "Scenario Name", "Visual Offset", "Visual Row Offset"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex >= 2 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(20);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        sorter = new TableRowSorter<>(tableModel);
        final java.util.Comparator<String> caseSensitiveComparator = java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder());
        sorter.setComparator(0, caseSensitiveComparator);
        sorter.setComparator(1, caseSensitiveComparator);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);

        // Search Panel
        final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchPanel.add(new JLabel("Search:"));
        final JButton clearBtn = new JButton("Clear");
        clearBtn.setMargin(new Insets(2, 6, 2, 6));
        clearBtn.addActionListener(e -> searchField.setText(""));
        searchPanel.add(searchField);
        searchPanel.add(clearBtn);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                final String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });

        add(searchPanel, BorderLayout.NORTH);

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        add(scrollPane, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        final JButton reloadBtn = new JButton("Reload from Sources");
        reloadBtn.addActionListener(e -> loadScenarios());

        final JButton previewBtn = new JButton("Preview in Browser");
        previewBtn.addActionListener(e -> preview());

        final JButton saveBtn = new JButton("Save to Sources");
        saveBtn.addActionListener(e -> saveToSources());

        buttonPanel.add(reloadBtn);
        buttonPanel.add(previewBtn);
        buttonPanel.add(saveBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        loadScenarios();
    }

    private void loadScenarios() {
        tableModel.setRowCount(0);
        scenarioMap.clear();
        initialColumnOffsets.clear();
        initialRowOffsets.clear();

        final String rootPackage = works.lysenko.util.prop.tree.Scenario.root;
        if (rootPackage == null || rootPackage.isBlank()) return;

        final Set<_Scenario> scenarios = ClassLoader.readFrom(rootPackage, false);
        for (_Scenario s : scenarios) {
            addScenarioRecursively(s);
        }

        for (Map.Entry<String, _Scenario> entry : scenarioMap.entrySet()) {
            final _Scenario sc = entry.getValue();
            int colOffset = 0;
            int rowOffset = 0;

            final String userDir = System.getProperty("user.dir");
            final Path sourcePath = Paths.get(userDir, "src", "main", "java", sc.getClass().getName().replace('.', '/') + ".java");
            final File sourceFile = sourcePath.toFile();
            if (sourceFile.exists()) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(sourcePath)) {
                        if (line.matches("^\\s*@VisualOffset\\([-+]?\\d+\\).*")) {
                            String num = line.replaceAll("[^0-9-]", "");
                            if (!num.isEmpty()) {
                                colOffset = Integer.parseInt(num);
                            }
                        }
                        if (line.matches("^\\s*@VisualRowOffset\\([-+]?\\d+\\).*")) {
                            String num = line.replaceAll("[^0-9-]", "");
                            if (!num.isEmpty()) {
                                rowOffset = Integer.parseInt(num);
                            }
                        }
                    }
                } catch (Exception ignore) {}
            } else {
                final VisualOffset vo = sc.getClass().getAnnotation(VisualOffset.class);
                if (vo != null) colOffset = vo.value();
                final VisualRowOffset vro = sc.getClass().getAnnotation(VisualRowOffset.class);
                if (vro != null) rowOffset = vro.value();
            }

            initialColumnOffsets.put(entry.getKey(), colOffset);
            initialRowOffsets.put(entry.getKey(), rowOffset);
            tableModel.addRow(new Object[]{sc.getClass().getName(), sc.getSimpleName(), colOffset, rowOffset});
        }
    }

    private void addScenarioRecursively(_Scenario scenario) {
        if (scenario == null) return;
        final String className = scenario.getClass().getName();
        if (!scenarioMap.containsKey(className)) {
            scenarioMap.put(className, scenario);
            if (scenario instanceof works.lysenko.tree.base.Node node) {
                final java.util.Collection<works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction>> children = node.getPool().getPairList();
                if (children != null) {
                    for (works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction> pair : children) {
                        addScenarioRecursively(pair.k());
                    }
                }
            }
        }
    }

    private void preview() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        
        TreeLayoutComputer.offsetOverrides.clear();
        TreeLayoutComputer.rowOffsetOverrides.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String className = (String) tableModel.getValueAt(i, 0);
            final String simpleName = (String) tableModel.getValueAt(i, 1);
            final int colOffset = (Integer) tableModel.getValueAt(i, 2);
            final int rowOffset = (Integer) tableModel.getValueAt(i, 3);
            if (className != null) {
                TreeLayoutComputer.offsetOverrides.put(className, colOffset);
                TreeLayoutComputer.rowOffsetOverrides.put(className, rowOffset);
            }
            if (simpleName != null) {
                TreeLayoutComputer.offsetOverrides.put(simpleName, colOffset);
                TreeLayoutComputer.rowOffsetOverrides.put(simpleName, rowOffset);
            }
        }

        try {
            final String rootPackage = works.lysenko.util.prop.tree.Scenario.root;
            Tree tree = new Tree(rootPackage);

            final boolean originalHideOff = works.lysenko.util.prop.tree.HideOff.branches;
            works.lysenko.util.prop.tree.HideOff.branches = false;
            
            // Collect all known scenarios from the newly initialized tree
            final Set<_Scenario> allScenarios = new java.util.HashSet<>();
            java.util.function.Consumer<_Scenario> collector = new java.util.function.Consumer<>() {
                @Override
                public void accept(_Scenario s) {
                    if (s == null || !allScenarios.add(s)) return;
                    if (s instanceof works.lysenko.tree.base.Node node) {
                        for (works.lysenko.util.data.records.KeyValue<_Scenario, org.apache.commons.math3.fraction.Fraction> child : node.getPool().getPairList()) {
                            accept(child.k());
                        }
                    }
                }
            };
            for (_Scenario b : tree.getBranches()) collector.accept(b);
            
            TreeHtml.TreeLayout layout;
            try {
                TreeLayoutComputer.previewAccessibleScenarios = allScenarios;
                final TreeMap<String, Result> previewResults = new TreeMap<>();
                for (_Scenario s : allScenarios) {
                    final String tagKey = works.lysenko.util.data.strs.Bind.b(s.getShortName(), s.type().tag());
                    previewResults.put(tagKey, new Result(s, s.type(), s.weightConfigured(), s.weightUpstream(), s.weightDownstream(), java.util.Collections.emptyList(), 1, 0));
                }
                layout = TreeHtml.computeLayout(tree.getBranches(), previewResults);
            } finally {
                works.lysenko.util.prop.tree.HideOff.branches = originalHideOff;
                TreeLayoutComputer.previewAccessibleScenarios = null;
            }

            String html = TreeHtml.renderHtml(layout.nodes(), layout.edges());
            
            File tempHtml = File.createTempFile("tree-preview-", ".html");
            works.lysenko.util.func.type.Files.writeToFile(html, tempHtml.getAbsolutePath());
            
            Desktop.getDesktop().browse(tempHtml.toURI());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating preview: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void saveToSources() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int modifiedCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String className = (String) tableModel.getValueAt(i, 0);
            final int currentColOffset = (Integer) tableModel.getValueAt(i, 2);
            final int currentRowOffset = (Integer) tableModel.getValueAt(i, 3);
            final int initialColOffset = initialColumnOffsets.getOrDefault(className, 0);
            final int initialRowOffset = initialRowOffsets.getOrDefault(className, 0);

            if (currentColOffset != initialColOffset || currentRowOffset != initialRowOffset) {
                try {
                    updateSourceFile(className, currentColOffset, currentRowOffset);
                    initialColumnOffsets.put(className, currentColOffset);
                    initialRowOffsets.put(className, currentRowOffset);
                    modifiedCount++;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Failed to update " + className + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (modifiedCount > 0) {
            JOptionPane.showMessageDialog(this, "Successfully updated " + modifiedCount + " source files.\nPlease recompile the test project for annotations to take effect.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No changes detected.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateSourceFile(String className, int newColOffset, int newRowOffset) throws IOException {
        final String userDir = System.getProperty("user.dir");
        final Path sourcePath = Paths.get(userDir, "src", "main", "java", className.replace('.', '/') + ".java");
        final File sourceFile = sourcePath.toFile();
        
        if (!sourceFile.exists()) {
            throw new java.io.FileNotFoundException("Could not find source file at " + sourceFile.getAbsolutePath() + ". If you are running bot-framework directly, it cannot edit files in the downstream project unless launched from there.");
        }

        final List<String> lines = java.nio.file.Files.readAllLines(sourcePath);
        final java.util.List<String> newLines = new java.util.ArrayList<>();
        
        boolean hasColImport = false;
        boolean hasRowImport = false;
        int classDeclarationLineIndex = -1;
        
        // Pass 1: detect existing elements
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.contains("import works.lysenko.util.prop.tree.VisualOffset;")) {
                hasColImport = true;
            }
            if (line.contains("import works.lysenko.util.prop.tree.VisualRowOffset;")) {
                hasRowImport = true;
            }
            if (line.matches("^\\s*public\\s+(final\\s+)?(abstract\\s+)?class\\s+.*")) {
                classDeclarationLineIndex = i;
                break;
            }
        }
        
        if (classDeclarationLineIndex == -1) {
            throw new IOException("Could not find public class declaration in " + sourcePath);
        }

        boolean colImportAdded = hasColImport;
        boolean rowImportAdded = hasRowImport;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Add imports after package declaration if missing and needed
            if (line.startsWith("package ")) {
                newLines.add(line);
                if (!colImportAdded && newColOffset != 0) {
                    newLines.add("import works.lysenko.util.prop.tree.VisualOffset;");
                    colImportAdded = true;
                }
                if (!rowImportAdded && newRowOffset != 0) {
                    newLines.add("import works.lysenko.util.prop.tree.VisualRowOffset;");
                    rowImportAdded = true;
                }
                continue;
            }
            
            // Remove existing annotations
            if (line.matches("^\\s*@VisualOffset.*") || line.matches("^\\s*@VisualRowOffset.*")) {
                continue;
            }
            
            // Add annotations right before class declaration
            if (i == classDeclarationLineIndex) {
                if (newColOffset != 0) {
                    newLines.add("@VisualOffset(" + newColOffset + ")");
                }
                if (newRowOffset != 0) {
                    newLines.add("@VisualRowOffset(" + newRowOffset + ")");
                }
            }
            
            newLines.add(line);
        }
        
        java.nio.file.Files.write(sourcePath, newLines);
    }
}