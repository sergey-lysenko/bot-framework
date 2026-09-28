package works.lysenko.util.data.records;

/**
 * Represents an element with x, y coordinates and a colour value.
 */
public record Element(int x, int y, int colour) {

    /**
     * Constructs and returns a new Element object with the specified x, y, and color values.
     *
     * @param x      the x coordinate of the Element
     * @param y      the y coordinate of the Element
     * @param colour the colour value of the Element
     * @return a new Element object with the given x, y, and colour values
     */
    public static Element e(final int x, final int y, final int colour) {

        return new Element(x, y, colour);
    }
}
