package works.lysenko.base.ui;

@SuppressWarnings({"unused", "MissingJavadoc", "WeakerAccess"})
public class IntegerDocumentFilter extends IntegerRangeDocumentFilter {

    public IntegerDocumentFilter() {

        super(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
