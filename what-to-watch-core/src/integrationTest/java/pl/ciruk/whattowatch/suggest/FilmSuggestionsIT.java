package pl.ciruk.whattowatch.suggest;

import com.google.common.collect.Lists;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.core.net.json.JsonConnection;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.IMDBScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FilmSuggestionsIT {
	public static final int NUMBER_OF_TITLES = 50;

	private FilmSuggestions suggestions;

	@Before
	public void setUp() throws Exception {
		HtmlConnection htmlConnection = new HtmlConnection(new OkHttpClient());
		ExecutorService pool = Executors.newFixedThreadPool(NUMBER_OF_TITLES / 4);
		suggestions = new FilmSuggestions(
				provideTitlesFromResource(),
				sampleDescriptionProvider(htmlConnection, pool),
				sampleScoreProviders(htmlConnection, pool),
				pool
		);
	}

	@Test
	public void shouldSuggestAllFilmsFromSampleTitleProvider() throws Exception {
		int numberOfFilms = (int) suggestions.suggestFilms()
				.map(CompletableFutures::get)
				.filter(Objects::nonNull)
				.filter(f -> f.isNotEmpty())
				.count();

		assertThat(numberOfFilms, is(equalTo(NUMBER_OF_TITLES)));
	}

	private FilmwebDescriptions sampleDescriptionProvider(HtmlConnection htmlConnection, ExecutorService pool) {
		return new FilmwebDescriptions(new FilmwebProxy(new JsoupConnection(htmlConnection)), pool);
	}

	private static List<ScoresProvider> sampleScoreProviders(HtmlConnection connection, ExecutorService executorService) {
		JsoupConnection jsoupConnection = new JsoupConnection(connection);
		JsonConnection jsonConnection = new JsonConnection(connection);
		return Lists.newArrayList(
				new FilmwebScores(new FilmwebProxy(jsoupConnection), executorService),
				new MetacriticScores(jsoupConnection, executorService),
				new IMDBScores(jsonConnection, executorService)
		);
	}

	private TitleProvider provideTitlesFromResource() {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("films-with-my-scores.csv");
		List<Title> titles = new BufferedReader(new InputStreamReader(inputStream))
				.lines()
				.limit(NUMBER_OF_TITLES)
				.map(line -> line.split(";"))
				.map(array -> Title.builder().title(array[0]).originalTitle(array[1]).year(Integer.parseInt(array[2])).build())
				.collect(toList());

		return () -> titles.stream();
	}
}