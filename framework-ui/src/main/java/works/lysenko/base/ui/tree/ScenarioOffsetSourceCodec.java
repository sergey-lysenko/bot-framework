package works.lysenko.base.ui.tree;

import works.lysenko.util.prop.tree.VisualOffset;
import works.lysenko.util.prop.tree.VisualRowOffset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.chrs.__.CH;
import static works.lysenko.util.chrs.__.CT;
import static works.lysenko.util.chrs.__.IM;
import static works.lysenko.util.chrs.__.LA;
import static works.lysenko.util.chrs.__.LY;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.__.UN;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.___.SRC;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.chrs.____.DIRE;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.FIND;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.MAIN;
import static works.lysenko.util.chrs.____.ROOT;
import static works.lysenko.util.chrs.____.SAVE;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.C.COULD;
import static works.lysenko.util.lang.word.E.EDITS;
import static works.lysenko.util.lang.word.J.JAVA;
import static works.lysenko.util.lang.word.P.PACKAGE;
import static works.lysenko.util.lang.word.P.PORT;
import static works.lysenko.util.lang.word.P.PROJECT;
import static works.lysenko.util.lang.word.S.SOURCE;
import static works.lysenko.util.lang.word.S.SOURCES;
import static works.lysenko.util.spec.Symbols.CLS_BRK;
import static works.lysenko.util.spec.Symbols.OPN_BRK;
import static works.lysenko.util.spec.Symbols.SEM_CLN;
import static works.lysenko.util.spec.Symbols._ASTRS_;
import static works.lysenko.util.spec.Symbols._AT_SGN;
import static works.lysenko.util.spec.Symbols._DOLLAR;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._LFD_;
import static works.lysenko.util.spec.Symbols._SLASH_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Reads and updates {@code @VisualOffset} and {@code @VisualRowOffset} annotations
 * in scenario Java source files cleanly, managing imports and preserving formatting.
 */
public enum ScenarioOffsetSourceCodec {
    ;

    private static final String VISUAL_OFFSET_IMPORT = VisualOffset.class.getName();
    private static final String VISUAL_ROW_OFFSET_IMPORT = VisualRowOffset.class.getName();
    private static final String VISUAL_OFFSET_SIMPLE = VisualOffset.class.getSimpleName();
    private static final String VISUAL_ROW_OFFSET_SIMPLE = VisualRowOffset.class.getSimpleName();
    private static final String WILDCARD_IMPORT = s(VisualOffset.class.getPackageName(), _DOT_, _ASTRS_);
    private static final String PACKAGE_PREFIX = s(PACKAGE, _SPACE_);
    private static final String IMPORT_PREFIX = s(IM, PORT, _SPACE_);
    private static final String JAVA_EXTENSION = s(_DOT_, JAVA);
    private static final char CARRIAGE_RETURN = 13;
    private static final String CRLF = s(CARRIAGE_RETURN, _LFD_);
    private static final String LF = s(_LFD_);
    private static final int NOT_FOUND = -1;
    private static final int EXTRA_LINE_CAPACITY = 4;
    private static final int MAX_OFFSET_ANNOTATIONS = 2;

    private static final Pattern VISUAL_OFFSET_LINE = Pattern.compile(
            "^\\s*@VisualOffset\\s*\\(\\s*(?:value\\s*=\\s*)?([-+]?\\d+)\\s*\\).*");
    private static final Pattern VISUAL_ROW_OFFSET_LINE = Pattern.compile(
            "^\\s*@VisualRowOffset\\s*\\(\\s*(?:value\\s*=\\s*)?([-+]?\\d+)\\s*\\).*");
    private static final Pattern ANY_VISUAL_OFFSET_LINE = Pattern.compile("^\\s*@VisualOffset\\b.*");
    private static final Pattern ANY_VISUAL_ROW_OFFSET_LINE = Pattern.compile("^\\s*@VisualRowOffset\\b.*");
    private static final Pattern TYPE_DECL_LINE = Pattern.compile(
            "^(\\s*)(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed)\\s+)*(?:class|record)\\s+\\w+.*");
    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+([^;\\s]+)\\s*;\\s*$");

