package nodes

import kotlin.random.Random

class NoiseNode(outputCount: Int = 1): AudioNode(0, outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = DoubleArray(outputCount)
        for (i in 0 ..< outputCount) {
            output[i] = Random.nextDouble()
        }
        return output
    }

    override fun clone(): AudioNode {
        return NoiseNode(outputCount)
    }
}