package nodes

class ModulatedDelay(private val tapCount: Int, private val maxDelay: Double) : AudioNode(tapCount + 1, 1) {
    private var bufferSize = (44100 * maxDelay).toInt() + 3
    private var buffer = DoubleArray(bufferSize)
    private var pointer = 0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        var output = 0.0

        for (i in 1..tapCount) {
            val delay = inputs[i].coerceIn(0.0, maxDelay)
            val delaySamples = delay * ctx.sampleRate
            val delaySamplesFloor = delaySamples.toInt()

            val readPointer1 = (pointer - delaySamplesFloor + bufferSize) % bufferSize
            val readPointer0 = (readPointer1 - 1 + bufferSize) % bufferSize
            val readPointer2 = (readPointer1 + 1) % bufferSize
            val readPointer3 = (readPointer1 + 2) % bufferSize

            output += cubicInterpolation(
                buffer[readPointer0],
                buffer[readPointer1],
                buffer[readPointer2],
                buffer[readPointer3],
                delaySamples - delaySamplesFloor
            )
        }

        buffer[pointer] = inputs[0]
        pointer = (pointer + 1) % bufferSize

        return doubleArrayOf(output)
    }

    override fun clone(): AudioNode {
        val node = ModulatedDelay(tapCount, maxDelay)
        node.buffer = buffer.copyOf()
        node.pointer = pointer
        return node
    }

    override fun init(ctx: Context) {
        bufferSize = (ctx.sampleRate * maxDelay).toInt() + 3
        buffer = DoubleArray(bufferSize)
    }

    private fun cubicInterpolation(x0: Double, x1: Double, x2: Double, x3: Double, t: Double): Double {
        return x1 + 0.5 * t * (x2 - x0 + t * (2.0 * x0 - 5.0 * x1 + 4.0 * x2 - x3 + t * (3.0 * (x1 - x2) + x3 - x0)))
    }
}