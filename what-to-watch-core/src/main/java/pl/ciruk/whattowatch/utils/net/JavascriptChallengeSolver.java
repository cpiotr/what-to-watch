package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class JavascriptChallengeSolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BEGINNING_OF_CHALLENGE = "var s,t,o,p,b,r,e,a,k,i,n,g,f";
    private static final String END_OF_CHALLENGE = ".toFixed(10);";
    private static final Pattern HIDDEN_DIV_ID_PATTERN = Pattern.compile("k = '([^']+)';");

    private final ScriptEngine engine;

    public JavascriptChallengeSolver(ScriptEngine engine) {
        this.engine = engine;
    }

    public HttpUrl solve(HttpUrl requestedUrl, String html) {
        Document document = Jsoup.parse(html);

        Element form = document.getElementById("challenge-form");
        String uri = form.attr("action");
        var requestBuilder = Optional.ofNullable(requestedUrl.newBuilder(uri))
                .orElseThrow(() -> new IllegalStateException("Cannot resolve URI: " + uri));
        form.select("input")
                .stream()
                .filter(input -> input.hasAttr("value"))
                .forEach(input -> requestBuilder.addQueryParameter(input.attr("name"), input.attr("value")));
        String value = evaluateScript(document, requestedUrl);
        LOGGER.debug("Solved challenge: {}", value);
        requestBuilder.addQueryParameter("jschl_answer", value);
        return requestBuilder.build();
    }

    private String evaluateScript(Document document, HttpUrl requestedUrl) {
        String scriptText = document.getElementsByTag("script")
                .stream()
                .map(this::extractScriptText)
                .filter(script -> script.contains(BEGINNING_OF_CHALLENGE))
                .map(script -> script.substring(script.indexOf(BEGINNING_OF_CHALLENGE)))
                .map(script -> script.substring(0, script.indexOf(END_OF_CHALLENGE) + END_OF_CHALLENGE.length()))
                .findFirst()
                .orElse("");

        String idOfDivContainingEmbeddedScript = extractIdOfHiddenDivWithMoreScript(scriptText);
        String scriptEmbeddedInDiv = document.getElementById(idOfDivContainingEmbeddedScript)
                .text();
        String missingFunctions = augmentMissingFunctions(
                List.of(
                        stubFunction("createElement").returning(createTagWithNestedLink(requestedUrl)),
                        stubFunction("getElementById").returning(createTag(scriptEmbeddedInDiv))
                )
        );

        try {
            return String.valueOf(engine.eval(missingFunctions + scriptText));
        } catch (ScriptException e) {
            LOGGER.warn("Unable to solved JS challenge: ```{}```", scriptText, e);
            return "";
        }
    }

    private String augmentMissingFunctions(List<String> missingFunctions) {
        var italicsFunction = "String.prototype.italics = function() {return \"<i>\" + this + \"</i>\";};";
        return Stream.concat(
                Stream.of(italicsFunction, "document = {};"),
                missingFunctions.stream()
        ).collect(joining("\n"));
    }

    private String extractScriptText(Element element) {
        return element.childNodes()
                .stream()
                .map(Object::toString)
                .collect(joining());
    }

    private String extractIdOfHiddenDivWithMoreScript(String script) {
        Matcher matcher = HIDDEN_DIV_ID_PATTERN.matcher(script);
        return matcher.find()
                ? matcher.group(1)
                : "";
    }

    private String createTag(String innerHtml) {
        return String.format("{innerHTML: '%s'};", innerHtml);
    }

    private String createTagWithNestedLink(HttpUrl url) {
        return String.format("{innerHTML: '', firstChild: {href: '%s'}};", url.toString());
    }

    private JavascriptFunctionMock stubFunction(String name) {
        return functionReturnValue -> String.format("document.%s = function(e){ %s };%n", name, functionReturnValue);
    }

    interface JavascriptFunctionMock extends Function<String, String> {
        default String returning(String returnValue) {
            return apply(String.format("return %s;", returnValue));
        }
    }
}
