package pl.ciruk.whattowatch.utils.text;

import java.util.Locale;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Texts {
    private static final String[] ARTICLES = {"a ", "an ", "the "};

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

    private static String unify(String text) {
        var lowerCaseText = stripArticle(text.toLowerCase(Locale.getDefault()));
        return Patterns.nonAlphaNumSpace()
                .matcher(lowerCaseText)
                .replaceAll("");
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
