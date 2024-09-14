package nodes

import kotlin.math.cos
import kotlin.math.sin

data class BiquadCoefficients(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
    companion object {
        fun lowpass(sampleRate: Double, cutoff: Double, q: Double): BiquadCoefficients {
            val omega = 2 * Math.PI * cutoff / sampleRate
            val alpha = sin(omega) / (2 * q)
            val beta = cos(omega)
            val a0r = 1 / (1 + alpha)
            return BiquadCoefficients(
                b0 = (1 - beta) * 0.5 * a0r,
                b1 = (1 - beta) * a0r,
                b2 = (1 - beta) * 0.5 * a0r,
                a1 = -2 * beta * a0r,
                a2 = (1 - alpha) * a0r
            )
        }

        fun highpass(sampleRate: Double, cutoff: Double, q: Double): BiquadCoefficients {
            val omega = 2 * Math.PI * cutoff / sampleRate
            val alpha = sin(omega) / (2 * q)
            val beta = cos(omega)
            val a0r = 1 / (1 + alpha)
            return BiquadCoefficients(
                b0 = (1 + beta) * 0.5 * a0r,
                b1 = -(1 + beta) * a0r,
                b2 = (1 + beta) * 0.5 * a0r,
                a1 = -2 * beta * a0r,
                a2 = (1 - alpha) * a0r
            )
        }

        fun bandpass(sampleRate: Double, center: Double, q: Double): BiquadCoefficients {
            val omega = 2 * Math.PI * center / sampleRate
            val alpha = sin(omega) / (2 * q)
            val beta = cos(omega)
            val a0r = 1 / (1 + alpha)
            return BiquadCoefficients(
                b0 = alpha * a0r, b1 = 0.0, b2 = -alpha * a0r, a1 = -2 * beta * a0r, a2 = (1 - alpha) * a0r
            )
        }
    }
}

class BiquadFilter(val coefficientFactory: (Double) -> BiquadCoefficients) : AudioNode(1, 1) {
    var x1 = 0.0
    var x2 = 0.0
    var y1 = 0.0
    var y2 = 0.0
    var coefficients = coefficientFactory(44100.0)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val x0 = inputs[0]
        val y0 =
            coefficients.b0 * x0 + coefficients.b1 * x1 + coefficients.b2 * x2 - coefficients.a1 * y1 - coefficients.a2 * y2
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        return doubleArrayOf(y0)
    }

    override fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    override fun clone(): AudioNode {
        return BiquadFilter(coefficientFactory)
    }

    override fun init(ctx: Context) {
        coefficients = coefficientFactory(ctx.sampleRate.toDouble())
    }

    companion object {
        fun lowpass(cutoff: Double, q: Double): BiquadFilter {
            return BiquadFilter { sampleRate ->
                BiquadCoefficients.lowpass(sampleRate, cutoff, q)
            }
        }

        fun highpass(cutoff: Double, q: Double): BiquadFilter {
            return BiquadFilter { sampleRate ->
                BiquadCoefficients.highpass(sampleRate, cutoff, q)
            }
        }

        fun bandpass(center: Double, q: Double): BiquadFilter {
            return BiquadFilter { sampleRate ->
                BiquadCoefficients.bandpass(sampleRate, center, q)
            }
        }
    }
}