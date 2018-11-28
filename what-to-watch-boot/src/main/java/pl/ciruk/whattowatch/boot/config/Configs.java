package pl.ciruk.whattowatch.boot.config;

import org.slf4j.Logger;

@SuppressWarnings("PMD.ClassNamingConventions")
final class Configs {
    private Configs() {
        throw new AssertionError();
    }

    static <T> void logConfigurationEntry(Logger logger, String name, T value) {
        logger.info("{}: <{}>", name, value);
    }

    static void logConfigurationEntry(Logger logger, String name, Object first, Object second) {
        logger.info("{}: <{} {}>", name, first, second);
    }
}
