package pl.ciruk.whattowatch.utils.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Texts {
    private static final String[] ARTICLES = {"a ", "an ", "the "};
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^\\w]");

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
     * Lower case, without diacritics, a leading article or anything but letters and digits. "&" counts as "and".
     * Spaces go too, as sites split words differently: Metacritic has "The 40 Year Old Virgin" for "The 40 Year-Old Virgin".
     */
    public static String unify(String text) {
        // Filmweb has " The Devil All the Time", where the space would keep the article
        var folded = withoutDiacritics(text.strip().toLowerCase(Locale.ROOT)).replace("&", "and");
        return NOT_ALPHANUMERIC
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
