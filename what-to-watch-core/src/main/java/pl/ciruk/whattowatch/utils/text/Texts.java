package pl.ciruk.whattowatch.utils.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Texts {
    private static final String[] ARTICLES = {"a ", "an ", "the "};
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private Texts() {
        throw new AssertionError();
    }

    public static boolean matchesAlphanumerically(String first, String second) {
        if (first == null || second == null) {
            return false;
        } else {
            return unify(first).equals(unify(second));
        }
    }

    /**
     * Lower case, without diacritics, a leading article or anything but letters, digits and spaces.
     * "&" counts as "and".
     */
    public static String unify(String text) {
        var folded = withoutDiacritics(text.toLowerCase(Locale.ROOT)).replace("&", "and");
        return Patterns.nonAlphaNumSpace()
                .matcher(stripArticle(folded))
                .replaceAll("");
    }

    // Sites drop them inconsistently: IMDb has "Katyn" for "Katyń"
    public static String withoutDiacritics(String text) {
        // "ł" has no decomposed form
        var decomposed = Normalizer.normalize(text.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }

    private static String stripArticle(String lowerCaseText) {
        for (String article : ARTICLES) {
            if (lowerCaseText.startsWith(article)) {
                return lowerCaseText.substring(article.length());
            }
        }
        return lowerCaseText;
    }
}
