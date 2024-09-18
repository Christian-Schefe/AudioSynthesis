package effects

import nodes.AudioNode
import nodes.BiquadFilter
import nodes.Context
import nodes.CustomNode

interface Effect {
    fun buildNode(): AudioNode
}

class NoEffect : Effect {
    override fun buildNode(): AudioNode {
        return CustomNode.pass(2)
    }
}

class FourBandEqualizer(
    private val low: Double, private val lowMid: Double, private val highMid: Double, private val high: Double
) : Effect {
    override fun buildNode(): AudioNode {
        val highpass = BiquadFilter.highpass(100.0, 0.7, low)
        val notchLow = BiquadFilter.bell(500.0, 0.7, lowMid)
        val notchHigh = BiquadFilter.bell(2000.0, 0.7, highMid)
        val lowpass = BiquadFilter.lowpass(5000.0, 0.7, high)
        return EQNode(
            listOf(
                (lowpass to lowpass.clone()) to 1.0,
                (notchLow to notchLow.clone()) to 1.0,
                (notchHigh to notchHigh.clone()) to 1.0,
                (highpass to highpass.clone()) to 1.0
            )
        )
    }

    class EQNode(private val filters: List<Pair<Pair<AudioNode, AudioNode>, Double>>) : AudioNode(2, 2) {
        override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
            var leftOutput = inputs[0]
            var rightOutput = inputs[1]
            for ((filterPair, mix) in filters) {
                val (leftFilter, rightFilter) = filterPair
                val left = leftFilter.process(ctx, doubleArrayOf(leftOutput))
                val right = rightFilter.process(ctx, doubleArrayOf(rightOutput))
                leftOutput = left[0] * mix + leftOutput * (1 - mix)
                rightOutput = right[0] * mix + rightOutput * (1 - mix)
            }
            return doubleArrayOf(leftOutput, rightOutput)
        }

        override fun clone(): AudioNode {
            return EQNode(filters.map { (filterPair, mix) ->
                Pair(Pair(filterPair.first.clone() as BiquadFilter, filterPair.second.clone() as BiquadFilter), mix)
            })
        }

        override fun reset() {
            filters.forEach { (filterPair, _) ->
                filterPair.first.reset()
                filterPair.second.reset()
            }
        }

        override fun init(ctx: Context) {
            filters.forEach { (filterPair, _) ->
                filterPair.first.init(ctx)
                filterPair.second.init(ctx)
            }
        }
    }
}