    /**
     * Immutable pair of column and row visual offsets.
     *
     * @param colOffset column offset applied to the scenario node
     * @param rowOffset row offset applied to the scenario node
     */
    public record Offsets(int colOffset, int rowOffset) {

        /**
         * Constant representing zero column and row visual offsets.
         */
        public static final Offsets ZERO = new Offsets(0, 0);

        /**
         * Returns {@code true} when both column and row offsets are zero.
         *
         * @return {@code true} if both offsets are zero
         */
        public boolean isZero() {
            return 0 == colOffset && 0 == rowOffset;
        }
    }

    private record StrippedSource(List<String> lines, int typeDeclIndex) {
    }

    private record ImportScan(
            List<String> lines,
            Collection<String> missingImports,
            int packageLineIdx,
            int lastImportIdx,
            int adjustedTypeDeclIdx) {
    }

    /**
     * Resolves the conventional Maven source path for a fully-qualified class name under {@code baseDir}.
     *
     * @param baseDir   project base directory
     * @param className fully-qualified class name
     * @return resolved Java source path
     */
    public static Path resolveSourcePath(final Path baseDir, final String className) {
        final String topLevelClass = stripInnerClass(className);
        return baseDir.resolve(SRC).resolve(MAIN).resolve(JAVA)
                .resolve(s(topLevelClass.replace(_DOT_, _SLASH_), JAVA_EXTENSION));
    }

    /**
     * Reads {@code @VisualOffset} and {@code @VisualRowOffset} from the given Java source content.
     *
     * @param lines Java source lines to inspect
     * @return parsed offsets, defaulting to zero for absent annotations
     */
    public static Offsets parseOffsets(final Iterable<String> lines) {
        int col = 0;
        int row = 0;
        final Matcher colMatcher = VISUAL_OFFSET_LINE.matcher(EMPTY);
        final Matcher rowMatcher = VISUAL_ROW_OFFSET_LINE.matcher(EMPTY);
        final Matcher typeDeclMatcher = TYPE_DECL_LINE.matcher(EMPTY);
        final Iterator<String> iterator = lines.iterator();
        boolean beforeTypeDecl = true;
        while (beforeTypeDecl && iterator.hasNext()) {
            final String line = iterator.next();
            if (colMatcher.reset(line).matches()) {
                col = Integer.parseInt(colMatcher.group(1));
            } else if (rowMatcher.reset(line).matches()) {
                row = Integer.parseInt(rowMatcher.group(1));
            } else if (typeDeclMatcher.reset(line).matches()) {
                beforeTypeDecl = false;
            }
        }
        return new Offsets(col, row);
    }

