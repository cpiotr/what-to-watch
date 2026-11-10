package pl.ciruk.whattowatch.utils.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class CompletableFutures {
    private CompletableFutures() {
        throw new AssertionError();
    }

    public static <T> Stream<T> getAllOf(List<CompletableFuture<T>> futures) {
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
}
