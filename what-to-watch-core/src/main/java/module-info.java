module what.to.watch.core {
    exports pl.ciruk.whattowatch.core.suggest;
    exports pl.ciruk.whattowatch.utils.concurrent;
    exports pl.ciruk.whattowatch.core.title;
    exports pl.ciruk.whattowatch.utils.net;
    exports pl.ciruk.whattowatch.core.title.ekino;
    exports pl.ciruk.whattowatch.core.source;
    exports pl.ciruk.whattowatch.core.description.filmweb;
    exports pl.ciruk.whattowatch.core.description;
    exports pl.ciruk.whattowatch.core.score;
    exports pl.ciruk.whattowatch.core.score.imdb;
    exports pl.ciruk.whattowatch.core.score.filmweb;
    exports pl.ciruk.whattowatch.core.score.metacritic;
    exports pl.ciruk.whattowatch.utils.cache;
    exports pl.ciruk.whattowatch.utils.stream;
    requires org.jsoup;
    requires org.slf4j;
    requires micrometer.core;
    requires okhttp3;
    requires com.fasterxml.jackson.databind;
    requires java.scripting;
    requires com.github.benmanes.caffeine;
}
