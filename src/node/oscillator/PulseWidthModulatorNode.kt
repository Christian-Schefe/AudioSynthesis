package node.oscillator

import node.AudioNode
import node.Context
import kotlin.math.PI
import kotlin.math.sin

class PulseWidthModulatorNode(
    private val modulationOscillator: (Double) -> Double,
    private val modulationAmount: Double,
    private val amplitude: Double = 1.0,
) : AudioNode(2, 1) {
    private var carrierPhase = 0.0
    private var modulationPhase = 0.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val (carrierFreq, modulatorFreq) = inputs

        carrierPhase += ctx.timeStep * carrierFreq
        while (carrierPhase >= 1.0) carrierPhase -= 1.0

        modulationPhase += ctx.timeStep * modulatorFreq
        while (modulationPhase >= 1.0) modulationPhase -= 1.0

        val dutyCycle = 0.5 + 0.5 * modulationOscillator(modulationPhase) * modulationAmount
        val output = if (carrierPhase < dutyCycle) 1.0 else -1.0
        return doubleArrayOf(output * amplitude)
    }

    override fun reset() {
        carrierPhase = 0.0
        modulationPhase = 0.0
    }

    override fun cloneSettings(): AudioNode {
        val cloned = PulseWidthModulatorNode(modulationOscillator, modulationAmount, amplitude)
        cloned.carrierPhase = carrierPhase
        cloned.modulationPhase = modulationPhase
        return cloned
    }

    companion object {
        fun sinePWM(
            modulationAmount: Double = 1.0, amplitude: Double = 1.0
        ): PulseWidthModulatorNode {
            return PulseWidthModulatorNode({ sin(it * 2 * PI) }, modulationAmount, amplitude)
        }

        fun trianglePWM(
            modulationAmount: Double = 1.0, amplitude: Double = 1.0
        ): PulseWidthModulatorNode {
            return PulseWidthModulatorNode(
                { if (it < 0.25) 4 * it else if (it < 0.75) 2 - 4 * it else -4 + 4 * it }, modulationAmount, amplitude
            )
        }
    }
}