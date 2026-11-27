package works.lysenko.base.ui;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.QUS_MRK;
import static works.lysenko.util.spec.Symbols._LFD_;

@SuppressWarnings({"ClassWithoutConstructor", "ClassWithoutLogger", "ClassUnconnectedToPackage", "ClassHasNoToStringMethod",
        "ChainedMethodCall", "NestedMethodCall", "ClassWithTooManyTransitiveDependencies",
        "ClassWithTooManyTransitiveDependents", "CyclicClassDependency", "FinalMethod"})
final class AnsiTextPane extends JTextPane {

    static final Color DEFAULT_BACKGROUND = new Color(30, 30, 30);
    private static final Color DEFAULT_FOREGROUND = new Color(229, 229, 229);
    private static final int FONT_SIZE = 13;
    private static final char BACKSPACE = 8;
    private static final char HORIZONTAL_TAB = 9;
    private static final char CARRIAGE_RETURN = 13;
    private static final char ESCAPE = 27;
    private static final Map<Character, Consumer<TerminalScreen>> SINGLE_CONTROL_HANDLERS = Map.of(
            CARRIAGE_RETURN, TerminalScreen::moveToLineStart,
            _LFD_, TerminalScreen::advanceLine,
            BACKSPACE, TerminalScreen::moveBackOneChar,
            HORIZONTAL_TAB, TerminalScreen::advanceTab
    );

    private final TerminalScreen screen;
    private String pendingEscape = "";

