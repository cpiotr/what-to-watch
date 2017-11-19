package pl.ciruk.core.net.html;

import lombok.experimental.UtilityClass;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

@UtilityClass
public final class JsoupNodes {
    public static Element emptyTextElement() {
        return new Element(Tag.valueOf("span"), "") {
            @Override
            public String text() {
                return "";
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                return super.equals(o);
            }
        };
    }
}
