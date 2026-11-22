package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.source.FilmwebTitle;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pl.ciruk.whattowatch.core.description.DescriptionAssert.assertThat;

class FilmwebDescriptionProviderTest {
    private static final String CASINO_ROYALE = "Casino Royale";
    private static final String SHAWSHANK = "Skazani na Shawshank";
    private static final String THE_BEAR = "The Bear";
    private static final String THE_DIPLOMAT = "The Diplomat";
    private static final String DYPLOMATKA = "Dyplomatka";
    private static final String AIR = "Air";
    private static final String DAREDEVIL = "Daredevil";
    private static final FilmwebTitle CASINO_ROYALE_2006 = new FilmwebTitle(107_642, CASINO_ROYALE, CASINO_ROYALE, 2006, TitleKind.FILM);

    private final FilmwebProxy filmwebProxy = mock(FilmwebProxy.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private FilmwebDescriptionProvider provider;

    @BeforeEach
    void setUp() {
        when(filmwebProxy.searchBy(anyString(), anyInt(), any())).thenAnswer(ignored -> Stream.empty());
        when(filmwebProxy.searchFor(anyString(), any())).thenCallRealMethod();
        provider = new FilmwebDescriptionProvider(filmwebProxy, executorService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldFindFilmRankedBelowSimilarlyNamedOnes() {
        var similarFilms = Stream.of(
                new FilmwebTitle(424_819, "The Real Casino Royale", "The Real Casino Royale", 2006, TitleKind.FILM),
                new FilmwebTitle(413_796, "Näin tehtiin Casino Royale", "", 2006, TitleKind.FILM),
                new FilmwebTitle(624_191, "Casino Royale with Cheese", "", 2007, TitleKind.FILM));
        when(filmwebProxy.searchBy(CASINO_ROYALE, 2006, TitleKind.FILM))
                .thenAnswer(ignored -> Stream.concat(similarFilms, Stream.of(CASINO_ROYALE_2006)));
        when(filmwebProxy.getDetailsPageFor(CASINO_ROYALE_2006))
                .thenReturn(Optional.of(filmPage(CASINO_ROYALE, CASINO_ROYALE, 2006)));

        var description = provider.findDescriptionBy(Title.builder().originalTitle(CASINO_ROYALE).year(2006).build());

        assertThat(description).hasTitle(CASINO_ROYALE);
        verify(filmwebProxy).getDetailsPageFor(CASINO_ROYALE_2006);
        verifyNoMoreInteractions(ignoreStubs(filmwebProxy));
    }

    @Test
    void shouldFindFilmByLocalTitle() {
        var film = new FilmwebTitle(1_048, SHAWSHANK, "The Shawshank Redemption", 1994, TitleKind.FILM);
        when(filmwebProxy.searchBy(SHAWSHANK, 1994, TitleKind.FILM)).thenAnswer(ignored -> Stream.of(film));
        when(filmwebProxy.getDetailsPageFor(film))
                .thenReturn(Optional.of(filmPage(SHAWSHANK, "The Shawshank Redemption", 1994)));

        var description = provider.findDescriptionBy(Title.builder()
                .title(SHAWSHANK)
                .originalTitle("Shawshank")
                .year(1994)
                .build());

        assertThat(description).hasTitle("The Shawshank Redemption");
    }

    @Test
    void shouldTellSeriesWithSameOriginalTitleApartByLocalTitle() {
        var british = new FilmwebTitle(10_060_925, "Dyplomatki", THE_DIPLOMAT, 2023, TitleKind.SERIES);
        var netflix = new FilmwebTitle(10_033_009, DYPLOMATKA, THE_DIPLOMAT, 2023, TitleKind.SERIES);
        when(filmwebProxy.searchBy(THE_DIPLOMAT, 2023, TitleKind.SERIES)).thenAnswer(ignored -> Stream.of(british, netflix));
        when(filmwebProxy.getDetailsPageFor(british)).thenReturn(Optional.of(filmPage("Dyplomatki", THE_DIPLOMAT, 2023)));
        when(filmwebProxy.getDetailsPageFor(netflix)).thenReturn(Optional.of(filmPage(DYPLOMATKA, THE_DIPLOMAT, 2023)));

        var found = provider.findDescriptionBy(Title.builder()
                        .title(DYPLOMATKA)
                        .originalTitle(THE_DIPLOMAT)
                        .year(2023)
                        .kind(TitleKind.SERIES)
                        .build())
                .orElseThrow()
                .getTitle();

        assertThat(found.localTitle()).isEqualTo(DYPLOMATKA);
    }

    // The score providers search each site for the kind of the title found here
    @Test
    void shouldFindSeriesAsSeries() {
        var series = new FilmwebTitle(10_014_243, THE_BEAR, THE_BEAR, 2022, TitleKind.SERIES);
        when(filmwebProxy.searchBy(THE_BEAR, 2022, TitleKind.SERIES)).thenAnswer(ignored -> Stream.of(series));
        when(filmwebProxy.getDetailsPageFor(series)).thenReturn(Optional.of(page(THE_BEAR, "", "2022 - 2026")));

        var found = provider.findDescriptionBy(Title.builder()
                        .originalTitle(THE_BEAR)
                        .year(2022)
                        .kind(TitleKind.SERIES)
                        .build())
                .orElseThrow()
                .getTitle();

        assertThat(found.kind()).isEqualTo(TitleKind.SERIES);
        assertThat(found.year()).isEqualTo(2022);
    }

    @Test
    void shouldTellFilmsWithSameTitleAndYearApartByVotes() {
        var unknown = new FilmwebTitle(10_082_942, AIR, AIR, 2023, TitleKind.FILM);
        var known = new FilmwebTitle(10_030_286, AIR, AIR, 2023, TitleKind.FILM);
        when(filmwebProxy.searchBy(AIR, 2023, TitleKind.FILM)).thenAnswer(ignored -> Stream.of(unknown, known));
        when(filmwebProxy.getDetailsPageFor(unknown)).thenReturn(Optional.of(ratedPage(AIR, 2023, "", "")));
        when(filmwebProxy.getDetailsPageFor(known))
                .thenReturn(Optional.of(ratedPage(AIR, 2023, "30 409", "Sonny Vaccaro signs Michael Jordan.")));

        var found = provider.findDescriptionBy(Title.builder().title(AIR).originalTitle(AIR).year(2023).build())
                .orElseThrow();

        assertThat(found.getPlot()).isEqualTo("Sonny Vaccaro signs Michael Jordan.");
    }

    // Filmweb finds it by the name streaming services use, but calls it "Daredevil"
    @Test
    void shouldDescribeSeriesFoundByAnotherName() {
        var series = new FilmwebTitle(702_252, DAREDEVIL, DAREDEVIL, 2015, TitleKind.SERIES);
        when(filmwebProxy.searchBy("Marvel's Daredevil", 2015, TitleKind.SERIES)).thenAnswer(ignored -> Stream.of(series));
        when(filmwebProxy.getDetailsPageFor(series)).thenReturn(Optional.of(page(DAREDEVIL, "", "2015 - 2018")));

        var found = provider.findDescriptionBy(Title.builder()
                        .originalTitle("Marvel's Daredevil")
                        .year(2015)
                        .kind(TitleKind.SERIES)
                        .build())
                .orElseThrow()
                .getTitle();

        assertThat(found.localTitle()).isEqualTo(DAREDEVIL);
    }

    @Test
    void shouldFindNothingForUnknownFilm() {
        var description = provider.findDescriptionBy(Title.builder().originalTitle("Unknown").year(2020).build());

        assertThat(description).isEmpty();
    }

    // A film nobody rated has no vote count
    private static Element ratedPage(String title, int year, String votes, String plot) {
        return Jsoup.parse("<div class=\"filmCoverSection__titleDetails\">"
                + "<h1 class=\"filmCoverSection__title\">" + title + "</h1>"
                + "<div class=\"filmCoverSection__originalTitle\">" + title
                + " <span class=\"filmCoverSection__year\">" + year + "</span></div>"
                + "</div>"
                + (votes.isEmpty() ? "" : "<span itemprop=\"ratingCount\">" + votes + "</span>")
                + "<div class=\"filmPosterSection__container\"><div class=\"filmPosterSection__plot\">"
                + "<span itemprop=\"description\">" + plot + "</span></div></div>");
    }

    private static Element filmPage(String localTitle, String originalTitle, int year) {
        return page(localTitle, originalTitle, String.valueOf(year));
    }

    private static Element page(String localTitle, String originalTitle, String years) {
        return Jsoup.parse("<div class=\"filmCoverSection__titleDetails\">"
                + "<h1 class=\"filmCoverSection__title\">" + localTitle + "</h1>"
                + "<div class=\"filmCoverSection__originalTitle\">" + originalTitle
                + " <span class=\"filmCoverSection__year\">" + years + "</span></div>"
                + "</div>");
    }
}
