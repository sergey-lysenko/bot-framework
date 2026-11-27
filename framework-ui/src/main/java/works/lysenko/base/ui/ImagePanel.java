package works.lysenko.base.ui;

import works.lysenko.base.output.ProgressionFrames;
import works.lysenko.util.apis.util._ImagePanel;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;

import static java.util.Objects.nonNull;

/**
 *
 */
@SuppressWarnings({"FieldHasSetterButNoGetter", "WeakerAccess"})
public final class ImagePanel extends JPanel implements _ImagePanel {

    private Image image;
    private ProgressionFrames.SvgFrame svgFrame;
    private float scale = 1.0F;

    /**
     * Creates an ImagePanel with the provided image file path.
     *
     * @param img the image file path to be displayed in the panel
     */
    @SuppressWarnings("unused")
    public ImagePanel(final String img) {

        this(new ImageIcon(img).getImage());
    }

    /**
     * Creates an ImagePanel with the given Image.
     *
     * @param img the Image to be displayed in the panel
     */
    public ImagePanel(final Image img) {

        image = img;
        svgFrame = null;
        final Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
        setLayout(null);
    }

    /**
     * Represents an ImagePanel that extends JPanel and implements _ImagePanel.
     */
    public ImagePanel() {

        image = null;
        svgFrame = null;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void paintComponent(final Graphics g) {

        super.paintComponent(g);
        if (nonNull(svgFrame)) {
            final Graphics2D graphics = (Graphics2D) g.create();
            if (nonNull(graphics)) {
                try {
                    svgFrame.paint(this, graphics, scale);
                } finally {
                    graphics.dispose();
                }
            }
        } else if (nonNull(image)) {
            final Graphics2D graphics = (Graphics2D) g.create();
            if (nonNull(graphics)) {
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(image, 0, 0, (int) (image.getWidth(null) * scale), (int) (image.getHeight(null) * scale), this);
                } finally {
                    graphics.dispose();
                }
            }
        }
    }

    /**
     * Performs set image.
     *
     * @param img the img
     */
    @Override
    public void setImage(final Image img) {

        image = img;
        svgFrame = null;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    /**
     * Sets an SVG progression frame to be rendered directly via JSVG.
     *
     * @param svgFrame the parsed SVG frame, or null to clear
     */
    void setSvgFrame(final ProgressionFrames.SvgFrame svgFrame) {

        this.svgFrame = svgFrame;
        image = null;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    /**
     * Performs set scale.
     *
     * @param scale the scale
     */
    @Override
    public void setScale(final float scale) {

        this.scale = scale;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    @Override
    public void fitTo(final Dimension bounds) {

        if (0 < bounds.width && 0 < bounds.height) {
            final int imageWidth = contentWidth();
            final int imageHeight = contentHeight();
            if (0 < imageWidth && 0 < imageHeight) {
                final float widthScale = (float) bounds.width / imageWidth;
                final float heightScale = (float) bounds.height / imageHeight;
                setScale(Math.min(1.0F, Math.min(widthScale, heightScale)));
            }
        }
    }

    /**
     * Performs get image.
     *
     * @return the get image result
     */
    Image getImage() {

        return image;
    }

    /**
     * Returns the currently displayed SVG frame, if any.
     *
     * @return the SVG frame, or null
     */
    ProgressionFrames.SvgFrame getSvgFrame() {

        return svgFrame;
    }

    /**
     * Performs get scale.
     *
     * @return the get scale result
     */
    float getScale() {

        return scale;
    }

    /**
     * Performs to image coordinates.
     *
     * @param panelX the panelX
     * @param panelY the panelY
     * @return the to image coordinates result
     */
    Point toImageCoordinates(final int panelX, final int panelY) {

        Point result = null;
        final int w = contentWidth();
        final int h = contentHeight();
        if (isCoordinateMappingReady(w, h) && isNonNegativePoint(panelX, panelY)) {
            final int x = Math.toIntExact(Math.round(Math.floor(panelX / scale)));
            final int y = Math.toIntExact(Math.round(Math.floor(panelY / scale)));
            if (isWithinBounds(x, w) && isWithinBounds(y, h)) {
                result = new Point(x, y);
            }
        }
        return result;
    }

    private boolean isCoordinateMappingReady(final int w, final int h) {
        return 0 < w && 0 < h && 0.0F < scale;
    }

    private static boolean isNonNegativePoint(final int panelX, final int panelY) {
        return 0 <= panelX && 0 <= panelY;
    }

    private static boolean isWithinBounds(final int value, final int limit) {
        return 0 <= value && value < limit;
    }

    private int contentWidth() {
        final int width;
        if (nonNull(svgFrame)) {
            width = svgFrame.width();
        } else if (nonNull(image)) {
            width = image.getWidth(null);
        } else {
            width = -1;
        }
        return width;
    }

    private int contentHeight() {
        final int height;
        if (nonNull(svgFrame)) {
            height = svgFrame.height();
        } else if (nonNull(image)) {
            height = image.getHeight(null);
        } else {
            height = -1;
        }
        return height;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private void updatePreferredSize() {

        final int width = contentWidth();
        final int height = contentHeight();
        if (0 < width && 0 < height) {
            setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
        }
    }
}
