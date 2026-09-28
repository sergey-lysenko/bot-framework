package works.lysenko.util.data.type;

import works.lysenko.util.apis.data._Pins;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.strip;
import static works.lysenko.util.chrs.____.BITS;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Collector.selectOneOf;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Symbols.CLS_CUB;
import static works.lysenko.util.spec.Symbols.OPN_CUB;

/**
 * Provides additional functionality to BitSet.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "BooleanMethodNameMustStartWithQuestion",
        "MethodWithMultipleReturnPoints",
        "NestedMethodCall", "AutoBoxing", "CallToSuspiciousStringMethod"})
public class Pins implements _Pins {

    private final BitSet bits;

    /**
     * Provides some additional functionality to BitSet
     */
    @SuppressWarnings("unused")
    public Pins() {

        bits = new BitSet(0);
    }

    /**
     * Constructs a new Pins object with the specified length.
     *
     * @param length the length of the Pins object
     */
    public Pins(final int length) {

        bits = new BitSet(length);
    }

    /**
     * Constructs a new Pins object with the specified BitSet.
     *
     * @param bitSet the BitSet object to be assigned as the internal representation of the Pins object
     */
    private Pins(final BitSet bitSet) {

        bits = bitSet;
    }

    /**
     * Constructs a new Pins object using a _Pins object.
     *
     * @param pinsToCopy the _Pins object to be used as the underlying BitSet
     */
    public Pins(final _Pins pinsToCopy) {

        bits = pinsToCopy.getBitsCopy();
    }

    /**
     * @param cellsA a
     * @param cellsB b
     * @return true if intersects
     */
    @SuppressWarnings("IfStatementWithNegatedCondition")
    public static boolean intersects(final _Pins cellsA, final _Pins cellsB) {

        if (!isAnyNull(cellsA, cellsB)) return cellsA.intersects(cellsB);
        else return false;
    }

    @Override
    public final void and(final _Pins cells) {

        bits.and(cells.getBits());
    }

    @Override
    public final void andNot(final _Pins pins) {

        bits.andNot(pins.getBits());
    }

    @Override
    public final List<String> asStringList() {

        final List<String> list = new ArrayList<>(List.of(strip(bits.toString(),
                s(CLS_CUB, OPN_CUB)).split(s(L1))));
        if (1 == list.size() && list.get(0).equals(EMPTY)) list.remove(0);
        return list;
    }

    @Override
    public final int cardinality() {

        return bits.cardinality();
    }

    @SuppressWarnings("UseOfClone")
    @Override
    public final _Pins copy() {

        return new Pins((BitSet) bits.clone());
    }

    @Override
    public final BitSet getBits() {

        return bits;
    }

    @SuppressWarnings("UseOfClone")
    @Override
    public final BitSet getBitsCopy() {

        return (BitSet) bits.clone();
    }

    @Override
    public final Integer getRandomTrue() {

        final List<String> list = asStringList();
        if (list.isEmpty()) return null;
        return i((selectOneOf(list)));
    }

    @Override
    public final boolean intersects(final _Pins pins) {

        return bits.intersects(pins.getBits());
    }

    @Override
    public final boolean is(final Integer i) {

        return bits.get(i);
    }

    @Override
    public final void or(final _Pins cells) {

        bits.or(cells.getBits());
    }

    @SuppressWarnings("BooleanParameter")
    public final void set(final int from, final int to, final boolean is) {

        bits.set(from, to, is);
    }

    @Override
    public final String toString() {

        return a(BITS, bits.toString());
    }

    @Override
    public final void xor(final _Pins cells) {

        bits.xor(cells.getBits());
    }
}
