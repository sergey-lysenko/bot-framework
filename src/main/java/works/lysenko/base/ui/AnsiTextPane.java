package works.lysenko.base.ui;

import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;

@SuppressWarnings({"ClassWithoutConstructor", "ClassWithoutLogger", "ClassUnconnectedToPackage", "ClassHasNoToStringMethod",
        "ChainedMethodCall", "NestedMethodCall", "ClassWithTooManyTransitiveDependencies",
        "ClassWithTooManyTransitiveDependents", "CyclicClassDependency", "FinalMethod"})
final class AnsiTextPane extends JTextPane {

    private static final Color[] COLORS = {
            new Color(0, 0, 0), new Color(205, 49, 49), new Color(13, 188, 121), new Color(229, 229, 16),
            new Color(36, 114, 200), new Color(188, 63, 188), new Color(17, 168, 205), new Color(229, 229, 229)
    };
    private static final Color[] BRIGHT_COLORS = {
            new Color(102, 102, 102), new Color(241, 76, 76), new Color(35, 209, 139), new Color(245, 245, 67),
            new Color(59, 142, 234), new Color(214, 112, 214), new Color(41, 184, 219), new Color(255, 255, 255)
    };
    private static final Color DEFAULT_FOREGROUND = new Color(229, 229, 229);
    private static final Color DEFAULT_BACKGROUND = new Color(30, 30, 30);
    private static final int TAB_SIZE = 8;
    private static final int MAX_SCROLLBACK_LINES = 5000;

    private final MutableAttributeSet attributes = new SimpleAttributeSet();
    private int cursor;
    private int savedCursor;
    private String pendingEscape = "";
    private boolean bold;

    AnsiTextPane() {
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        setForeground(DEFAULT_FOREGROUND);
        setBackground(DEFAULT_BACKGROUND);
        setEditable(false);
        setFocusable(false);
        setOpaque(true);
        StyleConstants.setForeground(attributes, DEFAULT_FOREGROUND);
        StyleConstants.setBackground(attributes, DEFAULT_BACKGROUND);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // getPreferredSize() would call back into this method, so ask the UI delegate directly
        return !(getParent() instanceof JViewport viewport) || getUI().getPreferredSize(this).width <= viewport.getWidth();
    }

    void appendOutput(final String text) {
        final String input = pendingEscape + text;
        pendingEscape = "";
        process(input);
        if (pendingEscape.isEmpty() && !text.endsWith("\n") && !text.endsWith("\r")) process("\n");
        trimScrollback();
    }

    private void process(final String input) {
        int index = 0;
        while (index < input.length()) {
            final char current = input.charAt(index);
            if ('\u001B' == current) {
                index = processEscape(input, index);
            } else if ('\r' == current) {
                cursor = lineStart(cursor);
                index++;
            } else if ('\n' == current) {
                cursor = lineEnd(cursor);
                if (cursor < getDocument().getLength()) cursor++;
                else insert("\n");
                index++;
            } else if ('\b' == current) {
                cursor = Math.max(lineStart(cursor), cursor - 1);
                index++;
            } else if ('\t' == current) {
                final int spaces = TAB_SIZE - (cursor - lineStart(cursor)) % TAB_SIZE;
                moveToColumn(column() + spaces);
                index++;
            } else if (Character.isISOControl(current)) {
                index++;
            } else {
                final int start = index++;
                while (index < input.length()) {
                    final char next = input.charAt(index);
                    if ('\u001B' == next || '\r' == next || '\n' == next || '\b' == next || '\t' == next
                            || Character.isISOControl(next)) break;
                    index++;
                }
                write(input.substring(start, index));
            }
        }
    }

    private int processEscape(final String input, final int start) {
        if (start + 1 >= input.length()) {
            pendingEscape = input.substring(start);
            return input.length();
        }
        if ('[' != input.charAt(start + 1)) {
            if ('7' == input.charAt(start + 1)) savedCursor = cursor;
            if ('8' == input.charAt(start + 1)) cursor = savedCursor;
            return start + 2;
        }
        int end = start + 2;
        while (end < input.length() && (input.charAt(end) < '@' || input.charAt(end) > '~')) end++;
        if (end == input.length()) {
            pendingEscape = input.substring(start);
            return input.length();
        }
        final char command = input.charAt(end);
        final String parameters = input.substring(start + 2, end);
        executeControl(parameters, command);
        return end + 1;
    }

