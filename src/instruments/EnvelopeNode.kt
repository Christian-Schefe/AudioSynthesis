package instruments

import nodes.AudioNode
import nodes.Context

abstract class EnvelopeNode : AudioNode(1, 1) {
    private var time = 0.0

    abstract fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val result = applyEnvelope(ctx, time, inputs[0])
        time += ctx.timeStep
        return doubleArrayOf(result)
    }

    override fun reset() {
        time = 0.0
    }

    abstract fun timeToSilence(releaseMoment: Double): Double
}
