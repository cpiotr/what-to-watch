package pl.ciruk.films.whattowatch.web.mvc;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.ciruk.films.whattowatch.Film;
import pl.ciruk.films.whattowatch.WhatToWatch;

import java.util.List;

@Controller
@org.springframework.boot.autoconfigure.EnableAutoConfiguration
public class FIlmsController {
    @Autowired
    WhatToWatch w2w;

    @RequestMapping("/films")
    public String suggestFilms(@RequestParam(value="numberOfFilms", required=false, defaultValue="20") Integer numberOfFilms, Model model) {
        model.addAttribute("numberOfFilms", numberOfFilms);
        List<Film> films = w2w.get(numberOfFilms);
        model.addAttribute("films", Lists.partition(films, 3));
        return "films";
    }
}
