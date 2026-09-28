package works.lysenko.util.apis.data;

import java.util.BitSet;
import java.util.List;

/**
 * Encapsulated BitSet
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "BooleanMethodNameMustStartWithQuestion"})
public interface _Pins {

    /**
     * @param cells to perform And operation
     */
    void and(_Pins cells);

    /**
     * @param pins to perform And Not operation
     */
    void andNot(_Pins pins);

    /**
     * @return List of text values of indexes
     */
    List<String> asStringList();

    /**
     * @return cardinality of Pins
     */
    int cardinality();

    /**
     * @return copy of object
     */
    _Pins copy();

    /**
     * @return underlying BitSet
     */
    BitSet getBits();

    /**
     * Returns a copy of the underlying BitSet.
     *
     * @return a copy of the underlying BitSet
     */
    BitSet getBitsCopy();

    /**
     * @return index of random pin set to true of null if there aren't one
     */
    Integer getRandomTrue();

    /**
     * @param pins to perform And Not operation
     * @return true if intersects
     */
    boolean intersects(_Pins pins);

    /**
     * @param i index of pin to check
     * @return its state
     */
    boolean is(Integer i);

    /**
     * @param cells to perform Or operation
     */
    void or(_Pins cells);

    /**
     * @param from index of first changed pin
     * @param to   index of first unchanged pin
     * @param is   to set
     */
    void set(int from, int to, boolean is);

    /**
     * @return text representation
     */
    String toString();

    /**
     * @param cells to perform Xor operation
     */
    void xor(_Pins cells);
}
