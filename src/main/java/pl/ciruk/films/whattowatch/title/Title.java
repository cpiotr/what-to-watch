package pl.ciruk.films.whattowatch.title;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Title {
	private String originalTitle;
	
	private String title;

	private int year;
	
	public Title(String title, String originalTitle, int year) {
		this.title = title;
		this.originalTitle = originalTitle;
		this.year = year;
	}
}
