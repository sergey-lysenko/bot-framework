package works.lysenko.base.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import static works.lysenko.util.data.strs.Swap.s;

/**
 * The IntegerRangeDocumentFilter class is a custom DocumentFilter implementation
 * that restricts the input of a document to integer values within a specified range.
 * It extends the DocumentFilter class.
 */
@SuppressWarnings({"QuestionableName", "PublicMethodNotExposedInInterface"})
public class IntegerRangeDocumentFilter extends DocumentFilter {

    private final int minimum;
    private final int maximum;

    /**
     * The IntegerRangeDocumentFilter class is a custom DocumentFilter implementation
     * that restricts the input of a document to integer values within a specified range.
     * It extends the DocumentFilter class.
     *
     * @param minimum The minimum value allowed for input.
     * @param maximum The maximum value allowed for input.
     */
    @SuppressWarnings("WeakerAccess")
    public IntegerRangeDocumentFilter(final int minimum, final int maximum) {

        this.minimum = minimum;
        this.maximum = maximum;
    }

    @SuppressWarnings("QuestionableName")
    @Override
    public final void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr) throws BadLocationException {

        final String newStr = s(fb.getDocument().getText(0, fb.getDocument().getLength()), string);
        verifyInput(newStr, fb, offset, 0, string, attr);
    }

    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Override
    public final void replace(final FilterBypass fb, final int offset, final int length, final String string,
                              final AttributeSet attrs) throws BadLocationException {

        final String text = fb.getDocument().getText(0, fb.getDocument().getLength());
        final String newStr = s(text.substring(0, offset), string, text.substring(offset + length));
        verifyInput(newStr, fb, offset, length, string, attrs);
    }

    private boolean isInRange(final int i) {

        return (minimum <= i && i <= maximum);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    private void verifyInput(final String newStr, final FilterBypass fb, final int offset, final int length,
                             final String source, final AttributeSet attr) throws BadLocationException {

        try {
            final int input = Integer.parseInt(newStr);
            if (isInRange(input)) {
                super.replace(fb, offset, length, source, attr);
            }
        } catch (final NumberFormatException e) {
            // input wasn't an integer, ignore
        }
    }
}
