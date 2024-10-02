package node.filter

import node.AudioNode
import node.Context
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

data class SvfCoefficients(
    val a1: Double, val a2: Double, val a3: Double, val m0: Double, val m1: Double, val m2: Double
) {
    companion object {
        private fun simple(params: SvfParams, m0: Double, m1: Double, m2: Double): SvfCoefficients {
            val g = tan(PI * params.cutoff / params.sampleRate)
            val k = 1.0 / params.q
            val a1 = 1.0 / (1.0 + g * (g + k))
            val a2 = g * a1
            val a3 = g * a2
            return SvfCoefficients(a1, a2, a3, m0, m1, m2)
        }

        fun lowpass(params: SvfParams): SvfCoefficients {
            return simple(params, 1.0, 0.0, 0.0)
        }

        fun highpass(params: SvfParams): SvfCoefficients {
            return simple(params, 1.0, -1.0 / params.q, -1.0)
        }

        fun bandpass(params: SvfParams): SvfCoefficients {
            return simple(params, 0.0, 1.0, 0.0)
        }

        fun notch(params: SvfParams): SvfCoefficients {
            return simple(params, 1.0, -1.0 / params.q, 0.0)
        }

        fun peak(params: SvfParams): SvfCoefficients {
            return simple(params, 1.0, -1.0 / params.q, -2.0)
        }

        fun allpass(params: SvfParams): SvfCoefficients {
            return simple(params, 1.0, -2.0 / params.q, 0.0)
        }

        fun bell(params: SvfParams): SvfCoefficients {
            require(params.gain != null)
            val a = sqrt(params.gain)
            val g = tan(PI * params.cutoff / params.sampleRate)
            val k = 1.0 / (params.q * a)
            val a1 = 1.0 / (1.0 + g * (g + k))
            val a2 = g * a1
            val a3 = g * a2
            val m0 = 1.0
            val m1 = k * (a * a - 1.0)
            val m2 = 0.0
            return SvfCoefficients(a1, a2, a3, m0, m1, m2)
        }

        fun lowshelf(params: SvfParams): SvfCoefficients {
            require(params.gain != null)
            val a = sqrt(params.gain)
            val g = tan(PI * params.cutoff / params.sampleRate) / sqrt(a)
            val k = 1.0 / params.q
            val a1 = 1.0 / (1.0 + g * (g + k))
            val a2 = g * a1
            val a3 = g * a2
            val m0 = 1.0
            val m1 = k * (a - 1.0)
            val m2 = a * a - 1.0
            return SvfCoefficients(a1, a2, a3, m0, m1, m2)
        }

        fun highshelf(params: SvfParams): SvfCoefficients {
            require(params.gain != null)
            val a = sqrt(params.gain)
            val g = tan(PI * params.cutoff / params.sampleRate) * sqrt(a)
            val k = 1.0 / params.q
            val a1 = 1.0 / (1.0 + g * (g + k))
            val a2 = g * a1
            val a3 = g * a2
            val m0 = a * a
            val m1 = k * (1.0 - a) * a
            val m2 = 1.0 - a * a
            return SvfCoefficients(a1, a2, a3, m0, m1, m2)
        }
    }
}

data class SvfParams(val sampleRate: Double, val cutoff: Double, val q: Double, val gain: Double?)

