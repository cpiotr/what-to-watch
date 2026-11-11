package pl.ciruk.whattowatch.neural

import kotlin.math.sqrt

/**
 * Linear regression with an L2 penalty on the weights. Features are standardised first,
 * so the penalty treats them alike whatever their scale.
 */
internal class RidgeRegression private constructor(
        private val means: DoubleArray,
        private val scales: DoubleArray,
        private val weights: DoubleArray,
        private val intercept: Double) {

    fun predict(features: DoubleArray): Double {
        return intercept + features.indices.sumOf { weights[it] * (features[it] - means[it]) / scales[it] }
    }

    companion object {
        fun fit(rows: List<DoubleArray>, targets: DoubleArray, penalty: Double): RidgeRegression {
            require(rows.isNotEmpty() && rows.size == targets.size) { "Need one target per row, and at least one row" }
            val width = rows.first().size
            val means = DoubleArray(width) { column -> rows.sumOf { it[column] } / rows.size }
            // A feature that never varies gets no weight, so any scale but 0 will do
            val scales = DoubleArray(width) { column ->
                sqrt(rows.sumOf { (it[column] - means[column]).let { d -> d * d } } / rows.size).takeIf { it > 0 } ?: 1.0
            }
            val standardised = rows.map { row -> DoubleArray(width) { (row[it] - means[it]) / scales[it] } }
            val meanTarget = targets.average()

            // Normal equations: (XᵀX + penalty·I) w = Xᵀ(y - mean of y)
            val gram = Array(width) { i ->
                DoubleArray(width) { j -> standardised.sumOf { it[i] * it[j] } + if (i == j) penalty else 0.0 }
            }
            val moments = DoubleArray(width) { i -> standardised.indices.sumOf { standardised[it][i] * (targets[it] - meanTarget) } }
            return RidgeRegression(means, scales, solve(gram, moments), meanTarget)
        }

        // Gaussian elimination. With a positive penalty the matrix is positive definite, so it needs no pivoting.
        private fun solve(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray {
            val a = Array(matrix.size) { matrix[it].copyOf() }
            val b = vector.copyOf()
            for (pivot in a.indices) {
                for (row in pivot + 1 until a.size) {
                    val factor = a[row][pivot] / a[pivot][pivot]
                    for (column in pivot until a.size) {
                        a[row][column] -= factor * a[pivot][column]
                    }
                    b[row] -= factor * b[pivot]
                }
            }
            val solution = DoubleArray(a.size)
            for (row in a.indices.reversed()) {
                val known = (row + 1 until a.size).sumOf { a[row][it] * solution[it] }
                solution[row] = (b[row] - known) / a[row][row]
            }
            return solution
        }
    }
}
