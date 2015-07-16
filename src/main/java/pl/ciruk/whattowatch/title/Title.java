package pl.ciruk.whattowatch.title;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class Title {
	private String originalTitle;
	
	private String title;

	private int year;

}
