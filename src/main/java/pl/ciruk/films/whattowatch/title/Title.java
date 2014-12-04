package pl.ciruk.films.whattowatch.title;

public class Title {
	private String originalTitle;
	
	private String title;

	private int year;
	
	public Title(String title, String originalTitle, int year) {
		this.title = title;
		this.originalTitle = originalTitle;
		this.year = year;
	}

	// TODO: Use Lombok instead 
	public String getTitle() {
		return title;
	}

	public String getOriginalTitle() {
		// TODO Auto-generated method stub
		return originalTitle;
	}

	public int getYear() {
		return year;
	}
}
