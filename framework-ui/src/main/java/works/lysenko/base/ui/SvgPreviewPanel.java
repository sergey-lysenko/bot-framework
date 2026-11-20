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
public final class SvgPreviewPanel extends JPanel {

    private static final Color BACKGROUND = new Color(0x0F, 0x17, 0x2A);
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
    public SvgPreviewPanel(final String placeholderText, final String threadName) {
        super();
        setLayout(layout);
        executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });

        final JPanel placeholderPanel = new JPanel(new GridBagLayout());
        final JLabel placeholderLabel = new JLabel(placeholderText);
        placeholderLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(0x94, 0xA3, 0xB8));
        placeholderPanel.add(placeholderLabel);

        imagePanel.setBackground(BACKGROUND);
        final JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        viewport = scrollPane.getViewport();
        viewport.setBackground(BACKGROUND);
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent event) {
                fitToViewport();
            }
        });

        add(placeholderPanel, PLACEHOLDER);
        add(scrollPane, IMAGE);
    }

    /**
     * @return the panel that paints the current frame
     */
    public ImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Shows a raster frame, or the placeholder when {@code image} is {@code null}.
     *
     * @param image frame to display, or {@code null} to clear
     */
    public void setImage(final Image image) {
        pendingSvg.set(null);
        onEdt(() -> {
            if (null == image) {
                imagePanel.setImage(null);
                layout.show(this, PLACEHOLDER);
            } else {
                imagePanel.setImage(image);
                fitToViewport();
                layout.show(this, IMAGE);
            }
        });
    }

    /**
     * Shows an SVG frame, or the placeholder when {@code svgContent} is {@code null} or blank.
     * Callable from any thread.
     *
     * @param svgContent SVG source, or {@code null} to clear
     */
    public void setSvg(final String svgContent) {
        if (null == svgContent || svgContent.isBlank()) {
            pendingSvg.set(null);
            onEdt(() -> {
                imagePanel.setSvgFrame(null);
                layout.show(this, PLACEHOLDER);
            });
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            pendingSvg.set(null);
            try {
                applySvgFrame(ProgressionFrames.parseSvg(svgContent, ProgressionSettings.current().maxFramePixels()));
            } catch (final IOException ignored) {
            }
            return;
        }
        if (null == pendingSvg.getAndSet(svgContent)) {
            executor.submit(() -> {
                String next;
                while (null != (next = pendingSvg.getAndSet(null))) {
                    try {
                        final ProgressionFrames.SvgFrame frame =
                                ProgressionFrames.parseSvg(next, ProgressionSettings.current().maxFramePixels());
                        SwingUtilities.invokeLater(() -> applySvgFrame(frame));
                    } catch (final IOException ignored) {
                    }
                }
            });
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
