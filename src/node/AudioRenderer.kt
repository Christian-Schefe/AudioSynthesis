package node

import kotlin.math.roundToInt

class AudioRenderer(private val ctx: Context, private val audioNode: AudioNode) {
    private val sampleRate = ctx.sampleRate

    init {
        require(sampleRate > 0)
        require(audioNode.inputCount == 0)
    }

    fun renderStereo(seconds: Double, channels: Int = 2): Array<DoubleArray> {
        val ticks = (sampleRate * seconds).toInt()

        val outputs = Array(channels) { mutableListOf<Double>() }

        audioNode.init(ctx)

        for (i in 0..<ticks) {
            if (i % (ticks / 4) == 0) {
                println("Rendering... ${(i.toDouble() / ticks * 100).roundToInt()}%")
            }
            val inputs = DoubleArray(0)
            val nodeOutputs = audioNode.process(ctx, inputs)
            for (channelIndex in 0..<channels) {
                outputs[channelIndex].add(nodeOutputs[channelIndex])
            }
            ctx.tick()
        }

        return outputs.map { it.toDoubleArray() }.toTypedArray()
    }
}