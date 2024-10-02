package node.filter

import node.AudioNode
import node.Context
import kotlin.math.cos
import kotlin.math.pow
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

        fun bell(sampleRate: Double, center: Double, q: Double, gain: Double): BiquadCoefficients {
            val omega = 2 * Math.PI * center / sampleRate
            val alpha = sin(omega) / (2 * q)
            val a = 10.0.pow(gain / 40.0) // Gain factor in linear scale
            val beta = cos(omega)

            val a0 = 1 + alpha / a
            val a1 = -2 * beta
            val a2 = 1 - alpha / a

            return BiquadCoefficients(
                b0 = (1 + alpha * a) / a0, b1 = -2 * beta / a0, b2 = (1 - alpha * a) / a0, a1 = a1 / a0, a2 = a2 / a0
            )
        }

        fun notch(sampleRate: Double, center: Double, q: Double): BiquadCoefficients {
            val omega = 2 * Math.PI * center / sampleRate
            val alpha = sin(omega) / (2 * q)
            val beta = cos(omega)
            val a0r = 1 / (1 + alpha)
            return BiquadCoefficients(
                b0 = a0r, b1 = -2 * beta * a0r, b2 = a0r, a1 = -2 * beta * a0r, a2 = (1 - alpha) * a0r
            )
        }
    }
}

class BiquadFilter(val coefficientFactory: (Double) -> BiquadCoefficients) : AudioNode(1, 1) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0
    private var coefficients = coefficientFactory(44100.0)

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

    override fun cloneSettings(): AudioNode {
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

        fun bell(center: Double, q: Double, gain: Double): BiquadFilter {
            return BiquadFilter { sampleRate ->
                BiquadCoefficients.bell(sampleRate, center, q, gain)
            }
        }

        fun notch(center: Double, q: Double): BiquadFilter {
            return BiquadFilter { sampleRate ->
                BiquadCoefficients.notch(sampleRate, center, q)
            }
        }
    }
}