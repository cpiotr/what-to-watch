package pl.ciruk.whattowatch.score;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ScoreMatchers {
	public static Matcher<? super Score> isMeaningful() {
		return new TypeSafeMatcher<Score>() {
			@Override
			protected boolean matchesSafely(Score item) {
				boolean validGrade = item.getGrade() >= 0.1 && item.getGrade() <= 1.0;
				boolean validQuantity = item.getQuantity() >= 10;

				return validGrade && validQuantity;
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
}
