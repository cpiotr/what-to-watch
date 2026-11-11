package pl.ciruk.whattowatch.utils.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextsTest {

    @Test
    void shouldIgnoreCaseArticleAndPunctuation() {
        assertThat(Texts.matchesAlphanumerically("The Good, the Bad and the Ugly", "good the bad and the ugly")).isTrue();
    }

    @Test
    void shouldIgnoreDiacritics() {
        assertThat(Texts.matchesAlphanumerically("Katyń", "Katyn")).isTrue();
        assertThat(Texts.matchesAlphanumerically("Żółw Łódź", "zolw lodz")).isTrue();
    }

    @Test
    void shouldTreatAmpersandAsAnd() {
        assertThat(Texts.matchesAlphanumerically("Dumb & Dumber", "Dumb and Dumber")).isTrue();
    }

    @Test
    void shouldNotMatchDifferentTitles() {
        assertThat(Texts.matchesAlphanumerically("Frozen", "The Frozen Ground")).isFalse();
        assertThat(Texts.matchesAlphanumerically("Frozen", null)).isFalse();
    }

    @Test
    void shouldKeepCaseWhenRemovingDiacritics() {
        assertThat(Texts.withoutDiacritics("Łódź Café")).isEqualTo("Lodz Cafe");
    }
}
