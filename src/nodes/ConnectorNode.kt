package nodes

class ConnectorNode(private val mode: Mode, private val leftNode: AudioNode, private val rightNode: AudioNode) :
    AudioNode(calcInputs(mode, leftNode, rightNode), calcOutputs(mode, leftNode, rightNode)) {

    enum class Mode {
        PIPE, STACK, STACK_ADD, STACK_MULTIPLY, BUS_SPLIT, BUS_ADD, BUS_MULTIPLY
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val leftInput = when (mode) {
            Mode.STACK, Mode.STACK_ADD, Mode.STACK_MULTIPLY -> inputs.copyOfRange(0, leftNode.inputCount)
            Mode.PIPE, Mode.BUS_SPLIT, Mode.BUS_ADD, Mode.BUS_MULTIPLY -> inputs
        }

        val leftOutput = leftNode.process(ctx, leftInput)

        val rightInput = when (mode) {
            Mode.PIPE -> leftOutput
            Mode.STACK, Mode.STACK_ADD, Mode.STACK_MULTIPLY -> inputs.copyOfRange(leftNode.inputCount, inputs.size)
            Mode.BUS_SPLIT, Mode.BUS_ADD, Mode.BUS_MULTIPLY -> inputs
        }

        val rightOutput = rightNode.process(ctx, rightInput)

        return when (mode) {
            Mode.PIPE -> rightOutput
            Mode.STACK, Mode.BUS_SPLIT -> DoubleArray(outputCount) { i ->
                if (i < leftNode.outputCount) leftOutput[i] else rightOutput[i - leftNode.outputCount]
            }

            Mode.STACK_ADD, Mode.BUS_ADD -> DoubleArray(outputCount) { i ->
                leftOutput[i] + rightOutput[i]
            }

            Mode.STACK_MULTIPLY, Mode.BUS_MULTIPLY -> DoubleArray(outputCount) { i ->
                leftOutput[i] * rightOutput[i]
            }
        }
    }

    override fun clone(): AudioNode {
        return ConnectorNode(mode, leftNode.clone(), rightNode.clone())
    }

    override fun init(ctx: Context) {
        leftNode.init(ctx)
        rightNode.init(ctx)
    }

    override fun reset() {
        leftNode.reset()
        rightNode.reset()
    }

    companion object {
        private fun calcInputs(mode: Mode, leftNode: AudioNode, rightNode: AudioNode): Int {
            if (mode == Mode.BUS_SPLIT || mode == Mode.BUS_ADD || mode == Mode.BUS_MULTIPLY) {
                require(leftNode.inputCount == rightNode.inputCount)
            }

            if (mode == Mode.PIPE) {
                require(leftNode.outputCount == rightNode.inputCount)
            }

            return when (mode) {
                Mode.STACK, Mode.STACK_ADD, Mode.STACK_MULTIPLY -> leftNode.inputCount + rightNode.inputCount
                Mode.PIPE, Mode.BUS_SPLIT, Mode.BUS_ADD, Mode.BUS_MULTIPLY -> leftNode.inputCount
            }
        }

        private fun calcOutputs(mode: Mode, leftNode: AudioNode, rightNode: AudioNode): Int {
            if (mode == Mode.STACK || mode == Mode.BUS_SPLIT) {
                require(leftNode.outputCount == rightNode.outputCount)
            }

            return when (mode) {
                Mode.PIPE -> rightNode.outputCount
                Mode.STACK, Mode.BUS_SPLIT -> leftNode.outputCount + rightNode.outputCount
                Mode.STACK_ADD, Mode.STACK_MULTIPLY, Mode.BUS_ADD, Mode.BUS_MULTIPLY -> leftNode.outputCount
            }
        }
    }
}