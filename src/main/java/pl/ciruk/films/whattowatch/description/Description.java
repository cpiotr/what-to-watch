package pl.ciruk.films.whattowatch.description;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import pl.ciruk.films.whattowatch.title.Title;

import java.util.List;
import java.util.Optional;

@ToString
@Getter
@Builder
public class Description {
	
	private Title title;

	private Title foundFor;

	private String poster;

	private String plot;

	private List<String> genres;

	public String titleAsText() {
		return Optional.ofNullable(title.getOriginalTitle())
				.filter(t -> !Strings.isNullOrEmpty(t))
				.orElse(title.getTitle());
	}
	
	public int getYear() {
		return title.getYear();
	}

	public void foundFor(Title title) {
		foundFor = title;
	}
}
