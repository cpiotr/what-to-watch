package pl.ciruk.whattowatch.boundary;

import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;
import pl.ciruk.whattowatch.title.zalukaj.ZalukajTitles;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@Path("/titles")
public class Titles {
	TitleProvider titles;

	@Inject
	public Titles(ZalukajTitles titles) {
		this.titles = titles;
	}

	@GET
	@Produces("application/json")
	public Response findAll() {
		List<Title> titleList = titles.streamOfTitles()
				.collect(toList());
		return Response.ok().entity(titleList).build();
	}
}
