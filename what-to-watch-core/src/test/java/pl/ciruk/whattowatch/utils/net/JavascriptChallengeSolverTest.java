package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.Resources;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static org.assertj.core.api.Assertions.assertThat;

class JavascriptChallengeSolverTest {

    private JavascriptChallengeSolver solver;

    @BeforeEach
    void init() {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("js");
        solver = new JavascriptChallengeSolver(js);
    }

    @Test
    void shouldSolveKnownChallenge() {
        HttpUrl requestedUrl = HttpUrl.get("https://ekino-tv.pl/movie/cat/strona[0]+");
        String expectedUrl = "https://ekino-tv.pl/cdn-cgi/l/chk_jschl" +
                "?s=3e76a2ad6a06de108793880fd0e8150aef39fb87-1557257583-1800-AR5vkMdyIRiOa2BL4mZmbG8De5kbjwiewoq3lLn%2F3t6IhLJCcqiTlYbmxPvBETHvoUwHIRrS366c4%2FS4FdhKt0RqBpV5jui3XauoLPHI0baKjx4ZzH2oAFZCq%2B2knxzezjZeVL6n0%2FMViD1D5%2BZx9Yg%3D" +
                "&jschl_vc=55dd9b25c2e7c67baf01738d529c555e" +
                "&pass=1557257587.072-D4Yu2XW7l8" +
                "&jschl_answer=-122.1646258253";
        HttpUrl actualUrl = solver.solve(requestedUrl, Resources.readContentOf("js-challenge.html"));

        assertThat(actualUrl.toString()).isEqualTo(expectedUrl);
    }
}
