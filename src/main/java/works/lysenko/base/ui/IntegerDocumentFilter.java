package works.lysenko.base.ui;

@SuppressWarnings({"unused", "MissingJavadoc", "WeakerAccess"})
/**
 * The IntegerDocumentFilter class.
 */
public class IntegerDocumentFilter extends IntegerRangeDocumentFilter {

    /**
     * Constructs a new IntegerDocumentFilter.
     */
    public IntegerDocumentFilter() {

        super(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
