package pl.ciruk.whattowatch.description;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import pl.ciruk.whattowatch.title.Title;

public class DescriptionMatchers {
	public static Matcher<Description> ofTitle(String title) {
		return new TypeSafeMatcher<Description>() {
			@Override
			protected boolean matchesSafely(Description item) {
				Title itemTitle = item.getTitle();
				return itemTitle.getOriginalTitle().equalsIgnoreCase(title)
						|| itemTitle.getTitle().equalsIgnoreCase(title);
			}

			@Override
			public void describeTo(org.hamcrest.Description description) {
				description.appendText("of title ")
						.appendValue(title);
			}
		};
	}
}