    AnsiTextPane() {
        screen = new TerminalScreen(getStyledDocument());
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE));
        setForeground(DEFAULT_FOREGROUND);
        setBackground(DEFAULT_BACKGROUND);
        setCaretColor(DEFAULT_FOREGROUND);
        setEditable(false);
        setFocusable(false);
        setOpaque(true);
        final UIDefaults defaults = new UIDefaults();
        defaults.put("TextPane[Enabled].backgroundPainter", (Painter<JComponent>) (g, component, w, h) -> {
            g.setColor(component.getBackground());
            g.fillRect(0, 0, w, h);
        });
        putClientProperty("Nimbus.Overrides", defaults);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // getPreferredSize() would call back into this method, so ask the UI delegate directly
        return !(getParent() instanceof final JViewport viewport) || getUI().getPreferredSize(this).width <= viewport.getWidth();
    }

    void appendOutput(final String text) {
        final String input = s(pendingEscape, text);
        pendingEscape = "";
        process(input);
        if (pendingEscape.isEmpty() && !endsWithLineBreak(text)) process(s(_LFD_));
        screen.trimScrollback();
    }

    private static boolean endsWithLineBreak(final CharSequence text) {
        final boolean result;
        if (text.isEmpty()) {
            result = false;
        } else {
            final char last = text.charAt(text.length() - 1);
            result = _LFD_ == last || CARRIAGE_RETURN == last;
        }
        return result;
    }

    private void process(final String input) {
        int index = 0;
        while (index < input.length()) {
            index = processAt(input, index);
        }
    }

    private int processAt(final String input, final int index) {
        final char current = input.charAt(index);
        final int nextIndex;
        if (!Character.isISOControl(current)) {
            nextIndex = scanPlainTextEnd(input, index);
            screen.write(input.substring(index, nextIndex));
        } else if (ESCAPE == current) {
            nextIndex = processEscape(input, index);
        } else {
            handleSingleControlChar(current);
            nextIndex = index + 1;
        }
        return nextIndex;
    }

    private static int scanPlainTextEnd(final CharSequence input, final int start) {
        int end = start + 1;
        while (end < input.length() && !Character.isISOControl(input.charAt(end))) {
            end++;
        }
        return end;
    }

    private void handleSingleControlChar(final char current) {
        final Consumer<TerminalScreen> handler = SINGLE_CONTROL_HANDLERS.get(current);
        if (null != handler) {
            handler.accept(screen);
        }
    }

    private int processEscape(final String input, final int start) {
        final int nextIndex;
        if (start + 1 >= input.length()) {
            pendingEscape = input.substring(start);
            nextIndex = input.length();
        } else if ('[' != input.charAt(start + 1)) {
            screen.handleSimpleEscape(input.charAt(start + 1));
            nextIndex = start + 2;
        } else {
            nextIndex = processCsiSequence(input, start);
        }
        return nextIndex;
    }

    private int processCsiSequence(final String input, final int start) {
        final int end = CsiDispatcher.findTerminator(input, start + 2);
        final int nextIndex;
        if (end == input.length()) {
            pendingEscape = input.substring(start);
            nextIndex = input.length();
        } else {
            CsiDispatcher.dispatch(screen, input.substring(start + 2, end), input.charAt(end));
            nextIndex = end + 1;
        }
        return nextIndex;
    }

    private static final class TerminalScreen {

        private static final int TAB_SIZE = 8;
        private static final int MAX_SCROLLBACK_LINES = 1000;

        private final StyledDocument document;
        private final SgrStyle style = new SgrStyle();
        private int cursor;
        private int savedCursor;

        private TerminalScreen(final StyledDocument document) {
            this.document = document;
            cursor = 0;
            savedCursor = 0;
        }

        private void handleSimpleEscape(final char code) {
            if ('7' == code) savedCursor = cursor;
            if ('8' == code) cursor = savedCursor;
        }

        private void moveToLineStart() {
            cursor = lineStart(cursor);
        }

        private void moveBackOneChar() {
            cursor = Math.max(lineStart(cursor), cursor - 1);
        }

        private void advanceTab() {
            final int spaces = TAB_SIZE - (cursor - lineStart(cursor)) % TAB_SIZE;
            moveToColumn(column() + spaces);
        }

        private void advanceLine() {
            cursor = lineEnd(cursor);
            if (cursor < document.getLength()) cursor++;
            else insert(s(_LFD_));
        }

        private void applyGraphics(final int[] values) {
            style.apply(values);
        }

        private void saveCursor() {
            savedCursor = cursor;
        }

        private void restoreCursor() {
            cursor = Math.min(savedCursor, document.getLength());
        }

        private void insertSpaces(final int count) {
            insert(" ".repeat(count));
        }

        private void write(final String text) {
            final int end = lineEnd(cursor);
            final int overwrite = Math.clamp(end - cursor, 0, text.length());
            try {
                if (0 < overwrite) document.remove(cursor, overwrite);
                document.insertString(cursor, text, style.attributes());
                cursor += text.length();
            } catch (final BadLocationException e) {
                throw new IllegalStateException("Unable to write terminal output", e);
            }
        }

        private void insert(final String text) {
            try {
                document.insertString(cursor, text, style.attributes());
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
            final int end = document.getLength();
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
            if (end > start) {
                try {
                    document.remove(start, end - start);
                    if (cursor > start) cursor = Math.max(start, cursor - (end - start));
                } catch (final BadLocationException e) {
                    throw new IllegalStateException("Unable to erase terminal output", e);
                }
            }
        }

        private void trimScrollback() {
            final var root = document.getDefaultRootElement();
            final int excessLines = root.getElementCount() - MAX_SCROLLBACK_LINES;
            if (0 < excessLines) remove(0, Math.min(document.getLength(),
                    root.getElement(excessLines - 1).getEndOffset()));
        }

        private void moveHorizontal(final int amount) {
            final int start = lineStart(cursor);
            final int end = lineEnd(cursor);
            cursor = Math.clamp(cursor + amount, start, end);
        }

        private void moveVertical(final int amount) {
            moveToRow(row() + amount, column());
        }

        private void moveToColumn(final int targetColumn) {
            moveToRow(row(), targetColumn);
        }

        private void moveToRow(final int targetRow, final int targetColumn) {
            if (0 > targetRow) {
                cursor = 0;
            } else {
                extendRows(targetRow);
                final int rowIndex = Math.min(targetRow, document.getDefaultRootElement().getElementCount() - 1);
                final var rowElement = document.getDefaultRootElement().getElement(rowIndex);
                final int start = rowElement.getStartOffset();
                final int end = lineEnd(start);
                final int column = Math.max(0, targetColumn);
                if (column > end - start) {
                    cursor = end;
                    insertSpaces(column - (end - start));
                } else {
                    cursor = start + column;
                }
            }
        }

        private void extendRows(final int targetRow) {
            while (document.getDefaultRootElement().getElementCount() <= targetRow) {
                try {
                    document.insertString(document.getLength(), s(_LFD_), style.attributes());
                } catch (final BadLocationException e) {
                    throw new IllegalStateException("Unable to extend terminal screen", e);
                }
            }
        }

        private int row() {
            return document.getDefaultRootElement().getElementIndex(cursor);
        }

        private int column() {
            return cursor - lineStart(cursor);
        }

        private int lineStart(final int position) {
            final var element = document.getDefaultRootElement().getElement(
                    document.getDefaultRootElement().getElementIndex(Math.min(position, document.getLength())));
            return element.getStartOffset();
        }

        private int lineEnd(final int position) {
            final var element = document.getDefaultRootElement().getElement(
                    document.getDefaultRootElement().getElementIndex(Math.min(position, document.getLength())));
            int end = Math.min(document.getLength(), element.getEndOffset());
            if (end > element.getStartOffset()) {
                try {
                    if (_LFD_ == document.getText(end - 1, 1).charAt(0)) end--;
                } catch (final BadLocationException e) {
                    throw new IllegalStateException("Unable to read terminal screen", e);
                }
            }
            return end;
        }
    }

    private static final class SgrStyle {

        private static final Color[] COLORS = {
                new Color(0, 0, 0), new Color(205, 49, 49), new Color(13, 188, 121), new Color(229, 229, 16),
                new Color(36, 114, 200), new Color(188, 63, 188), new Color(17, 168, 205), new Color(229, 229, 229)
        };
        private static final Color[] BRIGHT_COLORS = {
                new Color(102, 102, 102), new Color(241, 76, 76), new Color(35, 209, 139), new Color(245, 245, 67),
                new Color(59, 142, 234), new Color(214, 112, 214), new Color(41, 184, 219), new Color(255, 255, 255)
        };
        private static final int SGR_BOLD_OFF = 22;
        private static final int SGR_ITALIC_OFF = 23;
        private static final int SGR_UNDERLINE_OFF = 24;
        private static final int SGR_FG_MIN = 30;
        private static final int SGR_FG_MAX = 37;
        private static final int SGR_FG_EXTENDED = 38;
        private static final int SGR_FG_DEFAULT = 39;
        private static final int SGR_BG_MIN = 40;
        private static final int SGR_BG_MAX = 47;
        private static final int SGR_BG_EXTENDED = 48;
        private static final int SGR_BG_DEFAULT = 49;
        private static final int SGR_BRIGHT_FG_MIN = 90;
        private static final int SGR_BRIGHT_FG_MAX = 97;
        private static final int SGR_BRIGHT_BG_MIN = 100;
        private static final int SGR_BRIGHT_BG_MAX = 107;
        private static final int MAX_COLOR_VALUE = 255;
        private static final int ANSI_BASIC_COLOR_COUNT = 8;
        private static final int ANSI_EXTENDED_COLOR_START = 16;
        private static final int ANSI_GRAYSCALE_START = 232;
        private static final int COLOR_CUBE_STRIDE = 36;
        private static final int COLOR_CUBE_BASE = 55;
        private static final int COLOR_CUBE_STEP = 40;
        private static final Map<Integer, Consumer<SgrStyle>> DECORATION_HANDLERS = Map.of(
                0, SgrStyle::reset,
                1, style -> style.setBold(true),
                3, style -> StyleConstants.setItalic(style.attributes(), true),
                4, style -> StyleConstants.setUnderline(style.attributes(), true),
                SGR_BOLD_OFF, style -> style.setBold(false),
                SGR_ITALIC_OFF, style -> StyleConstants.setItalic(style.attributes(), false),
                SGR_UNDERLINE_OFF, style -> StyleConstants.setUnderline(style.attributes(), false),
                SGR_FG_DEFAULT, style -> StyleConstants.setForeground(style.attributes(), DEFAULT_FOREGROUND),
                SGR_BG_DEFAULT, style -> style.attributes().removeAttribute(StyleConstants.Background)
        );

        private final MutableAttributeSet attributes = new SimpleAttributeSet();
        private boolean bold;

        private SgrStyle() {
            bold = false;
            StyleConstants.setForeground(attributes, DEFAULT_FOREGROUND);
            attributes.removeAttribute(StyleConstants.Background);
        }

        private MutableAttributeSet attributes() {
            return attributes;
        }

        private void apply(final int[] parameters) {
            final int count = parameters.length;
            int index = 0;
            while (index < count) {
                index += applySingle(parameters, index, count);
            }
        }

        private int applySingle(final int[] parameters, final int index, final int count) {
            final int parameter = parameters[index];
            int advance = 1;
            if (SGR_FG_EXTENDED == parameter || SGR_BG_EXTENDED == parameter) {
                advance = applyExtendedColor(parameters, index, count, SGR_FG_EXTENDED == parameter);
            } else {
                applyStandardCode(parameter);
            }
            return advance;
        }

        private void applyStandardCode(final int parameter) {
            final Consumer<SgrStyle> decoration = DECORATION_HANDLERS.get(parameter);
            if (null != decoration) {
                decoration.accept(this);
            } else {
                applyForegroundColor(parameter);
                applyBackgroundColor(parameter);
            }
        }

        private void applyForegroundColor(final int parameter) {
            if (SGR_FG_MIN <= parameter && SGR_FG_MAX >= parameter) {
                StyleConstants.setForeground(attributes, color(parameter - SGR_FG_MIN));
            } else if (SGR_BRIGHT_FG_MIN <= parameter && SGR_BRIGHT_FG_MAX >= parameter) {
                StyleConstants.setForeground(attributes, BRIGHT_COLORS[parameter - SGR_BRIGHT_FG_MIN]);
            }
        }

        private void applyBackgroundColor(final int parameter) {
            if (SGR_BG_MIN <= parameter && SGR_BG_MAX >= parameter) {
                setColor(false, COLORS[parameter - SGR_BG_MIN]);
            } else if (SGR_BRIGHT_BG_MIN <= parameter && SGR_BRIGHT_BG_MAX >= parameter) {
                setColor(false, BRIGHT_COLORS[parameter - SGR_BRIGHT_BG_MIN]);
            }
        }

        private int applyExtendedColor(final int[] parameters, final int index, final int count, final boolean foreground) {
            int advance = 1;
            if (index + 2 < count) {
                final int mode = parameters[index + 1];
                if (5 == mode) {
                    setColor(foreground, indexedColor(parameters[index + 2]));
                    advance = 3;
                } else if (2 == mode && index + 4 < count) {
                    setColor(foreground, createRgbColor(parameters[index + 2], parameters[index + 3], parameters[index + 4]));
                    advance = 5;
                }
            }
            return advance;
        }

        private void reset() {
            bold = false;
            StyleConstants.setBold(attributes, false);
            StyleConstants.setItalic(attributes, false);
            StyleConstants.setUnderline(attributes, false);
            StyleConstants.setForeground(attributes, DEFAULT_FOREGROUND);
            attributes.removeAttribute(StyleConstants.Background);
        }

        private void setBold(final boolean enabled) {
            bold = enabled;
            StyleConstants.setBold(attributes, enabled);
        }

        private void setColor(final boolean foreground, final Color color) {
            if (foreground) {
                StyleConstants.setForeground(attributes, color);
            } else if (DEFAULT_BACKGROUND.equals(color)) {
                attributes.removeAttribute(StyleConstants.Background);
            } else {
                StyleConstants.setBackground(attributes, color);
            }
        }

        private Color color(final int index) {
            return bold ? BRIGHT_COLORS[index] : COLORS[index];
        }

        private static Color createRgbColor(final int red, final int green, final int blue) {
            return new Color(clampColor(red), clampColor(green), clampColor(blue));
        }

        private static int clampColor(final int component) {
            return Math.clamp(component, 0, MAX_COLOR_VALUE);
        }

        private static Color indexedColor(final int index) {
            final int safeIndex = Math.clamp(index, 0, MAX_COLOR_VALUE);
            return ANSI_EXTENDED_COLOR_START > safeIndex ? paletteColor(safeIndex) : extendedColor(safeIndex);
        }

        private static Color paletteColor(final int index) {
            return ANSI_BASIC_COLOR_COUNT > index ? COLORS[index] : BRIGHT_COLORS[index - ANSI_BASIC_COLOR_COUNT];
        }

        private static Color extendedColor(final int index) {
            final Color result;
            if (ANSI_GRAYSCALE_START > index) {
                final int value = index - ANSI_EXTENDED_COLOR_START;
                result = new Color(colorCube(value / COLOR_CUBE_STRIDE), colorCube(value / 6 % 6), colorCube(value % 6));
            } else {
                final int gray = ANSI_BASIC_COLOR_COUNT + (index - ANSI_GRAYSCALE_START) * 10;
                result = new Color(gray, gray, gray);
            }
            return result;
        }

        private static int colorCube(final int component) {
            return 0 == component ? 0 : COLOR_CUBE_BASE + component * COLOR_CUBE_STEP;
        }
    }

    private static final class CsiDispatcher {

        private static final int CSI_FINAL_MIN = 64;
        private static final int CSI_FINAL_MAX = 126;
        private static final int CSI_INSERT_BLANK = 64;
        private static final int CSI_CURSOR_UP = 65;
        private static final int CSI_CURSOR_DOWN = 66;
        private static final int CSI_CURSOR_FORWARD = 67;
        private static final int CSI_CURSOR_BACK = 68;
        private static final int CSI_CURSOR_NEXT_LINE = 69;
        private static final int CSI_CURSOR_PREV_LINE = 70;
        private static final int CSI_CURSOR_HORIZONTAL = 71;
        private static final int CSI_CURSOR_POSITION = 72;
        private static final int CSI_ERASE_DISPLAY = 74;
        private static final int CSI_ERASE_LINE = 75;
        private static final int CSI_DELETE_CHARS = 80;
        private static final int CSI_ERASE_CHARS = 88;
        private static final int CSI_HORIZONTAL_POS = 96;
        private static final int CSI_HORIZONTAL_REL = 97;
        private static final int CSI_VERTICAL_POS = 100;
        private static final int CSI_VERTICAL_REL = 101;
        private static final int CSI_HORIZONTAL_VERT_POS = 102;
        private static final int CSI_SGR = 109;
        private static final int CSI_SAVE_CURSOR = 115;
        private static final int CSI_RESTORE_CURSOR = 117;
        private static final int MAX_INSERT_SPACES = 1000;
        private static final int HANDLER_CAPACITY = 32;
        private static final Map<Integer, BiConsumer<TerminalScreen, int[]>> HANDLERS = createHandlers();

        private static Map<Integer, BiConsumer<TerminalScreen, int[]>> createHandlers() {
            final Map<Integer, BiConsumer<TerminalScreen, int[]>> map = new HashMap<>(HANDLER_CAPACITY);
            map.put(CSI_CURSOR_UP, (screen, values) -> screen.moveVertical(-parameter(values, 0, 1)));
            map.put(CSI_CURSOR_DOWN, (screen, values) -> screen.moveVertical(parameter(values, 0, 1)));
            map.put(CSI_VERTICAL_REL, (screen, values) -> screen.moveVertical(parameter(values, 0, 1)));
            map.put(CSI_CURSOR_FORWARD, (screen, values) -> screen.moveHorizontal(parameter(values, 0, 1)));
            map.put(CSI_HORIZONTAL_REL, (screen, values) -> screen.moveHorizontal(parameter(values, 0, 1)));
            map.put(CSI_CURSOR_BACK, (screen, values) -> screen.moveHorizontal(-parameter(values, 0, 1)));
            map.put(CSI_CURSOR_NEXT_LINE, (screen, values) -> screen.moveToRow(screen.row() + parameter(values, 0, 1), 0));
            map.put(CSI_CURSOR_PREV_LINE, (screen, values) -> screen.moveToRow(screen.row() - parameter(values, 0, 1), 0));
            map.put(CSI_CURSOR_HORIZONTAL, (screen, values) -> screen.moveToColumn(parameter(values, 0, 1) - 1));
            map.put(CSI_HORIZONTAL_POS, (screen, values) -> screen.moveToColumn(parameter(values, 0, 1) - 1));
            map.put(CSI_CURSOR_POSITION, (screen, values) -> screen.moveToRow(parameter(values, 0, 1) - 1, parameter(values, 1, 1) - 1));
            map.put(CSI_HORIZONTAL_VERT_POS, (screen, values) -> screen.moveToRow(parameter(values, 0, 1) - 1, parameter(values, 1, 1) - 1));
            map.put(CSI_ERASE_DISPLAY, (screen, values) -> screen.eraseScreen(parameter(values, 0, 0)));
            map.put(CSI_ERASE_LINE, (screen, values) -> screen.eraseLine(parameter(values, 0, 0)));
            map.put(CSI_DELETE_CHARS, (screen, values) -> screen.deleteCharacters(parameter(values, 0, 1)));
            map.put(CSI_ERASE_CHARS, (screen, values) -> screen.eraseCharacters(parameter(values, 0, 1)));
            map.put(CSI_VERTICAL_POS, (screen, values) -> screen.moveToRow(parameter(values, 0, 1) - 1, screen.column()));
            map.put(CSI_SGR, TerminalScreen::applyGraphics);
            map.put(CSI_SAVE_CURSOR, (screen, _) -> screen.saveCursor());
            map.put(CSI_RESTORE_CURSOR, (screen, _) -> screen.restoreCursor());
            map.put(CSI_INSERT_BLANK, (screen, values) -> screen.insertSpaces(Math.min(parameter(values, 0, 1), MAX_INSERT_SPACES)));
            return Map.copyOf(map);
        }

        private static int findTerminator(final CharSequence input, final int start) {
            int end = start;
            while (end < input.length() && isParameterChar(input.charAt(end))) {
                end++;
            }
            return end;
        }

        private static boolean isParameterChar(final int code) {
            return CSI_FINAL_MIN > code || CSI_FINAL_MAX < code;
        }

        private static void dispatch(final TerminalScreen screen, final String parameters, final char command) {
            final BiConsumer<TerminalScreen, int[]> handler = HANDLERS.get((int) command);
            if (null != handler) {
                handler.accept(screen, parseParameters(parameters));
            }
        }

        private static int[] parseParameters(final String parameters) {
            final String questionMark = s(QUS_MRK);
            final String normalized = parameters.isEmpty() || Objects.equals(questionMark, parameters)
                    ? "0" : parameters.replace(questionMark, "");
            final String[] parts = normalized.split(";", -1);
            final int count = parts.length;
            final int[] result = new int[count];
            for (int i = 0; i < count; i++) {
                result[i] = parseSingleParameter(parts[i]);
            }
            return result;
        }

        private static int parseSingleParameter(final String part) {
            int value = 0;
            if (!part.isEmpty()) {
                try {
                    value = Integer.parseInt(part);
                } catch (final NumberFormatException _) {
                    // Invalid numeric parameters default to 0.
                }
            }
            return value;
        }

        private static int parameter(final int[] parameters, final int index, final int defaultValue) {
            return index >= parameters.length || 0 == parameters[index] ? defaultValue : parameters[index];
        }
    }
}
