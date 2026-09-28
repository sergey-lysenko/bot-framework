package works.lysenko.util.data.records;

import java.util.List;
import java.util.Map;

/**
 * Represents metadata for properties with a set of default values and a list
 * of keys associated with valid class references.
 *
 * @param defaults             A1 map containing default key-value property pairs.
 * @param keysWithValidClasses A1 list containing keys associated with valid class references.
 */
public record PropertiesMeta(Map<String, String> defaults, List<? super String> keysWithValidClasses) {

}
