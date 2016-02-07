package pl.ciruk.whattowatch.title;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Getter
@Builder
public class Title {
	private String originalTitle;
	
	private String title;

	private int year;

	private String url;

	public String asText() {
		return Optional.ofNullable(getOriginalTitle())
				.filter(t -> !Strings.isNullOrEmpty(t))
				.orElse(getTitle());
	}

	@Override
	public String toString() {
		return String.format("%s (%d)", asText(), year);
	}
}
