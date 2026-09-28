package works.lysenko.base.ui;

import works.lysenko.util.spec.Symbols;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.Color;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * based on <a href="https://copyprogramming.com/howto/ansi-colors-in-java-swing-text-fields">this</a>
 */
@SuppressWarnings({"ClassWithoutConstructor", "ClassWithoutLogger", "ClassUnconnectedToPackage", "ClassHasNoToStringMethod",
        "ChainedMethodCall", "NestedMethodCall", "ClassWithTooManyTransitiveDependencies",
        "ClassWithTooManyTransitiveDependents",
        "CyclicClassDependency", "ImplicitNumericConversion", "FinalMethod"})
class AnsiTextPane extends JTextPane {

    private static final Color D_Black = Color.getHSBColor(0.00f, 0.0f, 0.00f);
    private static final Color D_Red = Color.getHSBColor(0.00f, 1.0f, 0.50f);
    private static final Color D_Blue = Color.getHSBColor(0.66f, 1.0f, 0.50f);
    private static final Color D_Magenta = Color.getHSBColor(0.83f, 1.0f, 0.50f);
    private static final Color D_Green = Color.getHSBColor(0.33f, 1.0f, 0.50f);
    private static final Color D_Yellow = Color.getHSBColor(0.16f, 1.0f, 0.50f);
    private static final Color D_Cyan = Color.getHSBColor(0.50f, 1.0f, 0.50f);
    private static final Color D_White = Color.getHSBColor(0.00f, 0.0f, 0.75f);
    private static final Color B_Black = Color.getHSBColor(0.00f, 0.0f, 0.50f);
    private static final Color B_Red = Color.getHSBColor(0.00f, 1.0f, 1.00f);
    private static final Color B_Blue = Color.getHSBColor(0.66f, 1.0f, 1.00f);
    private static final Color B_Magenta = Color.getHSBColor(0.83f, 1.0f, 1.00f);
    private static final Color B_Green = Color.getHSBColor(0.33f, 1.0f, 1.00f);
    private static final Color B_Yellow = Color.getHSBColor(0.16f, 1.0f, 1.00f);
    private static final Color B_Cyan = Color.getHSBColor(0.50f, 1.0f, 1.00f);
    private static final Color B_White = Color.getHSBColor(0.00f, 0.0f, 1.00f);
    private static final Color cReset = Color.getHSBColor(0.00f, 0.0f, 1.00f);
    private static Color colorCurrent = cReset;
    private String remaining = "";

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod", "MethodParameterNamingConvention",
            "HardCodedStringLiteral", "SwitchStatementWithTooManyBranches"})
    private static Color getANSIColor(final String ANSIColor) {

        return switch (ANSIColor) {
            case "\u001B[30m", "\u001B[0;30m" -> D_Black;
            case "\u001B[31m", "\u001B[0;31m" -> D_Red;
            case "\u001B[32m", "\u001B[0;32m" -> D_Green;
            case "\u001B[33m", "\u001B[0;33m" -> D_Yellow;
            case "\u001B[34m", "\u001B[0;34m" -> D_Blue;
            case "\u001B[35m", "\u001B[0;35m" -> D_Magenta;
            case "\u001B[36m", "\u001B[0;36m" -> D_Cyan;
            case "\u001B[37m", "\u001B[0;37m" -> D_White;
            case "\u001B[1;30m" -> B_Black;
            case "\u001B[1;31m" -> B_Red;
            case "\u001B[1;32m" -> B_Green;
            case "\u001B[1;33m" -> B_Yellow;
            case "\u001B[1;34m" -> B_Blue;
            case "\u001B[1;35m" -> B_Magenta;
            case "\u001B[1;36m" -> B_Cyan;
            case "\u001B[0m" -> cReset;
            default -> B_White;
        };
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "OverlyLongMethod", "SingleCharacterStringConcatenation",
            "ReassignedVariable",
            "ContinueStatement", "StringOperationCanBeSimplified", "AssignmentToStaticFieldFromInstanceMethod"})
    final void ansiSetText(final String s) {

        setText(EMPTY);
        int aPos = 0;   // current char position in addString
        int aIndex; // index of next Escape sequence
        int mIndex; // index of "m" terminating Escape sequence
        String tmpString;
        boolean stillSearching; // true until no more Escape sequences
        final String addString = s(remaining, s);
        remaining = EMPTY;
        if (!addString.isEmpty()) {
            aIndex = addString.indexOf("\u001B"); // find first escape
            if (-1 == aIndex) { // no escape/color change in this string, so just send it with current color
                append(colorCurrent, addString);
                return;
            }
// otherwise There is an escape character in the string, so we must process it
            if (0 < aIndex) { // Escape is not first char, so send text up to first escape
                tmpString = addString.substring(0, aIndex);
                append(colorCurrent, tmpString);
                aPos = aIndex;
            }
// aPos is now at the beginning of the first escape sequence
            stillSearching = true;
            while (stillSearching) {
                mIndex = addString.indexOf(Symbols.M, aPos); // find the end of the escape sequence
                if (0 > mIndex) { // the buffer ends halfway through the ansi string!
                    remaining = addString.substring(aPos, addString.length());
                    stillSearching = false;
                    continue;
                } else {
                    tmpString = addString.substring(aPos, mIndex + 1);
                    colorCurrent = getANSIColor(tmpString);
                }
                aPos = mIndex + 1;
// now we have the color, send text that is in that color (up to next escape)
                aIndex = addString.indexOf("\u001B", aPos);
                if (-1 == aIndex) { // if that was the last sequence of the input, send remaining text
                    tmpString = addString.substring(aPos, addString.length());
                    append(colorCurrent, tmpString);
                    stillSearching = false;
                    continue; // jump out of loop early, as the whole string has been sent now
                }
                // there is another escape sequence, so send part of the string and prepare for the next
                tmpString = addString.substring(aPos, aIndex);
                aPos = aIndex;
                append(colorCurrent, tmpString);
            } // while there's text in the input buffer
        }
    }

    @SuppressWarnings("StandardVariableNames")
    private void append(final Color c, final String s) {

        final StyleContext sc = StyleContext.getDefaultStyleContext();
        final AttributeSet aSet = sc.addAttribute(
                SimpleAttributeSet.EMPTY,
                StyleConstants.Foreground, c);
        final int len = getDocument().getLength(); // same value as getText().length();
        setCaretPosition(len);  // place caret at the end (with no selection)
        setCharacterAttributes(aSet, false);
        replaceSelection(s); // there is no selection, so inserts at caret
    }
}

