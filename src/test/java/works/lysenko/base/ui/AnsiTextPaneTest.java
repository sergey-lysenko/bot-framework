package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnsiTextPaneTest {

    @Test
    void carriageReturnRewritesCurrentLineAndAnsiSetsForeground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final AnsiTextPane pane = new AnsiTextPane();
            pane.appendOutput("Progress 10%\r\u001B[38;5;196mProgress 20%\u001B[0m");

            assertEquals("Progress 20%\n", pane.getText());
            assertEquals(Color.RED,
                    StyleConstants.getForeground(pane.getStyledDocument().getCharacterElement(0).getAttributes()));
        });
    }

    @Test
    void preferredSizeInsideScrollPaneDoesNotRecurse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final AnsiTextPane pane = new AnsiTextPane();
            final javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
            scroll.setSize(400, 200);
            pane.appendOutput("x".repeat(500));
            scroll.doLayout();
            assertEquals(false, pane.getScrollableTracksViewportWidth());
        });
    }

    @Test
    void cursorMovementAndEraseLineUpdateExistingScreen() throws Exception {
        final AtomicReference<String> text = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            final AnsiTextPane pane = new AnsiTextPane();
            pane.appendOutput("first\nsecond");
            pane.appendOutput("\r\u001B[1A\u001B[2C!\u001B[K");
            text.set(pane.getText());
        });

        assertEquals("first\nse!\n", text.get());
    }
}
