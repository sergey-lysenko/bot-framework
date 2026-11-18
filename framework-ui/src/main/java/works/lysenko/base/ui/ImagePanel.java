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

import static java.util.Objects.isNull;

/**
 *
 */
@SuppressWarnings({"FieldHasSetterButNoGetter", "WeakerAccess"})
public final class ImagePanel extends JPanel implements _ImagePanel {

    private Image img;
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

        this.img = img;
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

        img = null;
        svgFrame = null;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void paintComponent(final Graphics g) {

        super.paintComponent(g);
        if (!isNull(svgFrame)) {
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                svgFrame.paint(this, graphics, scale);
            } finally {
                graphics.dispose();
            }
        } else if (!isNull(img)) {
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(img, 0, 0, (int) (img.getWidth(null) * scale), (int) (img.getHeight(null) * scale), this);
            } finally {
                graphics.dispose();
            }
        }
    }

    /**
     * Performs set image.
     * @param img the img
     */
    public void setImage(final Image img) {

        this.img = img;
        this.svgFrame = null;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    /**
     * Sets an SVG progression frame to be rendered directly via JSVG.
     *
     * @param svgFrame the parsed SVG frame, or null to clear
     */
    public void setSvgFrame(final ProgressionFrames.SvgFrame svgFrame) {

        this.svgFrame = svgFrame;
        this.img = null;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    /**
     * Performs set scale.
     * @param scale the scale
     */
    public void setScale(final float scale) {

        this.scale = scale;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    @Override
    public void fitTo(final Dimension bounds) {

        if (bounds.width <= 0 || bounds.height <= 0) return;

        final int imageWidth = contentWidth();
        final int imageHeight = contentHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return;

        final float widthScale = (float) bounds.width / imageWidth;
        final float heightScale = (float) bounds.height / imageHeight;
        setScale(Math.min(1.0F, Math.min(widthScale, heightScale)));
    }

    /**
     * Performs get image.
     * @return the get image result
     */
    public Image getImage() {

        return img;
    }

    /**
     * Returns the currently displayed SVG frame, if any.
     *
     * @return the SVG frame, or null
     */
    public ProgressionFrames.SvgFrame getSvgFrame() {

        return svgFrame;
    }

    /**
     * Performs get scale.
     * @return the get scale result
     */
    public float getScale() {

        return scale;
    }

    /**
     * Performs to image coordinates.
     * @param panelX the panelX
     * @param panelY the panelY
     * @return the to image coordinates result
     */
    public Point toImageCoordinates(final int panelX, final int panelY) {

        final int w = contentWidth();
        final int h = contentHeight();
        if (w <= 0 || h <= 0 || scale <= 0) return null;
        final int x = (int) (panelX / scale);
        final int y = (int) (panelY / scale);
        if (x >= 0 && x < w && y >= 0 && y < h) {
            return new Point(x, y);
        }
        return null;
    }

    private int contentWidth() {
        if (!isNull(svgFrame)) return svgFrame.width();
        if (!isNull(img)) return img.getWidth(null);
        return -1;
    }

    private int contentHeight() {
        if (!isNull(svgFrame)) return svgFrame.height();
        if (!isNull(img)) return img.getHeight(null);
        return -1;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private void updatePreferredSize() {

        final int width = contentWidth();
        final int height = contentHeight();
        if (width > 0 && height > 0) {
            setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
        }
    }
}
