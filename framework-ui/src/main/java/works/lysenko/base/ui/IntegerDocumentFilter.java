package works.lysenko.base.ui;

/**
 * The IntegerDocumentFilter class.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class IntegerDocumentFilter extends IntegerRangeDocumentFilter {

    /**
     * Constructs a new IntegerDocumentFilter.
     */
    public IntegerDocumentFilter() {

        super(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
