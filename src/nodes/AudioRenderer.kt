package nodes

class AudioRenderer(private val audioNode: AudioNode, private val sampleRate: Int) {
    init {
        require(sampleRate > 0)
        require(audioNode.inputCount == 0)
    }

    fun renderMono(seconds: Double, channels: Int = 2): Array<DoubleArray> {
        val ticks = (sampleRate * seconds).toInt()

        val ctx = Context(sampleRate)
        val outputs = Array(channels, { mutableListOf<Double>() })

        for (i in 0..<ticks) {
            val inputs = DoubleArray(0)
            val nodeOutput = audioNode.process(ctx, inputs)[0]
            for (channelIndex in 0..<channels) {
                outputs[channelIndex].add(nodeOutput)
            }
            ctx.tick()
        }

        return outputs.map { it.toDoubleArray() }.toTypedArray()
    }

    fun renderStereo(seconds: Double, channels: Int = 2): Array<DoubleArray> {
        val ticks = (sampleRate * seconds).toInt()

        val ctx = Context(sampleRate)
        val outputs = Array(channels, { mutableListOf<Double>() })

        for (i in 0..<ticks) {
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