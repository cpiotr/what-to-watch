package pl.ciruk.whattowatch.utils.text;

import java.util.regex.Pattern;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Patterns {
    private static final java.util.regex.Pattern NON_DIGIT = Pattern.compile("[^0-9]");
    private static final java.util.regex.Pattern NON_ALPHA_NUM_SPACE = Pattern.compile("[^\\w ]");

    private Patterns() {
        throw new AssertionError();
    }

    public static Pattern nonDigit() {
        return NON_DIGIT;
    }

    public static Pattern nonAlphaNumSpace() {
        return NON_ALPHA_NUM_SPACE;
    }
}
