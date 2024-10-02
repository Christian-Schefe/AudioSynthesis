package node.composite

import node.AudioNode
import node.Context


class CustomNode(inputCount: Int, outputCount: Int, private val mapper: (DoubleArray) -> DoubleArray) :
    AudioNode(inputCount, outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return mapper(inputs)
    }

    override fun cloneSettings(): AudioNode = CustomNode(inputCount, outputCount, mapper)
}

class PassNode(inputCount: Int) : AudioNode(inputCount, inputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return inputs
    }

    override fun cloneSettings(): AudioNode = PassNode(inputCount)
}

class SinkNode(inputCount: Int) : AudioNode(inputCount, 0) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return doubleArrayOf()
    }

    override fun cloneSettings(): AudioNode = SinkNode(inputCount)
}

class IgnoreInputsNode(inputCount: Int, private val node: AudioNode) : AudioNode(inputCount, node.outputCount) {
    init {
        require(node.inputCount == 0)
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return node.process(ctx, inputs)
    }

    override fun cloneSettings(): AudioNode = IgnoreInputsNode(inputCount, node.cloneSettings())

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

    override fun cloneSettings(): AudioNode = ConstantNode(*values)
}

class PartialApplicationNode(private val node: AudioNode, private val inputs: Map<Int, Double>) :
    AudioNode(node.inputCount - inputs.size, node.outputCount) {
    init {
        inputs.keys.forEach { require(it in 0..<node.inputCount) { "Invalid index: $it of ${node.inputCount}" } }
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        var inputPointer = 0
        val actualInputs = DoubleArray(node.inputCount) {
            if (it in this.inputs) {
                this.inputs[it]!!
            } else {
                val value = inputs[inputPointer]
                inputPointer++
                value
            }
        }
        return node.process(ctx, actualInputs)
    }

    override fun cloneSettings(): AudioNode = PartialApplicationNode(node.cloneSettings(), inputs)

    override fun init(ctx: Context) {
        node.init(ctx)
    }

    override fun reset() {
        node.reset()
    }
}