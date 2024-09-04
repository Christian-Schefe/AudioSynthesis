package nodes

class ChorusNode(private val delay: Double, private val voices: Int, private val mix: Double) : AudioNode(1, 1) {
    private var buffer = DoubleArray(0)
    private var bufferPointers = listOf<Int>()

    override fun init(ctx: Context) {
        val delaySamples = (delay * ctx.sampleRate).toInt()
        buffer = DoubleArray(delaySamples)
        reset()
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = bufferPointers.map { buffer[it] }.average()
        buffer[bufferPointers[0]] = inputs[0]
        bufferPointers = bufferPointers.map { (it + 1) % buffer.size }
        return doubleArrayOf(inputs[0] * (1 - mix) + output * mix)
    }

    override fun reset() {
        buffer.fill(0.0)
        bufferPointers = List(voices) { it * buffer.size / voices }
    }

    override fun clone(): AudioNode {
        val cloned = ChorusNode(delay, voices, mix)
        return cloned
    }
}