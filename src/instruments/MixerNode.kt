package instruments

import nodes.AudioNode
import nodes.Context
import kotlin.math.min

class MixerNode : AudioNode(0, 2) {
    private val nodes = mutableListOf<Triple<AudioNode, Double, Double>>()

    fun addNode(node: AudioNode, volume: Double, pan: Double) {
        require(node.outputCount == 2) { "Node must have 2 outputs" }
        require(node.inputCount == 0) { "Node must have 0 inputs" }
        require(volume >= 0.0) { "Volume must be greater than or equal to 0" }
        require(pan in -1.0..1.0) { "Pan must be between -1 and 1" }
        nodes.add(Triple(node, volume, pan))
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = DoubleArray(2)
        for ((node, volume, pan) in nodes) {
            val nodeOutput = node.process(ctx, doubleArrayOf())
            val leftVolume = volume * min(1.0, 1.0 - pan)
            val rightVolume = volume * min(1.0, 1.0 + pan)
            output[0] += nodeOutput[0] * leftVolume
            output[1] += nodeOutput[1] * rightVolume
        }
        return output
    }

    override fun reset() {
        for ((node, _, _) in nodes) {
            node.reset()
        }
    }

    override fun init(ctx: Context) {
        for ((node, _, _) in nodes) {
            node.init(ctx)
        }
    }

    override fun clone(): AudioNode {
        val node = MixerNode()
        for ((n, v, p) in nodes) {
            node.addNode(n.clone(), v, p)
        }
        return node
    }
}