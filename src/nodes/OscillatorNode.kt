package nodes

import kotlin.math.*


class OscillatorNode(
    private val oscillator: (Double) -> Double, private val amplitude: Double, private val initialPhase: Double
) : AudioNode(1, 1) {
    private var phase = initialPhase % 1.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val freq = inputs[0]
        phase += ctx.timeStep * freq
        while (phase >= 1.0) phase -= 1.0
        return doubleArrayOf(oscillator(phase) * amplitude)
    }

    override fun reset() {
        phase = initialPhase
    }

    override fun clone(): AudioNode {
        val cloned = OscillatorNode(oscillator, amplitude, initialPhase)
        cloned.phase = phase
        return cloned
    }

    companion object {
        fun sine(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            return OscillatorNode({ sin(2 * PI * it) }, amplitude, initialPhase)
        }

        fun square(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            return OscillatorNode({ if (it < 0.5) 1.0 else -1.0 }, amplitude, initialPhase)
        }

        fun pulse(amplitude: Double = 1.0, initialPhase: Double = 0.0, dutyCycle: Double = 0.5): AudioNode {
            return OscillatorNode({ if (it < dutyCycle) 1.0 else -1.0 }, amplitude, initialPhase)
        }

        fun triangle(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            return OscillatorNode(
                { if (it < 0.25) 4 * it else if (it < 0.75) 2 - 4 * it else -4 + 4 * it }, amplitude, initialPhase
            )
        }

        fun saw(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            return OscillatorNode({ 1 - 2 * it }, amplitude, initialPhase)
        }

        fun softSaw(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            val func = { t: Double ->
                val t2 = 1 - (2 * t)
                t2 * (1 - t2.pow(12.0))
            }
            return OscillatorNode(func, amplitude, initialPhase)
        }

        fun softSquare(amplitude: Double = 1.0, initialPhase: Double = 0.0): AudioNode {
            val func = { t: Double ->
                val t2 = 4 * t - 1
                0.66 * atan(14 * cos(t2 * PI * 0.5))
            }
            return OscillatorNode(func, amplitude, initialPhase)
        }
    }
}