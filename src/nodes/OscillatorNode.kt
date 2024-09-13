package nodes

import kotlin.math.*


class OscillatorNode(private val oscillator: (Double) -> Double, private val initialPhase: Double = 0.0) :
    AudioNode(1, 1) {
    private var phase = initialPhase

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val freq = inputs[0]
        phase += ctx.timeStep * freq
        while (phase >= 1.0) phase -= 1.0
        return doubleArrayOf(oscillator(phase))
    }

    override fun reset() {
        phase = initialPhase
    }

    override fun clone(): AudioNode {
        val cloned = OscillatorNode(oscillator, initialPhase)
        cloned.phase = phase
        return cloned
    }

    companion object {
        fun sine(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            OscillatorNode({ sin(2 * PI * it) }, initialPhase).run {
                return if (freq != null) (freq pipe this) else this
            }
        }

        fun square(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            OscillatorNode({ if (it < 0.5) 1.0 else -1.0 }, initialPhase).run {
                return if (freq != null) (freq pipe this) else this
            }
        }

        fun triangle(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            OscillatorNode(
                { if (it < 0.25) 4 * it else if (it < 0.75) 2 - 4 * it else -4 + 4 * it }, initialPhase
            ).run {
                return if (freq != null) (freq pipe this) else this
            }
        }

        fun saw(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            OscillatorNode({ 2 * it - 1 }, initialPhase).run {
                return if (freq != null) (freq pipe this) else this
            }
        }

        fun softSaw(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            val func = { t: Double ->
                val t2 = 2 * t - 1
                t2 * (1 - t2.pow(12.0))
            }
            OscillatorNode(func, initialPhase).run {
                return if (freq != null) (freq pipe this) else this
            }
        }

        fun softSquare(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
            val func = { t: Double ->
                val t2 = 4 * t + 1
                2 * PI * atan(14 * cos(t2 * PI * 0.5))
            }
            OscillatorNode(func, initialPhase).run {
                return if (freq != null) (freq pipe this) else this
            }
        }
    }
}