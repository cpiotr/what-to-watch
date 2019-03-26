package pl.ciruk.whattowatch.core.description.filmweb;

import java.util.regex.Pattern;

class Commons {
    static java.util.regex.Pattern NON_DIGIT = Pattern.compile("[^0-9]");

    private Commons() {
        throw new AssertionError();
    }
}