    private void executeControl(final String parameters, final char command) {
        final int[] values = parseParameters(parameters);
        final int amount = parameter(values, 0, 1);
        switch (command) {
            case 'A' -> moveVertical(-amount);
            case 'B', 'e' -> moveVertical(amount);
            case 'C', 'a' -> moveHorizontal(amount);
            case 'D' -> moveHorizontal(-amount);
            case 'E' -> moveToRow(row() + amount, 0);
            case 'F' -> moveToRow(row() - amount, 0);
            case 'G', '`' -> moveToColumn(parameter(values, 0, 1) - 1);
            case 'H', 'f' -> moveToRow(parameter(values, 0, 1) - 1, parameter(values, 1, 1) - 1);
            case 'J' -> eraseScreen(parameter(values, 0, 0));
            case 'K' -> eraseLine(parameter(values, 0, 0));
            case 'P' -> deleteCharacters(amount);
            case 'X' -> eraseCharacters(amount);
            case 'd' -> moveToRow(parameter(values, 0, 1) - 1, column());
            case 'm' -> setGraphics(values);
            case 's' -> savedCursor = cursor;
            case 'u' -> cursor = Math.min(savedCursor, getDocument().getLength());
            case '@' -> insert(" ".repeat(Math.min(amount, 1000)));
            default -> {
                // Unsupported terminal controls are ignored.
            }
        }
    }

    private static int[] parseParameters(final String parameters) {
        final String normalized = parameters.isEmpty() || "?".equals(parameters)
                ? "0" : parameters.replace("?", "");
        final String[] parts = normalized.split(";", -1);
        final int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (final NumberFormatException ignored) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static int parameter(final int[] parameters, final int index, final int defaultValue) {
        return index >= parameters.length || parameters[index] == 0 ? defaultValue : parameters[index];
    }

    private void setGraphics(final int[] parameters) {
        for (int index = 0; index < parameters.length; index++) {
            final int parameter = parameters[index];
            if (0 == parameter) {
                bold = false;
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, false);
                StyleConstants.setUnderline(attributes, false);
                StyleConstants.setForeground(attributes, DEFAULT_FOREGROUND);
                StyleConstants.setBackground(attributes, DEFAULT_BACKGROUND);
            } else if (1 == parameter) {
                bold = true;
                StyleConstants.setBold(attributes, true);
            } else if (3 == parameter) {
                StyleConstants.setItalic(attributes, true);
            } else if (4 == parameter) {
                StyleConstants.setUnderline(attributes, true);
            } else if (22 == parameter) {
                bold = false;
                StyleConstants.setBold(attributes, false);
            } else if (23 == parameter) {
                StyleConstants.setItalic(attributes, false);
            } else if (24 == parameter) {
                StyleConstants.setUnderline(attributes, false);
            } else if (39 == parameter) {
                StyleConstants.setForeground(attributes, DEFAULT_FOREGROUND);
            } else if (49 == parameter) {
                StyleConstants.setBackground(attributes, DEFAULT_BACKGROUND);
            } else if ((38 == parameter || 48 == parameter) && index + 2 < parameters.length) {
                if (5 == parameters[index + 1]) {
                    setColor(38 == parameter, indexedColor(parameters[index + 2]));
                    index += 2;
                } else if (2 == parameters[index + 1] && index + 4 < parameters.length) {
                    setColor(38 == parameter, new Color(clampColor(parameters[index + 2]),
                            clampColor(parameters[index + 3]), clampColor(parameters[index + 4])));
                    index += 4;
                }
            } else if (parameter >= 30 && parameter <= 37) {
                StyleConstants.setForeground(attributes, color(parameter - 30));
            } else if (parameter >= 40 && parameter <= 47) {
                StyleConstants.setBackground(attributes, COLORS[parameter - 40]);
            } else if (parameter >= 90 && parameter <= 97) {
                StyleConstants.setForeground(attributes, BRIGHT_COLORS[parameter - 90]);
            } else if (parameter >= 100 && parameter <= 107) {
                StyleConstants.setBackground(attributes, BRIGHT_COLORS[parameter - 100]);
            }
        }
    }

    private static int clampColor(final int component) {
        return Math.max(0, Math.min(255, component));
    }

    private static Color indexedColor(final int index) {
        final int safeIndex = Math.max(0, Math.min(255, index));
        if (safeIndex < 8) return COLORS[safeIndex];
        if (safeIndex < 16) return BRIGHT_COLORS[safeIndex - 8];
        if (safeIndex < 232) {
            final int value = safeIndex - 16;
            return new Color(colorCube(value / 36), colorCube(value / 6 % 6), colorCube(value % 6));
        }
        final int gray = 8 + (safeIndex - 232) * 10;
        return new Color(gray, gray, gray);
    }

    private static int colorCube(final int component) {
        return 0 == component ? 0 : 55 + component * 40;
    }

    private void setColor(final boolean foreground, final Color color) {
        if (foreground) StyleConstants.setForeground(attributes, color);
        else StyleConstants.setBackground(attributes, color);
    }

