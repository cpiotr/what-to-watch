package pl.ciruk.whattowatch.boot.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

@RestController
@RequestMapping("/resources/titles")
public class Titles {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TitleProvider titleProvider;
    private final ExecutorService executorService;

    public Titles(TitleProvider titleProvider, ExecutorService executorService) {
        this.titleProvider = titleProvider;
        this.executorService = executorService;
    }

    @GetMapping("/{pageNumber}")
    public CompletableFuture<List<Title>> findAll(
            @PathVariable int pageNumber,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        LOGGER.info("Kind: {}, page number: {}", kind, pageNumber);

        return CompletableFuture.supplyAsync(() -> titleProvider.streamOfTitles(kind, pageNumber).toList(), executorService)
                .orTimeout(20, SECONDS);
    }
}
