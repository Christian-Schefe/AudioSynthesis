package node.effect

import node.AudioNode
import node.Context
import node.oscillator.OscillatorNode
import kotlin.math.*

class VibratoNode(private val fadeIn: Double, private val amount: Double, private val frequency: Double) : AudioNode(1, 1) {
    private var oscillatorNode = OscillatorNode.sine()
    var time = 0.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val oscVal = oscillatorNode.process(ctx, doubleArrayOf(frequency))[0] * 0.5 + 0.5
        val factor = 1 + amount * min(time / fadeIn, 1.0)

        time += ctx.timeStep

        val result = xerp(1.0 / factor, factor, oscVal)
        return doubleArrayOf(result * inputs[0])
    }

    override fun cloneSettings(): AudioNode {
        val clone = VibratoNode(fadeIn, amount, frequency)
        clone.time = time
        clone.oscillatorNode = oscillatorNode.cloneSettings()
        return clone
    }

    override fun reset() {
        time = 0.0
        oscillatorNode.reset()
    }

    override fun init(ctx: Context) {
        oscillatorNode.init(ctx)
    }

    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a * (1 - t) + b * t
    }

    private fun xerp(a: Double, b: Double, t: Double): Double {
        require(a > 0 && b > 0)
        return exp(lerp(ln(a), ln(b), t))
    }
}
