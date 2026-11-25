package works.lysenko.util.func.ui.snapshot;

/**
 * Immutable screen bounds parsed from a UI hierarchy snapshot (e.g. Android {@code [left,top][right,bottom]}).
 *
 * @param left   left X coordinate in pixels
 * @param top    top Y coordinate in pixels
 * @param right  right X coordinate in pixels
 * @param bottom bottom Y coordinate in pixels
 */
public record UiBounds(int left, int top, int right, int bottom) {

    /**
     * Empty/missing bounds constant.
     */
    public static final UiBounds EMPTY = new UiBounds(0, 0, 0, 0);

    /**
     * Parses Android UiAutomator bounds formatted as {@code [left,top][right,bottom]}.
     *
     * @param raw raw attribute value (may be null or blank)
     * @return parsed {@link UiBounds}, or {@link #EMPTY} if absent or malformed
     */
    public static UiBounds parse(final String raw) {

        if (null == raw || raw.length() < 9 || '[' != raw.charAt(0)) {
            return EMPTY;
        }
        final int comma1 = raw.indexOf(',', 1);
        final int close1 = (comma1 > 0) ? raw.indexOf(']', comma1 + 1) : -1;
        final int open2 = (close1 > 0) ? raw.indexOf('[', close1 + 1) : -1;
        final int comma2 = (open2 > 0) ? raw.indexOf(',', open2 + 1) : -1;
        final int close2 = (comma2 > 0) ? raw.indexOf(']', comma2 + 1) : -1;
        if (comma1 < 0 || close1 < 0 || open2 < 0 || comma2 < 0 || close2 < 0) {
            return EMPTY;
        }
        try {
            final int l = Integer.parseInt(raw, 1, comma1, 10);
            final int t = Integer.parseInt(raw, comma1 + 1, close1, 10);
            final int r = Integer.parseInt(raw, open2 + 1, comma2, 10);
            final int b = Integer.parseInt(raw, comma2 + 1, close2, 10);
            return new UiBounds(l, t, r, b);
        } catch (final NumberFormatException ignored) {
            return EMPTY;
        }
    }

    /**
     * @return non-negative width in pixels
     */
    public int width() {

        return Math.max(0, right - left);
    }

    /**
     * @return non-negative height in pixels
     */
    public int height() {

        return Math.max(0, bottom - top);
    }

    /**
     * @return {@code true} if width or height is zero
     */
    public boolean isEmpty() {

        return width() <= 0 || height() <= 0;
    }

    /**
     * @return horizontal center coordinate in pixels
     */
    public int centerX() {

        return left + width() / 2;
    }

    /**
     * @return vertical center coordinate in pixels
     */
    public int centerY() {

        return top + height() / 2;
    }

    /**
     * Computes an X coordinate at a relative horizontal offset within these bounds.
     *
     * @param ratio relative offset in {@code [0.0, 1.0]}
     * @return pixel X coordinate
     */
    public int relativeX(final double ratio) {

        return left + (int) Math.round(width() * ratio);
    }

    /**
     * Computes a Y coordinate at a relative vertical offset within these bounds.
     *
     * @param ratio relative offset in {@code [0.0, 1.0]}
     * @return pixel Y coordinate
     */
    public int relativeY(final double ratio) {

        return top + (int) Math.round(height() * ratio);
    }
}
