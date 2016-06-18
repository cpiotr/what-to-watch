package pl.ciruk.core.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class CompletableFutures {
	public static <T> CompletableFuture<Stream<T>> allOf(List<CompletableFuture<T>> futures) {
		CompletableFuture.allOf(
				futures.toArray(new CompletableFuture[futures.size()])
		);

		return CompletableFuture.supplyAsync(
				() -> futures.stream().map(CompletableFutures::get)
		);
	}

	public static <T> CompletableFuture<Stream<T>> allOf(Stream<CompletableFuture<T>> futures) {
		return allOf(futures.collect(toList()));
	}

	public static <T> BinaryOperator<CompletableFuture<T>> combineUsing(BiFunction<T, T, T> combinator) {
		return (cf1, cf2) -> cf1.thenCombineAsync(cf2, combinator);
	}

	public static <T> BinaryOperator<CompletableFuture<T>> combineUsing(BiFunction<T, T, T> combinator, ExecutorService executorService) {
		return (cf1, cf2) -> cf1.thenCombineAsync(cf2, combinator, executorService);
	}

	public static <T> T get(CompletableFuture<T> future) {
		try {
			return future.get(1, TimeUnit.MINUTES);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			future.completeExceptionally(e);
			return null;
		}
	}
}