    private Color color(final int index) {
        return bold ? BRIGHT_COLORS[index] : COLORS[index];
    }

    private void write(final String text) {
        final int end = lineEnd(cursor);
        final int overwrite = Math.min(text.length(), Math.max(0, end - cursor));
        try {
            final StyledDocument document = getStyledDocument();
            if (overwrite > 0) document.remove(cursor, overwrite);
            document.insertString(cursor, text, attributes);
            cursor += text.length();
        } catch (final BadLocationException e) {
            throw new IllegalStateException("Unable to write terminal output", e);
        }
    }

    private void insert(final String text) {
        try {
            getStyledDocument().insertString(cursor, text, attributes);
            cursor += text.length();
        } catch (final BadLocationException e) {
            throw new IllegalStateException("Unable to update terminal screen", e);
        }
    }

    private void eraseLine(final int mode) {
        final int start = lineStart(cursor);
        final int end = lineEnd(cursor);
        if (0 == mode) remove(cursor, end);
        else if (1 == mode) remove(start, cursor);
        else if (2 == mode) remove(start, end);
    }

    private void eraseScreen(final int mode) {
        final int end = getDocument().getLength();
        if (0 == mode) remove(cursor, end);
        else if (1 == mode) remove(0, cursor);
        else if (2 == mode) {
            remove(0, end);
            cursor = 0;
        }
    }

    private void eraseCharacters(final int amount) {
        remove(cursor, Math.min(lineEnd(cursor), cursor + amount));
    }

    private void deleteCharacters(final int amount) {
        final int end = Math.min(lineEnd(cursor), cursor + amount);
        remove(cursor, end);
    }

    private void remove(final int start, final int end) {
        if (end <= start) return;
        try {
            getStyledDocument().remove(start, end - start);
            if (cursor > start) cursor = Math.max(start, cursor - (end - start));
        } catch (final BadLocationException e) {
            throw new IllegalStateException("Unable to erase terminal output", e);
        }
    }

    private void trimScrollback() {
        final var root = getStyledDocument().getDefaultRootElement();
        final int excessLines = root.getElementCount() - MAX_SCROLLBACK_LINES;
        if (excessLines > 0) remove(0, Math.min(getStyledDocument().getLength(),
                root.getElement(excessLines - 1).getEndOffset()));
    }

    private void moveHorizontal(final int amount) {
        final int start = lineStart(cursor);
        final int end = lineEnd(cursor);
        cursor = Math.max(start, Math.min(end, cursor + amount));
    }

    private void moveVertical(final int amount) {
        moveToRow(row() + amount, column());
    }

    private void moveToColumn(final int targetColumn) {
        moveToRow(row(), targetColumn);
    }

    private void moveToRow(final int targetRow, final int targetColumn) {
        final StyledDocument document = getStyledDocument();
        while (document.getDefaultRootElement().getElementCount() <= targetRow && targetRow >= 0) {
            try {
                document.insertString(document.getLength(), "\n", attributes);
            } catch (final BadLocationException e) {
                throw new IllegalStateException("Unable to extend terminal screen", e);
            }
        }
        if (targetRow < 0) {
            cursor = 0;
            return;
        }
        final int rowIndex = Math.min(targetRow, document.getDefaultRootElement().getElementCount() - 1);
        final var rowElement = document.getDefaultRootElement().getElement(rowIndex);
        final int start = rowElement.getStartOffset();
        final int end = lineEnd(start);
        final int column = Math.max(0, targetColumn);
        if (column > end - start) {
            cursor = end;
            insert(" ".repeat(column - (end - start)));
        } else cursor = start + column;
    }

    private int row() {
        return getStyledDocument().getDefaultRootElement().getElementIndex(cursor);
    }

    private int column() {
        return cursor - lineStart(cursor);
    }

    private int lineStart(final int position) {
        final StyledDocument document = getStyledDocument();
        final var element = document.getDefaultRootElement().getElement(
                document.getDefaultRootElement().getElementIndex(Math.min(position, document.getLength())));
        return element.getStartOffset();
    }

    private int lineEnd(final int position) {
        final StyledDocument document = getStyledDocument();
        final var element = document.getDefaultRootElement().getElement(
                document.getDefaultRootElement().getElementIndex(Math.min(position, document.getLength())));
        int end = Math.min(document.getLength(), element.getEndOffset());
        if (end > element.getStartOffset()) {
            try {
                if ('\n' == document.getText(end - 1, 1).charAt(0)) end--;
            } catch (final BadLocationException e) {
                throw new IllegalStateException("Unable to read terminal screen", e);
            }
        }
        return end;
    }
}
