package nodes

import kotlin.math.min

class StereoPan(private val pan: Double) : AudioNode(2, 2) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val (left, right) = inputs
        val l = left * min(1.0, 1.0 - pan)
        val r = right * min(1.0, 1.0 + pan)
        return doubleArrayOf(l, r)
    }

    override fun clone(): AudioNode {
        return StereoPan(pan)
    }
}