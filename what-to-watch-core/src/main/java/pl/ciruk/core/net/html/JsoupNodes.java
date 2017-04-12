package pl.ciruk.core.net.html;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public final class JsoupNodes {
    private JsoupNodes() {

    }

    public static Element emptyTextElement() {
        return new Element(Tag.valueOf("span"), "") {
            @Override
            public String text() {
                return "";
            }
        };
    }
}
