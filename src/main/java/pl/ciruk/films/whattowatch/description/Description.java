package pl.ciruk.films.whattowatch.description;

import java.util.Optional;

import com.google.common.base.Strings;

import lombok.ToString;
import pl.ciruk.films.whattowatch.title.Title;

@ToString
public class Description {
	
	private Title filmTitle;

	public Description(Title t) {
		this.filmTitle = t;
	}

	public String getTitle() {
		return Optional.ofNullable(filmTitle.getOriginalTitle())
				.filter(t -> !Strings.isNullOrEmpty(t))
				.orElse(filmTitle.getTitle());
	}
	
	public int getYear() {
		return filmTitle.getYear();
	}
}
