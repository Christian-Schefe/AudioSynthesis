package nodes


class DelayNode(private val delay: Double, private val sampleRate: Int, channels: Int) : AudioNode(channels, channels) {
    private val buffer = DoubleArray((delay * sampleRate).toInt())
    private var bufferPointer = 0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = buffer[bufferPointer]
        buffer[bufferPointer] = inputs[0]
        bufferPointer = (bufferPointer + 1) % buffer.size
        return doubleArrayOf(output)
    }

    override fun reset() {
        buffer.fill(0.0)
        bufferPointer = 0
    }

    override fun clone(): AudioNode {
        val cloned = DelayNode(delay, sampleRate, inputCount)
        cloned.bufferPointer = bufferPointer
        buffer.copyInto(cloned.buffer)
        return cloned
    }
}