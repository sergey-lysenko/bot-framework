package works.lysenko.util.grid.record.meta;

import works.lysenko.util.data.records.Noun;

/**
 * Represents a subject consisting of a noun and a method.
 * <p>
 * This record combines a {@link Noun} object, which holds singular
 * and plural representations of a noun, with a {@link Method} enum
 * value that defines the subject's method type.
 * <p>
 * It is commonly used in contexts where a subject needs to be paired
 * with a method that determines its operational behavior.
 *
 * @param noun   The noun associated with the subject.
 * @param method The method defining the type of behavior for the subject.
 */
public record Subject(Noun noun, Method method) {
}
