package pl.ciruk.whattowatch.score;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ScoreMatchers {
    public static Matcher<? super Score> isMeaningful() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Score item) {
                return isMeaningful(item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" is meaningful");
            }

            @Override
            protected void describeMismatchSafely(Score item, Description mismatchDescription) {
                mismatchDescription.appendValue(item).appendText(" is not meaningful");
            }
        };
    }

    public static boolean isMeaningful(Score item) {
        boolean validGrade = item.getGrade() >= 0.1 && item.getGrade() <= 1.0;
        boolean significant = item.isSignificant();

        return validGrade && significant;
    }
}
