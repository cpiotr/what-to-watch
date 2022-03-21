package pl.ciruk.whattowatch.utils.cache;

import java.util.Optional;

public interface CacheProvider<T> {

    void put(String key, T value);

    Optional<T> get(String key);

    static <T> CacheProvider<T> empty() {
        return new CacheProvider<>() {
            @Override
            public void put(String key, T value) {

            }

            @Override
            public Optional<T> get(String key) {
                return Optional.empty();
            }

            @Override
            public long removeAll(String keyExpression) {
                return 0;
            }
        };
    }

    long removeAll(String keyExpression);
}
