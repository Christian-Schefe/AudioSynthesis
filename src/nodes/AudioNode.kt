package nodes

abstract class AudioNode(val inputCount: Int, val outputCount: Int) {
    open fun init(ctx: Context) {}
    abstract fun process(ctx: Context, inputs: DoubleArray): DoubleArray
    abstract fun clone(): AudioNode
    open fun reset() {}
}