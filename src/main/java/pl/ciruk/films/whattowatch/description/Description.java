package pl.ciruk.films.whattowatch.description;

import pl.ciruk.films.whattowatch.title.Title;

public class Description {
	
	private Title filmTitle;

	public Description(Title t) {
		this.filmTitle = t;
	}

	public Object getTitle() {
		// TODO Auto-generated method stub
		return filmTitle.title();
	}
	
	
}
