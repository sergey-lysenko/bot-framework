package works.lysenko.util.func.logs.trace;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.apis.log._Traceable;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.grid.record.gsrc.Resolution;

import static java.util.Objects.isNull;
import static works.lysenko.util.apis.log._Traceable.create;
import static works.lysenko.util.apis.log._Traceable.key;
import static works.lysenko.util.chrs.___.KEY;
import static works.lysenko.util.chrs.___.RAW;
import static works.lysenko.util.chrs.____.BASE;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.C.CENTER;
import static works.lysenko.util.lang.word.D.DRAFT;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.E.EXTRA;
import static works.lysenko.util.lang.word.H.HEIGHT;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.O.OUTPUT;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.SCALE;
import static works.lysenko.util.lang.word.S.SEVERITY;
import static works.lysenko.util.lang.word.S.SILENT;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.lang.word.W.WIDTH;
import static works.lysenko.util.lang.word.W.WITHIN;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._1_;

/**
 * The DataStorage class provides utility methods for creating instances of Traceable objects.
 * These methods take various input parameters, convert them into structured key-value pairs
 * and encapsulate them into Traceable objects. The resulting Traceable objects can be used
 * to represent structured data for different purposes.
 */
@SuppressWarnings({"unchecked", "MissingJavadoc"})
public record Data() {

    public static _Traceable traceable(final _Range<Fraction> expected, final Fraction actual, final double absoluteMargin,
                                       final _RangedMargin allowedMargin, final double allowedAbsoluteMargin,
                                       final boolean withinMargin, final Severity severity) {

        return create(key(EXPECTED, expected.toVerboseString()), key(ACTUAL, actual), key(s(ABSOLUTE, c(MARGIN)),
                absoluteMargin), key(s(ALLOWED, c(MARGIN)), allowedMargin.render()), key(s(ALLOWED, c(ABSOLUTE), c(MARGIN)),
                allowedAbsoluteMargin), key(s(WITHIN, c(MARGIN)), withinMargin), key(SEVERITY, severity));
    }

    public static _Traceable traceable(final int width, final int height, final double horizontal, final double vertical,
                                       final double scale, final float ratio) {

        return create(key(WIDTH, width), key(HEIGHT, height), key(HORIZONTAL, horizontal), key(VERTICAL, vertical),
                key(SCALE, scale), key(RATIO, ratio));
    }

    public static _Traceable traceable(final Location center, final float radius, final float ratio) {

        return create(key(CENTER, center), key(RADIUS, radius), key(RATIO, ratio));
    }

    public static _Traceable traceable(final float aspect, final Resolution resolution) {

        return create(key(ASPECT, aspect), key(RESOLUTION, resolution));
    }

    public static _Traceable traceable(final boolean expected, final boolean actual) {

        return create(key(EXPECTED, expected), key(ACTUAL, actual));
    }

    public static _Traceable traceable(final int w, final int h, final Fraction relativeX, final Fraction relativeY,
                                       final int x1, final int y1, final int result) {

        return create(key(WIDTH, w), key(HEIGHT, h), key(s(RELATIVE, X), relativeX), key(s(RELATIVE, Y), relativeY), key(s(X
                , _1_), x1), key(s(Y, _1_), y1), key(RESULT, result));
    }

    public static _Traceable traceable(final Fraction actual, final _Range<Fraction> expected, final Fraction margin) {

        return create(key(ACTUAL, ts(true, actual)), key(EXPECTED, expected.toVerboseString()), key(MARGIN, ts(true, margin)));
    }

    public static _Traceable traceable(final String output) {

        return create(key(OUTPUT, output));
    }

    public static _Traceable traceable(final _Range<Fraction> range) {

        return create(key(RANGE, range.toVerboseString()));
    }

    public static _Traceable traceable(final Integer key, final _ValuedRangeResult value, final _Quota<?> range,
                                       final Fraction margin) {

        final String rangeS = isNull(range) ? NULL : range.render();
        return create(key(KEY, key), key(VALUE, value.toString()), key(RANGE, rangeS), key(MARGIN, ts(true, margin)));
    }

    public static _Traceable traceable(final boolean silent, final int draft, final int base, final int extra,
                                       final float rate, final int reduction) {

        return create(key(SILENT, silent), key(DRAFT, draft), key(BASE, base), key(EXTRA, extra), key(RATE, rate),
                key(REDUCTION, reduction));
    }

    public static _Traceable traceable(final String rawText, final int x, final int y, final int x1, final int y1) {

        return create(key(RAW, rawText), key(s(X), x), key(s(Y), y), key(s(X, _1_), x1), key(s(Y, _1_), y1));
    }
}