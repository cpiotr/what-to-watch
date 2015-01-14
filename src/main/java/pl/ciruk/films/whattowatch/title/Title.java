package pl.ciruk.films.whattowatch.title;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

@Getter
@ToString
@Builder
public class Title {
	private String originalTitle;
	
	private String title;

	private int year;

}
