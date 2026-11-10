package pl.ciruk.whattowatch.utils.math;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DoublesTest {

    @Test
    void shouldAcceptOnlyValuesGreaterThanThreshold() {
        var greaterThanZero = Doubles.greaterThan(0.0);

        assertThat(greaterThanZero.test(0.5)).isTrue();
        assertThat(greaterThanZero.test(0.0)).isFalse();
        assertThat(greaterThanZero.test(-0.5)).isFalse();
        assertThat(greaterThanZero.test(Double.NaN)).isFalse();
    }
}
