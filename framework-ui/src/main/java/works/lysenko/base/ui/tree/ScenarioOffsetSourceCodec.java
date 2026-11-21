package works.lysenko.base.ui.tree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and updates {@code @VisualOffset} and {@code @VisualRowOffset} annotations
 * in scenario Java source files cleanly, managing imports and preserving formatting.
 */
public final class ScenarioOffsetSourceCodec {

    public static final String VISUAL_OFFSET_IMPORT = "works.lysenko.util.prop.tree.VisualOffset";
    public static final String VISUAL_ROW_OFFSET_IMPORT = "works.lysenko.util.prop.tree.VisualRowOffset";

    private static final Pattern VISUAL_OFFSET_LINE = Pattern.compile(
            "^\\s*@VisualOffset\\s*\\(\\s*(?:value\\s*=\\s*)?([-+]?\\d+)\\s*\\).*");
    private static final Pattern VISUAL_ROW_OFFSET_LINE = Pattern.compile(
            "^\\s*@VisualRowOffset\\s*\\(\\s*(?:value\\s*=\\s*)?([-+]?\\d+)\\s*\\).*");
    private static final Pattern ANY_VISUAL_OFFSET_LINE = Pattern.compile("^\\s*@VisualOffset\\b.*");
    private static final Pattern ANY_VISUAL_ROW_OFFSET_LINE = Pattern.compile("^\\s*@VisualRowOffset\\b.*");
    private static final Pattern TYPE_DECL_LINE = Pattern.compile(
            "^(\\s*)(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed)\\s+)*(?:class|record)\\s+\\w+.*");
    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+([^;\\s]+)\\s*;\\s*$");

    private ScenarioOffsetSourceCodec() {
    }

    /**
     * Immutable pair of column and row visual offsets.
     */
    public record Offsets(int colOffset, int rowOffset) {
        public static final Offsets ZERO = new Offsets(0, 0);

        public boolean isZero() {
            return colOffset == 0 && rowOffset == 0;
        }
    }

    /**
     * Resolves the conventional Maven source path for a fully-qualified class name under {@code baseDir}.
     */
    public static Path resolveSourcePath(final Path baseDir, final String className) {
        final String topLevelClass = stripInnerClass(className);
        return baseDir.resolve("src").resolve("main").resolve("java")
                .resolve(topLevelClass.replace('.', '/') + ".java");
    }

    /**
     * Reads {@code @VisualOffset} and {@code @VisualRowOffset} from the given Java source content.
     */
    public static Offsets parseOffsets(final List<String> lines) {
        int col = 0;
        int row = 0;
        for (final String line : lines) {
            final Matcher colMatcher = VISUAL_OFFSET_LINE.matcher(line);
            if (colMatcher.matches()) {
                col = Integer.parseInt(colMatcher.group(1));
                continue;
            }
            final Matcher rowMatcher = VISUAL_ROW_OFFSET_LINE.matcher(line);
            if (rowMatcher.matches()) {
                row = Integer.parseInt(rowMatcher.group(1));
                continue;
            }
            if (TYPE_DECL_LINE.matcher(line).matches()) {
                break;
            }
        }
        return new Offsets(col, row);
    }

    /**
     * Reads offsets from {@code sourcePath}, returning {@code fallback} when the file does not exist or cannot be read.
     */
    public static Offsets readOffsets(final Path sourcePath, final Offsets fallback) {
        if (null == sourcePath || !Files.isRegularFile(sourcePath)) {
            return (null != fallback) ? fallback : Offsets.ZERO;
        }
        try {
            return parseOffsets(Files.readAllLines(sourcePath, StandardCharsets.UTF_8));
        } catch (final IOException ignored) {
            return (null != fallback) ? fallback : Offsets.ZERO;
        }
    }

