package nodes

class Pipeline(val nodes: List<AudioNode>) : AudioNode(nodes.first().inputCount, nodes.last().outputCount) {
    init {
        require(nodes.isNotEmpty())
        for (i in 1..<nodes.size) {
            require(nodes[i - 1].outputCount == nodes[i].inputCount)
        }
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        var current = inputs
        for (node in nodes) {
            current = node.process(ctx, current)
        }
        return current
    }

    override fun clone(): AudioNode {
        return Pipeline(nodes.map { it.clone() })
    }

    override fun init(ctx: Context) {
        nodes.forEach { it.init(ctx) }
    }

    override fun reset() {
        nodes.forEach { it.reset() }
    }
}