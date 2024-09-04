package nodes


class DelayedNode(private val delay: Double, private val node: AudioNode) :
    AudioNode(node.inputCount, node.outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        if (ctx.time < delay) return DoubleArray(node.outputCount)
        return node.process(ctx, inputs)
    }

    override fun reset() {
        node.reset()
    }

    override fun init(ctx: Context) {
        node.init(ctx)
    }

    override fun clone(): AudioNode = DelayedNode(delay, node.clone())
}