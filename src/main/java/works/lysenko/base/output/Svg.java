package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;
import works.lysenko.base.output.svg.Groups;
import works.lysenko.base.output.svg.Parts;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.chrs.___;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.core.SVG;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.INTO;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.f;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITE;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_SVG_;
import static works.lysenko.util.spec.Numbers.*;
import static works.lysenko.util.spec.Symbols._COLON_;


/**
 * Svg related routines
 */
@SuppressWarnings({"UtilityClass", "UtilityClassCanBeEnum", "FinalClass", "ClassWithTooManyTransitiveDependents",
        "ClassWithTooManyTransitiveDependencies", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassWithTooManyDependencies",
        "ImplicitCallToSuper", "CyclicClassDependency", "UseOfConcreteClass", "ClassUnconnectedToPackage", "FeatureEnvy",
        "ChainedMethodCall", "NestedMethodCall", "AutoBoxing", "AutoUnboxing"})
public final class Svg {

    private static final int LINE_SPACE = 20;
    private static final int COLUMN_WIDTH = 300;
    private static final int ROW_HEIGHT = 15;
    private static final Color COLOR_EMPTY_EVENTS = new Color(0, 128, 0);
    private static final Color COLOR_NON_ZERO_EVENTS = new Color(255, 128, 0);
    private static final Color COLOR_DEFAULT = new Color(164, 164, 255);

    private Svg() {

    }

    @SuppressWarnings({"SameParameterValue", "TypeMayBeWeakened", "LocalCanBeFinal", "UnqualifiedStaticUsage",
            "ForeachStatement"})
    private static void drawGroup(SVGGraphics2D g, int col, int[] dy, TreeMap<String, Result> sorted) {

        SortedMap<String, TreeMap<String, Result>> t = new Groups(TESTS, sorted);
        for (Map.Entry<String, TreeMap<String, Result>> group : t.entrySet())
            drawGroup(g, null, col, dy, group);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "StringToUpperCaseOrToLowerCaseWithoutLocale",
            "ValueOfIncrementOrDecrementUsed",
            "ForeachStatement"})
    private static void drawGroup(final SVGGraphics2D g, final Map<String, Integer> parentDys, final int col, final int[] dy,
                                  final Map.Entry<String, ? extends TreeMap<String, Result>> group) {

        final Map<String, Integer> myDys = new HashMap<>(ZERO);
        final String groupKey = group.getKey().toLowerCase();
        final Integer pDy = (null == parentDys) ? null : parentDys.get(groupKey);

        // Handle title
        dy[col]++;
        final int tDy = dy[col]++;
        g.setColor(Color.GRAY);
        if (isNotNull(pDy))
            drawLine(g, pDy, tDy, col);

        // Handle Head
        g.setColor(Color.BLACK);
        g.drawString(group.getKey(), col * COLUMN_WIDTH + ROW_HEIGHT, tDy * ROW_HEIGHT);
        final Parts parts = Parts.process(group.getValue());
        for (final Map.Entry<String, Result> entry : parts.head.entrySet()) {
            setGraphicsColor(g, entry.getValue());
            final int myDy = dy[col]++;
            final String key = entry.getKey().toLowerCase().split(SPACE)[ZERO];
            myDys.put(key, myDy);
            final String label = s(entry.getKey(), e(s(_COLON_)), entry.getValue().toString());
            g.drawString(label, col * COLUMN_WIDTH + ROW_HEIGHT, myDy * ROW_HEIGHT);
        }

        // Handle Body
        final Groups groups = Groups.process(parts.body);
        for (final Map.Entry<String, TreeMap<String, Result>> subgroup : groups.entrySet())
            drawGroup(g, myDys, col + ONE, dy, subgroup);
    }

    private static void drawLine(final SVGGraphics2D g, final int pDy, final int tDy, final int col) {

        final int startX = col * COLUMN_WIDTH - (ROW_HEIGHT * SIX);
        final int startY = pDy * ROW_HEIGHT - FOUR;
        final int endX = col * COLUMN_WIDTH + (LINE_SPACE / TWO);
        final int endY = tDy * ROW_HEIGHT - FOUR;

        g.drawLine(startX, startY, startX + LINE_SPACE, startY);
        g.drawLine(startX + LINE_SPACE, startY, startX + (LINE_SPACE << TWO), endY);
        g.drawLine(startX + (LINE_SPACE << TWO), endY, endX, endY);
    }

    @SuppressWarnings({"ImplicitNumericConversion", "ChainedMethodCall", "NestedConditionalExpression"})
    private static void setGraphicsColor(final SVGGraphics2D g, final _Result result) {

        if (ZERO == result.getExecutions()) {
            final Fraction totalWeight = (null == result.getConfiguredWeight()) ? null :
                    fr(result.getConfiguredWeight().doubleValue() + result.getDownstreamWeight().doubleValue() + result.getUpstreamWeight().doubleValue());
            g.setColor(null == totalWeight ? Color.GRAY : ZERO < totalWeight.doubleValue() ? COLOR_DEFAULT : Color.GRAY);
        } else {
            g.setColor(result.getEvents().isEmpty() ? COLOR_EMPTY_EVENTS : COLOR_NON_ZERO_EVENTS);
        }
    }

    /**
     * Generates SVG statistics based on the results obtained from getResults().getSortedStrings().
     * The statistics are written to an SVG file specified in the properties.
     */
    @SuppressWarnings({"LawOfDemeter",
            "ThrowInsideCatchBlockWhichIgnoresCaughtException", "ProhibitedExceptionThrown", "ChainedMethodCall"})
    public static void svgStats() {

        final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
        final SVGGraphics2D g = new SVGGraphics2D(SVG.width, SVG.height);

        final int[] dy = new int[HUNDRED];
        Arrays.fill(dy, ONE);

        drawGroup(g, ZERO, dy, sorted);
        try {
            SVGUtils.writeToSVG(new File(f(name(RUN_SVG_))), g.getSVGElement());
        } catch (final IOException e) {
            throw new RuntimeException(b(c(UNABLE), TO, WRITE, ___.SVG, INTO, PNG, FILE));
        }
    }
}