    /**
     * Reads offsets from {@code sourcePath}, returning {@code fallback} when the file does not exist or cannot be read.
     *
     * @param sourcePath path to the scenario Java source file
     * @param fallback   fallback offsets returned when reading fails
     * @return resolved offsets from source or fallback
     */
    public static Offsets readOffsets(final Path sourcePath, final Offsets fallback) {
        final Offsets defaultValue = (null == fallback) ? Offsets.ZERO : fallback;
        Offsets result;
        if (null != sourcePath && Files.isRegularFile(sourcePath)) {
            try {
                result = parseOffsets(Files.readAllLines(sourcePath, StandardCharsets.UTF_8));
            } catch (final IOException _) {
                result = defaultValue;
            }
        } else {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Transforms Java source lines so the top-level class carries exactly {@code offsets},
     * adding needed imports and removing unused {@code VisualOffset}/{@code VisualRowOffset} imports.
     *
     * @param originalLines original Java source lines
     * @param offsets       target visual offsets to apply
     * @return transformed Java source lines
     * @throws IOException if no top-level class or record declaration can be located
     */
    public static List<String> applyOffsets(final Collection<String> originalLines, final Offsets offsets)
            throws IOException {
        final int newCol = Math.max(0, offsets.colOffset());
        final int newRow = offsets.rowOffset();

        final StrippedSource stripped = stripVisualOffsetAnnotations(originalLines);
        if (NOT_FOUND == stripped.typeDeclIndex()) {
            throw new IOException("Could not find class declaration");
        }

        final boolean needColImport = 0 != newCol
                || containsToken(stripped.lines(), stripped.typeDeclIndex(), VISUAL_OFFSET_SIMPLE);
        final boolean needRowImport = 0 != newRow
                || containsToken(stripped.lines(), stripped.typeDeclIndex(), VISUAL_ROW_OFFSET_SIMPLE);

        final ImportScan scan = scanAndFilterImports(
                stripped.lines(), stripped.typeDeclIndex(), needColImport, needRowImport);
        final int finalTypeDeclIdx = insertMissingImports(scan);
        insertAnnotations(scan.lines(), finalTypeDeclIdx, newCol, newRow);

        return scan.lines();
    }

    /**
     * Reads, updates, and writes {@code sourcePath} in UTF-8 preserving its original line separator.
     *
     * @param sourcePath path to the scenario Java source file
     * @param offsets    target visual offsets to persist
     * @throws IOException if the source file does not exist or cannot be read or written
     */
    public static void writeOffsets(final Path sourcePath, final Offsets offsets) throws IOException {
        if (!Files.isRegularFile(sourcePath)) {
            throw new IOException(b(c(COULD), NOT, FIND, SOURCE, FILE, AT,
                    s(sourcePath.toAbsolutePath(), _DOT_),
                    c(s(LA, UN, CH)), FROM, THE, TEST, PROJECT, ROOT, TO, SAVE, EDITS,
                    s(DIRE, CT, LY), TO, s(SOURCES, _DOT_)));
        }
        final String raw = Files.readString(sourcePath, StandardCharsets.UTF_8);
        final String lineSep = raw.contains(CRLF) ? CRLF : LF;
        final boolean trailingNewline = raw.endsWith(LF);
        final List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
        final List<String> updated = applyOffsets(lines, offsets);
        final String joined = s(String.join(lineSep, updated), trailingNewline ? lineSep : EMPTY);
        Files.writeString(sourcePath, joined, StandardCharsets.UTF_8);
    }

    private static StrippedSource stripVisualOffsetAnnotations(final Collection<String> originalLines) {
        final List<String> stripped = new ArrayList<>(originalLines.size() + EXTRA_LINE_CAPACITY);
        final Matcher anyColMatcher = ANY_VISUAL_OFFSET_LINE.matcher(EMPTY);
        final Matcher anyRowMatcher = ANY_VISUAL_ROW_OFFSET_LINE.matcher(EMPTY);
        final Matcher typeDeclMatcher = TYPE_DECL_LINE.matcher(EMPTY);
        int typeDeclIndex = NOT_FOUND;
        for (final String line : originalLines) {
            final boolean beforeDecl = NOT_FOUND == typeDeclIndex;
            final boolean keepLine = !beforeDecl
                    || (!anyColMatcher.reset(line).matches() && !anyRowMatcher.reset(line).matches());
            if (keepLine) {
                if (beforeDecl && typeDeclMatcher.reset(line).matches()) {
                    typeDeclIndex = stripped.size();
                }
                stripped.add(line);
            }
        }
        return new StrippedSource(stripped, typeDeclIndex);
    }

    private static boolean containsToken(final List<String> lines, final int startIndex, final CharSequence token) {
        return lines.subList(startIndex, lines.size()).stream().anyMatch(line -> line.contains(token));
    }

    private static ImportScan scanAndFilterImports(
            final List<String> stripped,
            final int typeDeclIndex,
            final boolean needColImport,
            final boolean needRowImport) {
        final List<String> withImports = new ArrayList<>(stripped.size() + EXTRA_LINE_CAPACITY);
        final Matcher importMatcher = IMPORT_LINE.matcher(EMPTY);
        boolean colImportMissing = true;
        boolean rowImportMissing = true;
        boolean wildcardAbsent = true;
        int packageLineIdx = NOT_FOUND;
        int lastImportIdx = NOT_FOUND;
        int adjustedTypeDeclIdx = NOT_FOUND;

        for (int i = 0; i < stripped.size(); i++) {
            final String line = stripped.get(i);
            boolean keepLine = true;
            if (i < typeDeclIndex) {
                if (importMatcher.reset(line).matches()) {
                    final String imported = importMatcher.group(1);
                    if (Objects.equals(VISUAL_OFFSET_IMPORT, imported)) {
                        keepLine = needColImport;
                        colImportMissing = false;
                    } else if (Objects.equals(VISUAL_ROW_OFFSET_IMPORT, imported)) {
                        keepLine = needRowImport;
                        rowImportMissing = false;
                    } else if (Objects.equals(WILDCARD_IMPORT, imported)) {
                        wildcardAbsent = false;
                    }
                    if (keepLine) {
                        lastImportIdx = withImports.size();
                    }
                } else if (line.strip().startsWith(PACKAGE_PREFIX)) {
                    packageLineIdx = withImports.size();
                }
            }
            if (i == typeDeclIndex) {
                adjustedTypeDeclIdx = withImports.size();
            }
            if (keepLine) {
                withImports.add(line);
            }
        }

        final Collection<String> missingImports = new ArrayList<>(MAX_OFFSET_ANNOTATIONS);
        if (needColImport && colImportMissing && wildcardAbsent) {
            missingImports.add(s(IMPORT_PREFIX, VISUAL_OFFSET_IMPORT, SEM_CLN));
        }
        if (needRowImport && rowImportMissing && wildcardAbsent) {
            missingImports.add(s(IMPORT_PREFIX, VISUAL_ROW_OFFSET_IMPORT, SEM_CLN));
        }
        return new ImportScan(withImports, missingImports, packageLineIdx, lastImportIdx, adjustedTypeDeclIdx);
    }

    private static int insertMissingImports(final ImportScan scan) {
        final List<String> lines = scan.lines();
        final Collection<String> missingImports = scan.missingImports();
        int adjustedTypeDeclIdx = scan.adjustedTypeDeclIdx();
        if (!missingImports.isEmpty()) {
            if (0 <= scan.lastImportIdx()) {
                lines.addAll(scan.lastImportIdx() + 1, missingImports);
                adjustedTypeDeclIdx += missingImports.size();
            } else if (0 <= scan.packageLineIdx()) {
                int insertAt = scan.packageLineIdx() + 1;
                final Collection<String> block = new ArrayList<>(missingImports.size() + MAX_OFFSET_ANNOTATIONS);
                if (insertAt < lines.size() && lines.get(insertAt).isBlank()) {
                    insertAt++;
                } else {
                    block.add(EMPTY);
                }
                block.addAll(missingImports);
                if (insertAt >= lines.size() || !lines.get(insertAt).isBlank()) {
                    block.add(EMPTY);
                }
                lines.addAll(insertAt, block);
                adjustedTypeDeclIdx += block.size();
            } else {
                lines.addAll(0, missingImports);
                adjustedTypeDeclIdx += missingImports.size();
            }
        }
        return adjustedTypeDeclIdx;
    }

    private static void insertAnnotations(
            final List<String> lines,
            final int typeDeclIdx,
            final int newCol,
            final int newRow) {
        final Matcher declMatcher = TYPE_DECL_LINE.matcher(lines.get(typeDeclIdx));
        final String indent = declMatcher.matches() ? declMatcher.group(1) : EMPTY;
        final Collection<String> annotations = new ArrayList<>(MAX_OFFSET_ANNOTATIONS);
        if (0 != newCol) {
            annotations.add(s(indent, _AT_SGN, VISUAL_OFFSET_SIMPLE, OPN_BRK, newCol, CLS_BRK));
        }
        if (0 != newRow) {
            annotations.add(s(indent, _AT_SGN, VISUAL_ROW_OFFSET_SIMPLE, OPN_BRK, newRow, CLS_BRK));
        }
        if (!annotations.isEmpty()) {
            lines.addAll(typeDeclIdx, annotations);
        }
    }

    private static String stripInnerClass(final String className) {
        final int dollar = className.indexOf(_DOLLAR);
        return (0 <= dollar) ? className.substring(0, dollar) : className;
    }
}
