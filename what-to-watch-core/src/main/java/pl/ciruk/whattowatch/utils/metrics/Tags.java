package pl.ciruk.whattowatch.utils.metrics;

import io.micrometer.core.instrument.Tag;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Tags {
    private Tags() {
        throw new AssertionError();
    }

    public static Tag getProviderTag(String providerName) {
        return Tag.of("provider", providerName);
    }
}
