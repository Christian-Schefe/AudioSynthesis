package effects

import nodes.*
import nodes.Distortion
import kotlin.random.Random

interface Effect {
    fun buildNode(): AudioNode
}

class NoEffect : Effect {
    override fun buildNode(): AudioNode {
        return CustomNode.pass(2)
    }
}

fun mixer(node: AudioNode, mix: Double): AudioNode {
    return (CustomNode.pass(1) branch node stack mix) pipe LerpNode()
}

class LowpassFilter(
    private val frequency: Double, private val q: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val singleChannelNode = mixer(BiquadFilter.lowpass(frequency, q), mix)
        return singleChannelNode stack singleChannelNode.clone()
    }
}

class HighpassFilter(
    private val frequency: Double, private val q: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val singleChannelNode = mixer(BiquadFilter.highpass(frequency, q), mix)
        return singleChannelNode stack singleChannelNode.clone()
    }
}

class DistortionEffect(
    private val hardness: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val singleChannelNode = mixer(Distortion.tanh(hardness), mix)
        return singleChannelNode stack singleChannelNode.clone()
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