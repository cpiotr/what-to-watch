package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class RidgeRegressionTest {
    @Test
    fun shouldRecoverLinearRelationship() {
        val rows = (0 until 50).map { doubleArrayOf(it % 7.0, it % 5.0) }
        val targets = rows.map { 2.0 + 3.0 * it[0] - it[1] }.toDoubleArray()

        val model = RidgeRegression.fit(rows, targets, 1e-9)

        assertThat(model.predict(doubleArrayOf(4.0, 1.0))).isCloseTo(13.0, within(1e-6))
    }

    @Test
    fun shouldIgnoreFeatureThatNeverVaries() {
        val rows = (0 until 20).map { doubleArrayOf(it.toDouble(), 1.0) }
        val targets = rows.map { 1.0 + it[0] }.toDoubleArray()

        val model = RidgeRegression.fit(rows, targets, 1e-9)

        assertThat(model.predict(doubleArrayOf(5.0, 1.0))).isCloseTo(6.0, within(1e-6))
        assertThat(model.predict(doubleArrayOf(5.0, 0.0))).isCloseTo(6.0, within(1e-6))
    }

    @Test
    fun shouldShrinkWeightsTowardsMeanWithStrongPenalty() {
        val rows = (0 until 20).map { doubleArrayOf(it.toDouble()) }
        val targets = rows.map { it[0] }.toDoubleArray()

        val model = RidgeRegression.fit(rows, targets, 1e9)

        assertThat(model.predict(doubleArrayOf(19.0))).isCloseTo(targets.average(), within(1e-3))
    }
}
