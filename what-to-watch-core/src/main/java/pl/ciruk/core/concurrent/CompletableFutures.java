package pl.ciruk.core.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public class CompletableFutures {
	public static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
		return CompletableFuture.allOf(
				futures.toArray(new CompletableFuture[futures.size()])
		);
	}

	public static <T> CompletableFuture<Stream<T>> getAllOf(List<CompletableFuture<T>> futures) {
		CompletableFuture.allOf(
				futures.toArray(new CompletableFuture[futures.size()])
		);

		return CompletableFuture.supplyAsync(
				() ->
						futures.stream()
								.map(future -> {
									try {
										return future.get();
									} catch (InterruptedException | ExecutionException e) {
										throw new RuntimeException(e);
									}
								}));
	}

	public static <T> BinaryOperator<CompletableFuture<T>> combineUsing(BiFunction<T, T, T> combinator) {
		return (cf1, cf2) -> cf1.thenCombine(cf2, combinator);
	}
}
