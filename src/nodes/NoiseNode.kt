package nodes

import kotlin.random.Random

class NoiseNode : AudioNode(0, 1) {
    var random = Random(0)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return doubleArrayOf(random.nextDouble() * 2 - 1)
    }

    override fun clone(): AudioNode {
        return NoiseNode()
    }

    override fun init(ctx: Context) {
        random = ctx.random
    }
}