package node.filter

import node.AudioNode
import node.Context

class PinkingFilter : AudioNode(1, 1) {
    private var b0: Double = 0.0
    private var b1: Double = 0.0
    private var b2: Double = 0.0
    private var b3: Double = 0.0
    private var b4: Double = 0.0
    private var b5: Double = 0.0
    private var b6: Double = 0.0

    override fun reset() {
        b0 = 0.0
        b1 = 0.0
        b2 = 0.0
        b3 = 0.0
        b4 = 0.0
        b5 = 0.0
        b6 = 0.0
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val x = inputs[0]

        b0 = 0.99886 * b0 + x * 0.0555179
        b1 = 0.99332 * b1 + x * 0.0750759
        b2 = 0.96900 * b2 + x * 0.1538520
        b3 = 0.86650 * b3 + x * 0.3104856
        b4 = 0.55000 * b4 + x * 0.5329522
        b5 = -0.7616 * b5 - x * 0.0168980

        val out = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + x * 0.5362) * 0.115830421
        b6 = x * 0.115926

        return doubleArrayOf(out)
    }

    override fun cloneSettings(): AudioNode {
        return PinkingFilter()
    }
}