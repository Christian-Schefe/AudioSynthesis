package effects

import nodes.*
import nodes.Distortion

interface Effect {
    fun buildNode(): AudioNode
}

class LowpassFilter(
    private val frequency: Double, private val q: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        return StereoMixNode(BiquadFilter.lowpass(frequency, q) to BiquadFilter.lowpass(frequency, q), mix)
    }
}

class HighpassFilter(
    private val frequency: Double, private val q: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        return StereoMixNode(BiquadFilter.highpass(frequency, q) to BiquadFilter.highpass(frequency, q), mix)
    }
}

class StereoMixNode(private val nodes: Pair<AudioNode, AudioNode>, val mix: Double) : AudioNode(2, 2) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val left = nodes.first.process(ctx, doubleArrayOf(inputs[0]))[0]
        val right = nodes.second.process(ctx, doubleArrayOf(inputs[1]))[0]
        val mixedLeft = inputs[0] + (left - inputs[0]) * mix
        val mixedRight = inputs[1] + (right - inputs[1]) * mix
        return doubleArrayOf(mixedLeft, mixedRight)
    }

    override fun clone(): AudioNode {
        return StereoMixNode(nodes.first.clone() to nodes.second.clone(), mix)
    }

    override fun reset() {
        nodes.first.reset()
        nodes.second.reset()
    }

    override fun init(ctx: Context) {
        nodes.first.init(ctx)
        nodes.second.init(ctx)
    }
}

class DistortionEffect(
    private val hardness: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        return StereoMixNode(Distortion.tanh(hardness) to Distortion.tanh(hardness), mix)
    }
}

class GainEffect(
    private val gain: Double
) : Effect {
    override fun buildNode(): AudioNode {
        return CustomNode(2, 2) { inputs ->
            doubleArrayOf(inputs[0] * gain, inputs[1] * gain)
        }
    }
}

class ChorusEffect(
    private val voiceCount: Int,
    private val separation: Double,
    private val variance: Double,
    private val modulationSpeed: Double,
    private val mix: Double = 1.0
) : Effect {
    override fun buildNode(): AudioNode {
        return StereoMixNode(
            ChorusNode(0, voiceCount, separation, variance, modulationSpeed) to ChorusNode(
                1, voiceCount, separation, variance, modulationSpeed
            ), mix
        )
    }
}