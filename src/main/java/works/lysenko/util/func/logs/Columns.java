package works.lysenko.util.func.logs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The Something class represents a utility for formatting strings into columns.
 */
@SuppressWarnings("TypeMayBeWeakened")
public final class Columns {

    private static final Pattern PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private final List<String> rows = new ArrayList<>(0);
    private final int columns;
    private final int totalWidth;
    private String buffer = EMPTY;
    private int current = 0;
    private int padding = 0;

    private Columns(final int columns, final int totalWidth) {

        this.columns = columns;
        this.totalWidth = totalWidth;
    }

    /**
     * Returns the minimal number of rows required to display a list of strings in a specified format.
     *
     * @param list    the list of strings to be displayed
     * @param length  the desired length of each row
     * @param maxCols the maximum number of columns allowed
     * @return the list of strings arranged in the format requiring the minimal number of rows
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "AccessingNonPublicFieldOfAnotherObject", "TypeMayBeWeakened"})
    public static List<String> minimalRows(final List<String> list, final int length, final int maxCols) {

        int minimalRows = Integer.MAX_VALUE;
        int cols = 1;
        List<String> result = List.of();
        Columns columns;

        do {
            columns = new Columns(cols, length);
            columns.addAll(list);
            if (columns.rows.size() < minimalRows) {
                minimalRows = columns.rows.size();
                result = columns.rows;
            }
            cols++;
        } while (cols < maxCols);
        return result;

    }

    /**
     * This method calculates the real length of a string by removing any ANSI escape codes present in the string.
     *
     * @param input the input string
     * @return the real length of the string after removing ANSI escape codes
     */
    private static int realLength(final CharSequence input) {

        return PATTERN.matcher(input).replaceAll(EMPTY).length();
    }

    @SuppressWarnings({"PublicMethodNotExposedInInterface", "MissingJavadoc", "AssignmentOrReturnOfFieldWithMutableType"})
    public List<String> getRows() {

        return rows;
    }

    /**
     * Adds a string to the buffer, considering column alignment and total width constraints.
     * If the buffer is not empty, the string is added based on the current column alignment. If the real length of the
     * buffer combined with the padding and the real length of the string exceeds the total width, the buffer is flushed.
     * The buffer is then updated by concatenating the existing buffer, padding spaces, and the new string. After each
     * string addition, the method checks if the current column index is at the last column or if the buffer size exceeds
     * the column width multiplied by (columns - 1). If either condition is true, the buffer is flushed.
     *
     * @param s the string to be added to the buffer
     */
    private void add(final String s) {

        final int column = (totalWidth / columns);
        if (buffer.isEmpty()) buffer = s;
        else {
            // column alignment
            padding = (column * current) - realLength(buffer);
            while (ZERO >= padding) {
                ++current;
                padding = column + padding;
            }
            if (realLength(buffer) + padding + realLength(s) > totalWidth) {
                flush();
            }
            buffer = s(buffer, s(_SPACE_).repeat(padding), s);
            // post-flushes
            if (current >= columns - 1) flush(); // regular
            if (realLength(buffer) > column * (columns - 1)) flush(); // overflow
        }
    }

    private void addAll(final Iterable<String> list) {

        for (final String s : list)
            add(s);
        flush();
    }

    /**
     * Flushes the buffer by adding its contents to the rows and resetting the buffer, current column index, and padding.
     * If the buffer is not empty, it is added to the rows list. After that, the buffer is cleared and the current column
     * index and padding are set to 0.
     */
    private void flush() {

        if (!buffer.isEmpty()) rows.add(buffer);
        buffer = EMPTY;
        current = 0;
        padding = 0;
    }
}
