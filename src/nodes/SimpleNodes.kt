package nodes


class CustomNode(inputCount: Int, outputCount: Int, private val mapper: (DoubleArray) -> DoubleArray) :
    AudioNode(inputCount, outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return mapper(inputs)
    }

    override fun clone(): AudioNode = CustomNode(inputCount, outputCount, mapper)
}

class PassNode(inputCount: Int) : AudioNode(inputCount, inputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return inputs
    }

    override fun clone(): AudioNode = PassNode(inputCount)
}

class SinkNode(inputCount: Int) : AudioNode(inputCount, 0) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return doubleArrayOf()
    }

    override fun clone(): AudioNode = SinkNode(inputCount)
}

class IgnoreInputsNode(inputCount: Int, val node: AudioNode) : AudioNode(inputCount, node.outputCount) {
    init {
        require(node.inputCount == 0)
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return node.process(ctx, inputs)
    }

    override fun clone(): AudioNode = IgnoreInputsNode(inputCount, node.clone())

    override fun init(ctx: Context) {
        node.init(ctx)
    }

    override fun reset() {
        node.reset()
    }
}

class ConstantNode(private vararg val values: Double) : AudioNode(0, values.size) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return values
    }

    override fun clone(): AudioNode = ConstantNode(*values)
}
