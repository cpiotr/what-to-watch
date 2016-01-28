package pl.ciruk.core.stream;

import java.util.function.Predicate;

public class Predicates {
	private Predicates() {
		// utility class
	}

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return predicate.negate();
	}
}
