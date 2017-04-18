package pl.ciruk.whattowatch;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Resources {
    public static String readContentOf(String resourceName) {
        try {
            URL url = Resources.class.getClassLoader().getResource(resourceName);
            URI uri = url.toURI();
            return new String(
                    Files.readAllBytes(Paths.get(uri)),
                    Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private Resources() {
        throw new AssertionError();
    }
}
