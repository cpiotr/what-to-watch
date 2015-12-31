package pl.ciruk.core.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFutures {
	public static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
		return CompletableFuture.allOf(
				futures.toArray(new CompletableFuture[futures.size()])
		);
	}
}
