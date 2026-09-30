package works.lysenko.base.ui;

import works.lysenko.util.apis.util._ImagePanel;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;

import static java.util.Objects.isNull;

/**
 *
 */
@SuppressWarnings({"FieldHasSetterButNoGetter", "WeakerAccess"})
public final class ImagePanel extends JPanel implements _ImagePanel {

    private Image img;
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
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void paintComponent(final Graphics g) {

        super.paintComponent(g);
        if (!isNull(img)) {
            g.drawImage(img, 0, 0, (int) (img.getWidth(null) * scale), (int) (img.getHeight(null) * scale), this);
        }
    }

    public void setImage(final Image img) {

        this.img = img;
        updatePreferredSize();
    }

    public void setScale(final float scale) {

        this.scale = scale;
        updatePreferredSize();
    }

    public Image getImage() {

        return img;
    }

    public float getScale() {

        return scale;
    }

    public Point toImageCoordinates(final int panelX, final int panelY) {

        if (isNull(img) || scale <= 0) return null;
        final int x = (int) (panelX / scale);
        final int y = (int) (panelY / scale);
        if (x >= 0 && x < img.getWidth(null) && y >= 0 && y < img.getHeight(null)) {
            return new Point(x, y);
        }
        return null;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private void updatePreferredSize() {

        if (!isNull(img)) {
            final int width = (int) (img.getWidth(null) * scale);
            final int height = (int) (img.getHeight(null) * scale);
            setPreferredSize(new Dimension(width, height));
        }
    }
}
