package pl.ciruk.films.whattowatch.description;

import java.util.Optional;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;
import pl.ciruk.films.whattowatch.title.Title;

@ToString
@Getter
@Builder
public class Description {
	
	private Title title;

	private Title foundFor;

	private String poster;

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
