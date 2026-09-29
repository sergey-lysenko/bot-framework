package works.lysenko.util.spec;

import static works.lysenko.util.func.type.Chars.booleansToChar;

/**
 * In 1995, Unicode and Java believed 16 bits (65,536 code points) would suffice for all human languages forever.
 * When that turned out to be an understatement, UTF-16 surrogate pairs were born—stitching together two (or more)
 * 16-bit chars into a String because a single char was no longer enough.
 * <p>
 * This class houses those multi-unit surrogate constructs that outgrew the Basic Multilingual Plane.
 */
@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "unused", "WeakerAccess"})
public record Surrogates() {

    public static final String HOUSE = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(true, false, false, false, false, true, true, true, true, true, true, true, true, false, true, true)}); // '🏡'
    public static final String GRADUATION_CAP = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(true, true, false, false, true, false, false, true, true, true, true, true, true, false, true, true)}); // '🎓'
    public static final String BRIEFCASE = new String(new char[]{booleansToChar(true, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, false, true, true, true, true, false, true, false, false, true, true, true, false, true, true)}); // '💼'
    public static final String STETHOSCOPE = new String(new char[]{booleansToChar(false, true, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, true, false, true, true, true, true, false, false, true, true, true, true, false, true, true)}); // '🩺'
    public static final String AUTOMOBILE = new String(new char[]{booleansToChar(true, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(true, true, true, false, true, false, false, true, false, true, true, true, true, false, true, true)}); // '🚗'
    public static final String SCISSORS = new String(new char[]{booleansToChar(false, true, false, false, false, false, false, false, true, true, true, false, false, true), booleansToChar(true, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true)}); // '✂️'
    public static final String BACKPACK = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, true, false, false, true, false, false, true, true, true, true, true, true, false, true, true)}); // '🎒'
    public static final String PLATE = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(true, false, true, true, true, true, true, false, true, true, true, true, true, false, true, true), booleansToChar(true, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true)}); // '🍽️'
    public static final String WEIGHT_LIFTER = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(true, true, false, true, false, false, true, true, true, true, true, true, true, false, true, true), booleansToChar(true, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true)}); // '🏋️'
    public static final String ARTIST_PALETTE = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, false, false, true, false, true, false, true, true, true, true, true, true, false, true, true)}); // '🎨'
    public static final String MICROPHONE = new String(new char[]{booleansToChar(false, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, false, true, false, false, true, false, true, true, true, true, true, true, false, true, true)}); // '🎤'
    public static final String DELIVERY_TRUCK = new String(new char[]{booleansToChar(true, false, true, true, true, true, false, false, false, false, false, true, true, false, true, true), booleansToChar(false, true, false, true, true, false, false, true, false, true, true, true, true, false, true, true)}); // '🚚'
}
