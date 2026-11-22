package pl.ciruk.whattowatch.utils.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextsTest {

    @Test
    void shouldIgnoreCaseArticleAndPunctuation() {
        assertThat(Texts.matchesAlphanumerically("The Good, the Bad and the Ugly", "good the bad and the ugly")).isTrue();
    }

    @Test
    void shouldIgnoreSurroundingWhitespace() {
        assertThat(Texts.matchesAlphanumerically(" The Devil All the Time", "The Devil All the Time ")).isTrue();
    }

    @Test
    void shouldIgnoreDiacritics() {
        assertThat(Texts.matchesAlphanumerically("Katyń", "Katyn")).isTrue();
        assertThat(Texts.matchesAlphanumerically("Żółw Łódź", "zolw lodz")).isTrue();
    }

    @Test
    void shouldIgnoreLettersWithoutDecomposedForm() {
        assertThat(Texts.matchesAlphanumerically("Yahşi Batı", "Yahsi Bati")).isTrue();
        assertThat(Texts.matchesAlphanumerically("E=mc²", "E=mc2")).isTrue();
    }

    @Test
    void shouldTellTitlesInOtherScriptsApart() {
        assertThat(Texts.matchesAlphanumerically("無職転生 ～異世界行ったら本気だす～", "無職転生～異世界行ったら本気だす～")).isTrue();
        assertThat(Texts.matchesAlphanumerically("無職転生 ～異世界行ったら本気だす～", "嘉人本色")).isFalse();
    }

    @Test
    void shouldNotMatchTitlesWithoutLettersOrDigits() {
        assertThat(Texts.matchesAlphanumerically("?!", "...")).isFalse();
    }

    @Test
    void shouldTreatAmpersandAsAnd() {
        assertThat(Texts.matchesAlphanumerically("Dumb & Dumber", "Dumb and Dumber")).isTrue();
    }

    @Test
    void shouldIgnoreHowWordsAreSplit() {
        assertThat(Texts.matchesAlphanumerically("The 40 Year-Old Virgin", "The 40 Year Old Virgin")).isTrue();
        assertThat(Texts.matchesAlphanumerically("Spider-Man", "Spiderman")).isTrue();
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