    /**
     * Transforms Java source lines so the top-level class carries exactly {@code offsets},
     * adding needed imports and removing unused {@code VisualOffset}/{@code VisualRowOffset} imports.
     */
    public static List<String> applyOffsets(final List<String> originalLines, final Offsets offsets) throws IOException {
        final int newCol = Math.max(0, offsets.colOffset());
        final int newRow = offsets.rowOffset();

        // Pass 1: strip existing @VisualOffset / @VisualRowOffset annotation lines and detect type declaration
        final List<String> stripped = new ArrayList<>(originalLines.size() + 4);
        int typeDeclIndex = -1;
        for (final String line : originalLines) {
            if (typeDeclIndex == -1
                    && (ANY_VISUAL_OFFSET_LINE.matcher(line).matches()
                    || ANY_VISUAL_ROW_OFFSET_LINE.matcher(line).matches())) {
                continue;
            }
            if (typeDeclIndex == -1 && TYPE_DECL_LINE.matcher(line).matches()) {
                typeDeclIndex = stripped.size();
            }
            stripped.add(line);
        }

        if (typeDeclIndex == -1) {
            throw new IOException("Could not find class declaration");
        }

        // Check whether VisualOffset / VisualRowOffset are referenced anywhere in the remaining body
        boolean colReferencedInBody = false;
        boolean rowReferencedInBody = false;
        for (int i = typeDeclIndex; i < stripped.size(); i++) {
            final String line = stripped.get(i);
            if (line.contains("VisualOffset")) colReferencedInBody = true;
            if (line.contains("VisualRowOffset")) rowReferencedInBody = true;
        }
        final boolean needColImport = newCol != 0 || colReferencedInBody;
        final boolean needRowImport = newRow != 0 || rowReferencedInBody;

        // Pass 2: manage imports cleanly (remove unneeded ones, detect existing ones & insertion point)
        final List<String> withImports = new ArrayList<>(stripped.size() + 4);
        boolean hasColImport = false;
        boolean hasRowImport = false;
        boolean hasWildcardImport = false;
        int packageLineIdx = -1;
        int lastImportIdx = -1;
        int adjustedTypeDeclIdx = -1;

        for (int i = 0; i < stripped.size(); i++) {
            final String line = stripped.get(i);
            if (i < typeDeclIndex) {
                final Matcher imp = IMPORT_LINE.matcher(line);
                if (imp.matches()) {
                    final String imported = imp.group(1);
                    if (VISUAL_OFFSET_IMPORT.equals(imported)) {
                        if (!needColImport) continue;
                        hasColImport = true;
                    } else if (VISUAL_ROW_OFFSET_IMPORT.equals(imported)) {
                        if (!needRowImport) continue;
                        hasRowImport = true;
                    } else if ("works.lysenko.util.prop.tree.*".equals(imported)) {
                        hasWildcardImport = true;
                    }
                    lastImportIdx = withImports.size();
                } else if (line.trim().startsWith("package ")) {
                    packageLineIdx = withImports.size();
                }
            }
            if (i == typeDeclIndex) {
                adjustedTypeDeclIdx = withImports.size();
            }
            withImports.add(line);
        }

        final List<String> missingImports = new ArrayList<>(2);
        if (needColImport && !hasColImport && !hasWildcardImport) {
            missingImports.add("import " + VISUAL_OFFSET_IMPORT + ";");
        }
        if (needRowImport && !hasRowImport && !hasWildcardImport) {
            missingImports.add("import " + VISUAL_ROW_OFFSET_IMPORT + ";");
        }

        if (!missingImports.isEmpty()) {
            if (lastImportIdx >= 0) {
                final int insertAt = lastImportIdx + 1;
                withImports.addAll(insertAt, missingImports);
                adjustedTypeDeclIdx += missingImports.size();
            } else if (packageLineIdx >= 0) {
                int insertAt = packageLineIdx + 1;
                final List<String> block = new ArrayList<>();
                if (insertAt >= withImports.size() || !withImports.get(insertAt).isBlank()) {
                    block.add("");
                } else {
                    insertAt++;
                }
                block.addAll(missingImports);
                if (insertAt >= withImports.size() || !withImports.get(insertAt).isBlank()) {
                    block.add("");
                }
                withImports.addAll(insertAt, block);
                adjustedTypeDeclIdx += block.size();
            } else {
                withImports.addAll(0, missingImports);
                adjustedTypeDeclIdx += missingImports.size();
            }
        }

        // Pass 3: insert annotations immediately before the class declaration preserving indentation
        final Matcher declMatcher = TYPE_DECL_LINE.matcher(withImports.get(adjustedTypeDeclIdx));
        final String indent = declMatcher.matches() ? declMatcher.group(1) : "";
        final List<String> annotations = new ArrayList<>(2);
        if (newCol != 0) {
            annotations.add(indent + "@VisualOffset(" + newCol + ")");
        }
        if (newRow != 0) {
            annotations.add(indent + "@VisualRowOffset(" + newRow + ")");
        }
        if (!annotations.isEmpty()) {
            withImports.addAll(adjustedTypeDeclIdx, annotations);
        }

        return withImports;
    }

    /**
     * Reads, updates, and writes {@code sourcePath} in UTF-8 preserving its original line separator.
     */
    public static void writeOffsets(final Path sourcePath, final Offsets offsets) throws IOException {
        if (!Files.isRegularFile(sourcePath)) {
            throw new IOException("Could not find source file at " + sourcePath.toAbsolutePath()
                    + ". Launch from the test project root to save edits directly to sources.");
        }
        final String raw = Files.readString(sourcePath, StandardCharsets.UTF_8);
        final String lineSep = raw.contains("\r\n") ? "\r\n" : "\n";
        final boolean trailingNewline = raw.endsWith("\n");
        final List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
        final List<String> updated = applyOffsets(lines, offsets);
        final String joined = String.join(lineSep, updated) + (trailingNewline ? lineSep : "");
        Files.writeString(sourcePath, joined, StandardCharsets.UTF_8);
    }

    private static String stripInnerClass(final String className) {
        final int dollar = className.indexOf('$');
        return (dollar >= 0) ? className.substring(0, dollar) : className;
    }
}
