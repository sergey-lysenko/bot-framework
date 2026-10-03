package link.util.spec;

import works.lysenko.util.apis.*;

import static works.lysenko.util.data.strs.Swap.*;

@SuppressWarnings({"MissingJavadoc", "OverloadedVarargsMethod", "Singleton"})
public enum PropEnum implements _PropEnum {

    _DUMMY(Boolean.class, "false");

    private final Class<?> type;
    private final String defaultValue;

    PropEnum(final Class<?> type, final String defaultStringValue) {

        this.type = type;
        defaultValue = defaultStringValue;
    }

    PropEnum(final Class<?> type, final char defaultCharValue) {

        this.type = type;
        defaultValue = s(defaultCharValue);
    }

    PropEnum(final Class<?> type, final char... defaultChars) {

        this.type = type;
        defaultValue = new String(defaultChars);
    }

    @Override
    public String defaultValue() {

        return defaultValue;
    }

    @Override
    public Class<?> type() {

        return type;
    }
}
