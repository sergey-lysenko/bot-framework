package works.lysenko.base.ui;

import works.lysenko.base.output.ProgressionFrames;
import works.lysenko.base.output.ProgressionSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A dashboard tab that previews the latest progression frame (SVG or raster) fitted to the viewport,
 * showing a placeholder message until the first frame arrives. SVG parsing happens off the EDT and
 * is coalesced so that only the most recent pending frame is rendered.
 */
final class SvgPreviewPanel extends JPanel {

    private static final Color BACKGROUND = new Color(0x0F, 0x17, 0x2A);
    private static final Color PLACEHOLDER_FOREGROUND = new Color(0x94, 0xA3, 0xB8);
    private static final Font PLACEHOLDER_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final int SCROLL_UNIT_INCREMENT = 16;
    private static final String PLACEHOLDER = "PLACEHOLDER";
    private static final String IMAGE = "IMAGE";

    private final CardLayout layout = new CardLayout();
    private final ImagePanel imagePanel = new ImagePanel();
    private final JViewport viewport;
    private final AtomicReference<String> pendingSvg = new AtomicReference<>();
    private final ExecutorService executor;

    /**
     * Creates a preview panel.
     *
     * @param placeholderText message shown while no frame is available
     * @param threadName      name of the background thread used for SVG parsing
     */
    SvgPreviewPanel(final String placeholderText, final String threadName) {
        setLayout(layout);
        executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });

        final JPanel placeholderPanel = new JPanel(new GridBagLayout());
        final JLabel placeholderLabel = new JLabel(placeholderText);
        placeholderLabel.setFont(PLACEHOLDER_FONT);
        placeholderLabel.setForeground(PLACEHOLDER_FOREGROUND);
        placeholderPanel.add(placeholderLabel);

        imagePanel.setBackground(BACKGROUND);
        final JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        viewport = scrollPane.getViewport();
        viewport.setBackground(BACKGROUND);
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                fitToViewport();
            }
        });

        add(placeholderPanel, PLACEHOLDER);
        add(scrollPane, IMAGE);
    }

    /**
     * @return the panel that paints the current frame
     */
    ImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Shows a raster frame, or the placeholder when {@code image} is {@code null}.
     *
     * @param image frame to display, or {@code null} to clear
     */
    void setImage(final Image image) {
        pendingSvg.set(null);
        onEdt(() -> applyImage(image));
    }

    /**
     * Shows an SVG frame, or the placeholder when {@code svgContent} is {@code null} or blank.
     * Callable from any thread.
     *
     * @param svgContent SVG source, or {@code null} to clear
     */
    void setSvg(final String svgContent) {
        if (null == svgContent || svgContent.isBlank()) {
            pendingSvg.set(null);
            onEdt(this::clearSvgFrame);
        } else if (SwingUtilities.isEventDispatchThread()) {
            pendingSvg.set(null);
            parseAndApplySvg(svgContent);
        } else if (null == pendingSvg.getAndSet(svgContent)) {
            executor.submit(this::drainPendingSvg);
        }
    }

    private void applyImage(final Image image) {
        if (null == image) {
            imagePanel.setImage(null);
            layout.show(this, PLACEHOLDER);
        } else {
            imagePanel.setImage(image);
            fitToViewport();
            layout.show(this, IMAGE);
        }
    }

    private void clearSvgFrame() {
        imagePanel.setSvgFrame(null);
        layout.show(this, PLACEHOLDER);
    }

    private void parseAndApplySvg(final String svgContent) {
        try {
            applySvgFrame(ProgressionFrames.parseSvg(svgContent, ProgressionSettings.current().maxFramePixels()));
        } catch (final IOException _) {
            // Ignore malformed SVG frames
        }
    }

    private void drainPendingSvg() {
        String next = pendingSvg.getAndSet(null);
        while (null != next) {
            parseAndScheduleSvg(next);
            next = pendingSvg.getAndSet(null);
        }
    }

    private void parseAndScheduleSvg(final String svgContent) {
        try {
            final ProgressionFrames.SvgFrame frame =
                    ProgressionFrames.parseSvg(svgContent, ProgressionSettings.current().maxFramePixels());
            SwingUtilities.invokeLater(() -> applySvgFrame(frame));
        } catch (final IOException _) {
            // Ignore malformed SVG frames
        }
    }

    private void applySvgFrame(final ProgressionFrames.SvgFrame frame) {
        imagePanel.setSvgFrame(frame);
        fitToViewport();
        layout.show(this, IMAGE);
    }

    private void fitToViewport() {
        imagePanel.fitTo(viewport.getExtentSize());
    }

    private static void onEdt(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
