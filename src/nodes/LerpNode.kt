package nodes

class LerpNode : AudioNode(3, 1) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val a = inputs[0]
        val b = inputs[1]
        val t = inputs[2]
        return doubleArrayOf(a + (b - a) * t)
    }

    override fun clone(): AudioNode {
        return LerpNode()
    }
}