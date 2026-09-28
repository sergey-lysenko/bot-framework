package works.lysenko.util.func;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.D.DIFFERENCE;
import static works.lysenko.util.spec.Symbols.B;
import static works.lysenko.util.spec.Symbols.G;
import static works.lysenko.util.spec.Symbols.R;

/**
 * Various graphics routines
 */
@SuppressWarnings({"unused", "MethodWithMultipleReturnPoints", "MethodWithMultipleLoops",
        "QuestionableName", "StaticMethodOnlyUsedInOneClass"})
public record Imgs() {

    /**
     * Checks if two BufferedImage objects are identical by comparing their pixel differences.
     * The method determines whether the color difference between the two images at all pixels
     * is zero by internally using the compareImages method.
     *
     * @param one the first BufferedImage to compare
     * @param two the second BufferedImage to compare
     * @return true if the two BufferedImage objects are identical; false otherwise
     */
    public static boolean areIdentical(final BufferedImage one, final BufferedImage two) {

        final Color result = compareImages(one, two);
        final int rgb = result.getRGB();
        return (new Color(0, 0, 0).getRGB() == rgb);
    }

    /**
     * Compares two BufferedImages to check if they are exactly the same.
     *
     * @param one the first BufferedImage to compare
     * @param two the second BufferedImage to compare
     * @return true if the two BufferedImages are the same, false otherwise
     */
    @SuppressWarnings({"MethodCallInLoopCondition", "MethodWithMultipleLoops", "MethodWithMultipleReturnPoints",
            "QuestionableName"})
    public static boolean areSameImages(final BufferedImage one, final BufferedImage two) {

        if (one.getWidth() == two.getWidth() && one.getHeight() == two.getHeight())
            for (int x = 0; x < one.getWidth(); x++)
                for (int y = 0; y < one.getHeight(); y++) {
                    if (one.getRGB(x, y) != two.getRGB(x, y))
                        return false;
                }
        else return false;
        return true;
    }

    /**
     * Compares two BufferedImages to determine if they are similar based on a specified color difference tolerance.
     * This method iterates over each pixel in the images, comparing their RGB values and ensuring the difference
     * remains within the specified tolerance.
     *
     * @param one       the first BufferedImage to compare
     * @param two       the second BufferedImage to compare
     * @param tolerance an integer value specifying the maximum allowable difference in RGB values for the images to be
     *                  considered similar
     * @return true if both images have the same dimensions and their pixel-by-pixel RGB differences are within the
     * specified tolerance; false otherwise
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    public static boolean areSimilarImages(final BufferedImage one, final BufferedImage two, final int tolerance) {

        if (one.getWidth() == two.getWidth() && one.getHeight() == two.getHeight()) {
            for (int x = 0; x < one.getWidth(); x++) {
                for (int y = 0; y < one.getHeight(); y++) {

                    final Color colorOne = new Color(one.getRGB(x, y));
                    final Color colorTwo = new Color(two.getRGB(x, y));

                    if (Math.abs(colorOne.getRed() - colorTwo.getRed()) > tolerance ||
                            Math.abs(colorOne.getGreen() - colorTwo.getGreen()) > tolerance ||
                            Math.abs(colorOne.getBlue() - colorTwo.getBlue()) > tolerance) {
                        return false;
                    }
                }
            }
        } else return false;
        return true;
    }

    /**
     * Compares two images to determine if they are similar based on a grid-based comparison approach.
     * For each pixel in the first image, this method checks whether there is a corresponding pixel
     * in the second image, within a defined 3x3 grid and within the specified color tolerance.
     *
     * @param one       the first BufferedImage to compare
     * @param two       the second BufferedImage to compare
     * @param tolerance an integer value specifying the maximum allowable difference in RGB values
     *                  for the images to be considered similar
     * @return true if the images have the same dimensions and every pixel in the first image has
     * a corresponding nearby match in the second image within the specified tolerance;
     * false otherwise
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "OverlyNestedMethod", "OverlyComplexMethod", "QuestionableName",
            "ObjectAllocationInLoop", "BooleanVariableAlwaysNegated", "LabeledStatement", "BreakStatementWithLabel",
            "BreakStatement"})
    public static boolean areSimilarImagesGridBased(final BufferedImage one, final BufferedImage two, final int tolerance) {

        if (one.getWidth() == two.getWidth() && one.getHeight() == two.getHeight()) {
            for (int x = 1; x < one.getWidth() - 1; x++) { // start from 1 and stop before last to prevent out-of-bounds
                for (int y = 1; y < one.getHeight() - 1; y++) {

                    final Color colorOne = new Color(one.getRGB(x, y));
                    boolean matched = false;
                    gridCheck:
                    for (int dx = -1; 1 >= dx; dx++) {
                        for (int dy = -1; 1 >= dy; dy++) {

                            final Color colorTwo = new Color(two.getRGB(x + dx, y + dy));

                            if (Math.abs(colorOne.getRed() - colorTwo.getRed()) <= tolerance &&
                                    Math.abs(colorOne.getGreen() - colorTwo.getGreen()) <= tolerance &&
                                    Math.abs(colorOne.getBlue() - colorTwo.getBlue()) <= tolerance) {
                                matched = true;
                                break gridCheck;
                            }
                        }
                    }
                    if (!matched) return false;
                }
            }
        } else return false;
        return true;
    }

    /**
     * Compares two BufferedImage objects to calculate the maximum difference in their pixel RGB values.
     * This method iterates over each pixel in the first image and compares it with the corresponding
     * pixels in the second image within a 3x3 grid.
     *
     * @param one the first BufferedImage to compare
     * @param two the second BufferedImage to compare
     * @return a Colour object representing the maximum differences in the red, green, and blue channels
     * across all pixels in the images
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "QuestionableName", "StandardVariableNames", "ObjectAllocationInLoop"})
    private static Color compareImages(final BufferedImage one, final BufferedImage two) {

        int r = 0;
        int g = 0;
        int b = 0;

        if (one.getWidth() == two.getWidth() && one.getHeight() == two.getHeight()) {
            for (int x = 1; x < one.getWidth() - 1; x++) {
                for (int y = 1; y < one.getHeight() - 1; y++) {

                    final Color colorOne = new Color(one.getRGB(x, y));

                    for (int dx = -1; 1 >= dx; dx++) {
                        for (int dy = -1; 1 >= dy; dy++) {

                            final Color colorTwo = new Color(two.getRGB(x + dx, y + dy));
                            final int dR = Math.abs(colorOne.getRed() - colorTwo.getRed());
                            final int dG = Math.abs(colorOne.getGreen() - colorTwo.getGreen());
                            final int dB = Math.abs(colorOne.getBlue() - colorTwo.getBlue());

                            r = Math.max(r, dR);
                            g = Math.max(g, dG);
                            b = Math.max(b, dB);
                        }
                    }
                }
            }
        }
        logTrace(b(c(DIFFERENCE), a(List.of(
                kv(s(R), r),
                kv(s(G), g),
                kv(s(B), b)), COMMA_SPACE)));
        return new Color(r, g, b);
    }
}


