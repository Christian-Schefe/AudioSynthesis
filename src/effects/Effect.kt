package effects

import nodes.*

interface Effect {
    fun buildNode(): AudioNode
}

class NoEffect : Effect {
    override fun buildNode(): AudioNode {
        return CustomNode.pass(2)
    }
}

class LowpassFilter(
    private val frequency: Double, private val q: Double, private val gain: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val singleChannelNode =
            ((CustomNode.pass(1) branch BiquadFilter.lowpass(frequency, q, gain)) stack mix) pipe LerpNode()
        return singleChannelNode stack singleChannelNode.clone()
    }
}

class HighpassFilter(
    private val frequency: Double, private val q: Double, private val gain: Double, private val mix: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val singleChannelNode =
            ((CustomNode.pass(1) branch BiquadFilter.highpass(frequency, q, gain)) stack mix) pipe LerpNode()
        return singleChannelNode stack singleChannelNode.clone()
    }
}