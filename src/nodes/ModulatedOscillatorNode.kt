package nodes

import kotlin.math.PI
import kotlin.math.sin

class ModulatedOscillatorNode(
    private val carrierOscillator: (Double) -> Double,
    private val modulatorOscillator: (Double) -> Double,
    private val amplitude: Double = 1.0,
    private val modulationIndex: Double = 1.0
) : AudioNode(2, 1) {
    private var carrierPhase = 0.0
    private var modulatorPhase = 0.0

    private val invTwoPi = 1.0 / (2 * PI)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val (carrierFreq, modulatorFreqFactor) = inputs

        modulatorPhase += modulatorFreqFactor * carrierFreq * ctx.timeStep
        while (modulatorPhase >= 1.0) modulatorPhase -= 1.0

        carrierPhase += carrierFreq * ctx.timeStep
        while (carrierPhase >= 1.0) carrierPhase -= 1.0

        val modulatorValue = modulatorOscillator(modulatorPhase) * modulationIndex * invTwoPi

        return doubleArrayOf(carrierOscillator(carrierPhase + modulatorValue) * amplitude)
    }

    override fun reset() {
        carrierPhase = 0.0
        modulatorPhase = 0.0
    }

    override fun clone(): AudioNode {
        val cloned = ModulatedOscillatorNode(
            carrierOscillator, modulatorOscillator, amplitude, modulationIndex
        )
        cloned.carrierPhase = carrierPhase
        cloned.modulatorPhase = modulatorPhase
        return cloned
    }

    companion object {
        fun fm(amplitude: Double = 1.0, modulationIndex: Double = 1.0): ModulatedOscillatorNode {
            return ModulatedOscillatorNode({ sin(2 * PI * it) }, { sin(2 * PI * it) }, amplitude, modulationIndex
            )
        }
    }
}