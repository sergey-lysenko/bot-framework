package works.lysenko.base.ui;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.tree.TreeLayoutComputer;
import works.lysenko.tree.Tree;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.func.core.ClassLoader;
import works.lysenko.util.func.type.Files;
import works.lysenko.util.prop.tree.VisualOffset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
    private final Map<String, _Scenario> scenarioMap = new TreeMap<>();
    private final Map<String, Integer> initialOffsets = new TreeMap<>();

    public TreeLayoutEditorPanel() {
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Scenario Class", "Scenario Name", "Visual Offset"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
        initialOffsets.clear();

        final String rootPackage = works.lysenko.util.prop.tree.Scenario.root;
        if (rootPackage == null || rootPackage.isBlank()) return;

        final Set<_Scenario> scenarios = ClassLoader.readFrom(rootPackage, false);
        for (_Scenario s : scenarios) {
            addScenarioRecursively(s);
        }

        for (Map.Entry<String, _Scenario> entry : scenarioMap.entrySet()) {
            final _Scenario sc = entry.getValue();
            int offset = 0;

            final String userDir = System.getProperty("user.dir");
            final Path sourcePath = Paths.get(userDir, "src", "main", "java", sc.getClass().getName().replace('.', '/') + ".java");
            final File sourceFile = sourcePath.toFile();
            if (sourceFile.exists()) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(sourcePath)) {
                        if (line.matches("^\\s*@VisualOffset\\(\\d+\\).*")) {
                            String num = line.replaceAll("[^0-9]", "");
                            if (!num.isEmpty()) {
                                offset = Integer.parseInt(num);
                            }
                            break;
                        }
                    }
                } catch (Exception ignore) {}
            } else {
                final VisualOffset vo = sc.getClass().getAnnotation(VisualOffset.class);
                if (vo != null) offset = vo.value();
            }

            initialOffsets.put(entry.getKey(), offset);
            tableModel.addRow(new Object[]{sc.getClass().getName(), sc.getSimpleName(), offset});
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
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final String simpleName = (String) tableModel.getValueAt(i, 1);
            final int offset = (Integer) tableModel.getValueAt(i, 2);
            TreeLayoutComputer.offsetOverrides.put(simpleName, offset);
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
                layout = TreeHtml.computeLayout(tree.getBranches(), new TreeMap<>());
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
            final int currentOffset = (Integer) tableModel.getValueAt(i, 2);
            final int initialOffset = initialOffsets.getOrDefault(className, 0);

            if (currentOffset != initialOffset) {
                try {
                    updateSourceFile(className, currentOffset);
                    initialOffsets.put(className, currentOffset);
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

    private void updateSourceFile(String className, int newOffset) throws IOException {
        final String userDir = System.getProperty("user.dir");
        final Path sourcePath = Paths.get(userDir, "src", "main", "java", className.replace('.', '/') + ".java");
        final File sourceFile = sourcePath.toFile();
        
        if (!sourceFile.exists()) {
            throw new java.io.FileNotFoundException("Could not find source file at " + sourceFile.getAbsolutePath() + ". If you are running bot-framework directly, it cannot edit files in the downstream project unless launched from there.");
        }

        final List<String> lines = java.nio.file.Files.readAllLines(sourcePath);
        final java.util.List<String> newLines = new java.util.ArrayList<>();
        
        boolean hasImport = false;
        boolean hasAnnotation = false;
        int classDeclarationLineIndex = -1;
        
        // Pass 1: detect existing elements
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.contains("import works.lysenko.util.prop.tree.VisualOffset;")) {
                hasImport = true;
            }
            if (line.matches("^\\s*@VisualOffset.*")) {
                hasAnnotation = true;
            }
            if (line.matches("^\\s*public\\s+(final\\s+)?(abstract\\s+)?class\\s+.*")) {
                classDeclarationLineIndex = i;
                break;
            }
        }
        
        if (classDeclarationLineIndex == -1) {
            throw new IOException("Could not find public class declaration in " + sourcePath);
        }

        boolean importAdded = hasImport;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Add import after package declaration if missing and needed
            if (!importAdded && newOffset != 0 && line.startsWith("package ")) {
                newLines.add(line);
                newLines.add("");
                newLines.add("import works.lysenko.util.prop.tree.VisualOffset;");
                importAdded = true;
                continue;
            }
            
            // Remove existing annotation
            if (line.matches("^\\s*@VisualOffset.*")) {
                continue;
            }
            
            // Add annotation right before class declaration
            if (i == classDeclarationLineIndex && newOffset != 0) {
                newLines.add("@VisualOffset(" + newOffset + ")");
            }
            
            newLines.add(line);
        }
        
        java.nio.file.Files.write(sourcePath, newLines);
    }
}