class SvfSettings(
    private val cutoff: Double?,
    private val q: Double?,
    private val gain: Pair<Boolean, Double?>,
    val factory: (SvfParams) -> SvfCoefficients
) {
    private var prevParams: SvfParams? = null
    private var prevCoefficients: SvfCoefficients? = null

    val inputs = arrayOf(cutoff, q).count { it == null } + if (gain.first && gain.second == null) 1 else 0

    private fun getParams(ctx: Context, inputs: DoubleArray): SvfParams {
        var pointer = 1
        val cutoff = this.cutoff ?: inputs[pointer++]
        val q = this.q ?: inputs[pointer++]
        val gain = if (this.gain.first) {
            this.gain.second ?: inputs[pointer]
        } else {
            null
        }
        return SvfParams(ctx.sampleRate.toDouble(), cutoff, q, gain)
    }

    fun getCoefficients(ctx: Context, inputs: DoubleArray): SvfCoefficients {
        val params = getParams(ctx, inputs)
        if (params != prevParams) {
            prevParams = params
            val coefficients = factory(params)
            prevCoefficients = coefficients
            return coefficients
        }
        return prevCoefficients!!
    }

    companion object {
        fun withoutGain(cutoff: Double?, q: Double?, factory: (SvfParams) -> SvfCoefficients): SvfSettings {
            return SvfSettings(cutoff, q, Pair(false, null), factory)
        }

        fun withGain(cutoff: Double?, q: Double?, gain: Double?, factory: (SvfParams) -> SvfCoefficients): SvfSettings {
            return SvfSettings(cutoff, q, Pair(true, gain), factory)
        }
    }
}

enum class SvfFilterType(val id: String, val hasGain: Boolean) {
    LOWPASS("lowpass", false), HIGHPASS("highpass", false), BANDPASS("bandpass", false), NOTCH(
        "notch", false
    ),
    PEAK("peak", false), ALLPASS("allpass", false), BELL("bell", true), LOWSHELF(
        "lowshelf", true
    ),
    HIGHSHELF("highshelf", true)
}

class SvfFilter(val settings: SvfSettings) : AudioNode(settings.inputs + 1, 1) {
    private var ic1eq = 0.0
    private var ic2eq = 0.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val v0 = inputs[0]
        val coefficients = settings.getCoefficients(ctx, inputs)
        val v3 = v0 - ic2eq
        val v1 = coefficients.a1 * ic1eq + coefficients.a2 * v3
        val v2 = ic2eq + coefficients.a2 * ic1eq + coefficients.a3 * v3
        ic1eq = 2.0 * v1 - ic1eq
        ic2eq = 2.0 * v2 - ic2eq
        return doubleArrayOf(coefficients.m0 * v0 + coefficients.m1 * v1 + coefficients.m2 * v2)
    }

    override fun cloneSettings(): AudioNode {
        return SvfFilter(settings)
    }

    override fun reset() {
        ic1eq = 0.0
        ic2eq = 0.0
    }

    companion object {
        fun lowpass(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::lowpass))
        }

        fun highpass(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::highpass))
        }

        fun bandpass(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::bandpass))
        }

        fun notch(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::notch))
        }

        fun peak(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::peak))
        }

        fun allpass(cutoff: Double?, q: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withoutGain(cutoff, q, SvfCoefficients::allpass))
        }

        fun bell(cutoff: Double?, q: Double?, gain: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withGain(cutoff, q, gain, SvfCoefficients::bell))
        }

        fun lowshelf(cutoff: Double?, q: Double?, gain: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withGain(cutoff, q, gain, SvfCoefficients::lowshelf))
        }

        fun highshelf(cutoff: Double?, q: Double?, gain: Double?): SvfFilter {
            return SvfFilter(SvfSettings.withGain(cutoff, q, gain, SvfCoefficients::highshelf))
        }

        fun fromType(type: SvfFilterType, cutoff: Double?, q: Double?, gain: Double?): SvfFilter {
            require(type.hasGain || gain == null) { "Gain cannot be provided for this filter type" }
            return when (type) {
                SvfFilterType.LOWPASS -> lowpass(cutoff, q)
                SvfFilterType.HIGHPASS -> highpass(cutoff, q)
                SvfFilterType.BANDPASS -> bandpass(cutoff, q)
                SvfFilterType.NOTCH -> notch(cutoff, q)
                SvfFilterType.PEAK -> peak(cutoff, q)
                SvfFilterType.ALLPASS -> allpass(cutoff, q)
                SvfFilterType.BELL -> bell(cutoff, q, gain)
                SvfFilterType.LOWSHELF -> lowshelf(cutoff, q, gain)
                SvfFilterType.HIGHSHELF -> highshelf(cutoff, q, gain)
            }
        }
    }
}