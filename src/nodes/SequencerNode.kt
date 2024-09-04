package nodes

class SequencerNode(outputCount: Int) : AudioNode(0, outputCount) {
    private var node: AudioNode? = null

    fun addNode(node: AudioNode, time: Double) {
        this.node = this.node?.let {
            it + DelayedNode(time, node)
        } ?: node
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return node?.process(ctx, inputs) ?: DoubleArray(outputCount)
    }

    override fun reset() {
        node?.reset()
    }

    override fun init(ctx: Context) {
        node?.init(ctx)
    }

    override fun clone(): AudioNode {
        val cloned = SequencerNode(outputCount)
        cloned.node = node?.clone()
        return cloned
    }
}