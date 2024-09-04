package nodes


class ConstantNode(private vararg val values: Double) : AudioNode(0, values.size) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return values
    }

    override fun clone(): AudioNode = ConstantNode(*values)
}
