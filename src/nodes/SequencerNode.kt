package nodes

class SequencerNode(outputCount: Int) : AudioNode(0, outputCount) {
    private var node = MixerNode(emptyArray(), outputCount, MixerNode.Mode.SUM)

    fun addNode(node: AudioNode, time: Double) {
        this.node = this.node.withAddedNode(DelayedNode(time, node))
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return node.process(ctx, inputs)
    }

    override fun reset() {
        node.reset()
    }

    override fun clone(): AudioNode {
        val cloned = SequencerNode(outputCount)
        cloned.node = node.clone() as MixerNode
        return cloned
    }
}