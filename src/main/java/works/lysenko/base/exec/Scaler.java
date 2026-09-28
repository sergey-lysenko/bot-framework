package works.lysenko.base.exec;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.execution._Scaler;
import works.lysenko.util.prop.core.Native;

import java.awt.image.BufferedImage;

import static java.util.Objects.isNull;
import static works.lysenko.Base.log;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.UI;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.U.UNABLE_TO_DEFINE___;
import static works.lysenko.util.lang.word.C.COMPENSATION;
import static works.lysenko.util.lang.word.C.CURRENT;
import static works.lysenko.util.lang.word.N.NATIVE;
import static works.lysenko.util.lang.word.R.RESOLUTION;
import static works.lysenko.util.lang.word.S.SCALE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * The Scaler class represents a utility for scaling images based on the current screen resolution.
 * It initialises by capturing a screenshot, getting the screen dimensions, calculating the current scaling fraction, native
 * scaling fraction,
 * and the compensation ratio. The class logs information about the scaling process.
 */
@SuppressWarnings("WeakerAccess")
public class Scaler implements _Scaler {

    private final BufferedImage image;
    private final Fraction nativeScale;
    private final Fraction compensation;
    private final Fraction current;

    /**
     * The Scaler class represents a utility for scaling images based on the current screen resolution.
     * It initialises by capturing a screenshot, getting the screen dimensions, calculating the current scaling fraction,
     * native scaling fraction,
     * and the compensation ratio. The class logs information about the scaling process.
     */
    @SuppressWarnings("WeakerAccess")
    public Scaler() {

        image = makeScreenshot();
        // Handles screenshot failure; logs scaling information
        if (isNull(image)) {
            nativeScale = null;
            compensation = null;
            current = null;
            fail(UNABLE_TO_DEFINE___);
        } else {
            final int x = image.getWidth();
            final int y = image.getHeight();
            current = new Fraction(x, y);
            nativeScale = new Fraction(Native.x, Native.y);
            compensation = nativeScale.divide(current);
            String message = b(c(CURRENT), u(UI), RESOLUTION, IS, s(yb(x), X, yb(image.getHeight()), _COMMA_));

            if (ZERO == current.compareTo(nativeScale))
                message = b(message, SCALE, s(yb(ts(current))), IS, NATIVE);
            // Logs scale and compensation relative to native
            else message = b(message,
                    SCALE, IS, s(yb(ts(current)), _COMMA_),
                    NATIVE, IS, s(yb(ts(nativeScale)), _COMMA_),
                    COMPENSATION, IS, s(yb(ts(compensation))));
            log(message);
        }
    }

    @Override
    public final Fraction getCompensation() {

        return compensation;
    }

    @Override
    public final Fraction getCurrent() {

        return current;
    }

    public final BufferedImage getImage() {

        return image;
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public final Fraction getNative() {

        return nativeScale;
    }

    @Override
    public final boolean isOk() {

        return isNotNull(current);
    }
}
