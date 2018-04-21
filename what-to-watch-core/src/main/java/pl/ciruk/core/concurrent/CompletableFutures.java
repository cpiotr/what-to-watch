package pl.ciruk.core.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class CompletableFutures {
    public static <T> CompletableFuture<Stream<T>> allOf(List<CompletableFuture<T>> futures) {
        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        return CompletableFuture.supplyAsync(
                () -> futures.stream().map(CompletableFuture::join)
        );
    }

    public static <T> CompletableFuture<Stream<T>> allOf(Stream<CompletableFuture<T>> futures) {
        return allOf(futures.collect(toList()));
    }

    public static <T> Stream<T> getAllOf(List<CompletableFuture<T>> futures) {
        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        return futures.stream().map(CompletableFuture::join);
    }

    public static <T> Stream<T> getAllOf(Stream<CompletableFuture<T>> futures) {
        return getAllOf(futures.collect(toList()));
    }

    public static <T> BinaryOperator<CompletableFuture<T>> combineUsing(BiFunction<T, T, T> combinator) {
        return (cf1, cf2) -> cf1.thenCombine(cf2, combinator);
    }

    public static <T> BinaryOperator<CompletableFuture<T>> combineUsing(
            BiFunction<T, T, T> combinator,
            ExecutorService executorService) {
        return (first, second) -> first.thenCombineAsync(second, combinator, executorService);
    }

    public static <T> CompletableFuture<T> of(Future<T> future) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }
}
