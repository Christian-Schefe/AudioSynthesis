package nodes


class CustomNode(inputCount: Int, outputCount: Int, private val mapper: (DoubleArray) -> DoubleArray) :
    AudioNode(inputCount, outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return mapper(inputs)
    }

    override fun clone(): AudioNode = CustomNode(inputCount, outputCount, mapper)

    companion object {
        fun single(mapper: (Double) -> Double): CustomNode {
            return CustomNode(1, 1) { doubleArrayOf(mapper(it[0])) }
        }

        fun multiple(vararg mappers: (Double) -> Double): CustomNode {
            return CustomNode(1, 1) { it.mapIndexed { i, x -> mappers[i](x) }.toDoubleArray() }
        }

        fun pass(inputCount: Int): CustomNode {
            return CustomNode(inputCount, inputCount) { it }
        }

        fun constant(vararg values: Double): CustomNode {
            return CustomNode(0, values.size) { values }
        }

        fun sink(inputCount: Int): CustomNode {
            return CustomNode(inputCount, 0) { doubleArrayOf() }
        }

        fun sum(inputCount: Int): CustomNode {
            return CustomNode(inputCount, 1) { doubleArrayOf(it.sum()) }
        }

        fun product(inputCount: Int): CustomNode {
            return reduce(inputCount) { acc, x -> acc * x }
        }

        fun avg(inputCount: Int): CustomNode {
            return CustomNode(inputCount, 1) { doubleArrayOf(it.average()) }
        }

        private fun reduce(inputCount: Int, operation: (acc: Double, Double) -> Double): CustomNode {
            return CustomNode(inputCount, 1) { doubleArrayOf(it.reduce(operation)) }
        }
        
        fun duplicate(inputCount: Int): CustomNode {
            return CustomNode(inputCount, inputCount * 2) { it + it }
        }
    }
}