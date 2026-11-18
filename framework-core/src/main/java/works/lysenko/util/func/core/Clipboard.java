package works.lysenko.util.func.core;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import static works.lysenko.util.lang.U.UNABLE_TO_READ_CLIPBOARD;

/**
 * The Clipboard class provides functionality to read and write the content of the system clipboard as a string.
 */
@SuppressWarnings({"unused", "ClassIndependentOfModule"})
public record Clipboard() {

    private static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;

    /**
     * Retrieves the content of the system clipboard as a string.
     *
     * @return the content of the system clipboard as a string
     * @throws UnsupportedFlavorException if the requested data flavor is not available
     * @throws IOException                if an error occurs while reading the clipboard data
     */
    private static String getClipboardContentAsString() throws UnsupportedFlavorException, IOException {

        return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(STRING_FLAVOR);
    }

    /**
     * Reads the content of the system clipboard as a string.
     *
     * @return the content of the system clipboard as a string
     * @throws IllegalStateException if unable to read the clipboard
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    public static String readClipboard() {

        try {
            return getClipboardContentAsString();
        } catch (final UnsupportedFlavorException | IOException e) {
            throw new IllegalStateException(UNABLE_TO_READ_CLIPBOARD);
        }
    }

    /**
     * Writes the specified text string to the system clipboard.
     *
     * @param text string to copy to the clipboard
     */
    public static void writeClipboard(final String text) {

        if (null != text) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            } catch (final Exception ignored) {
            }
        }
    }
}
