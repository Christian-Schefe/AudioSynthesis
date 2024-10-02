package instruments

import node.*
import node.composite.IgnoreInputsNode
import node.composite.PartialApplicationNode
import node.composite.Pipeline
import node.composite.SinkNode
import node.filter.PinkingFilter
import node.oscillator.FrequencyModulatorNode
import node.oscillator.NoiseNode
import node.oscillator.OscillatorNode
import node.oscillator.PulseWidthModulatorNode


enum class SimpleWaveType(val id: String) {
    SINE("sine"), SQUARE("square"), SAWTOOTH("saw"), TRIANGLE("triangle"), SOFT_SQUARE("soft square"), SOFT_SAWTOOTH("soft saw")
}

enum class NoiseType(val id: String) {
    WHITE_NOISE("white noise"), PINK_NOISE("pink noise"), BROWN_NOISE("brown noise")
}

interface IWaveData {
    fun buildOscillatorNode(phaseOffset: Double): AudioNode
}

data class SimpleWaveData(val type: SimpleWaveType, val amplitude: Double, val phase: Double) : IWaveData {
    override fun buildOscillatorNode(phaseOffset: Double): AudioNode {
        return when (type) {
            SimpleWaveType.SINE -> OscillatorNode.sine(amplitude, phase + phaseOffset)
            SimpleWaveType.SQUARE -> OscillatorNode.square(amplitude, phase + phaseOffset)
            SimpleWaveType.SAWTOOTH -> OscillatorNode.saw(amplitude, phase + phaseOffset)
            SimpleWaveType.TRIANGLE -> OscillatorNode.triangle(amplitude, phase + phaseOffset)
            SimpleWaveType.SOFT_SQUARE -> OscillatorNode.softSquare(amplitude, phase + phaseOffset)
            SimpleWaveType.SOFT_SAWTOOTH -> OscillatorNode.softSaw(amplitude, phase + phaseOffset)
        }
    }
}

data class PulseWaveData(val amplitude: Double, val dutyCycle: Double, val phase: Double) : IWaveData {
    override fun buildOscillatorNode(phaseOffset: Double): AudioNode {
        return OscillatorNode.pulse(amplitude, phase + phaseOffset, dutyCycle)
    }
}

data class NoiseWaveData(val type: NoiseType, val amplitude: Double) : IWaveData {
    override fun buildOscillatorNode(phaseOffset: Double): AudioNode {
        return when (type) {
            NoiseType.WHITE_NOISE -> IgnoreInputsNode(1, NoiseNode(amplitude))
            NoiseType.PINK_NOISE -> Pipeline(listOf(SinkNode(1), NoiseNode(amplitude), PinkingFilter()))
            NoiseType.BROWN_NOISE -> Pipeline(
                listOf(
                    SinkNode(1), NoiseNode(amplitude), PinkingFilter(), PinkingFilter()
                )
            )
        }
    }
}

data class FMWaveData(
    val amplitude: Double, val modulationIndex: Double, val modulationFreqFactor: Double
) : IWaveData {
    override fun buildOscillatorNode(phaseOffset: Double): AudioNode {
        return PartialApplicationNode(
            FrequencyModulatorNode.fm(amplitude, modulationIndex), mapOf(1 to modulationFreqFactor)
        )
    }
}

data class PWMWaveData(
    val amplitude: Double, val modulationAmount: Double, val modulationFreq: Double, val modulatorShape: SimpleWaveType
) : IWaveData {
    override fun buildOscillatorNode(phaseOffset: Double): AudioNode {
        return PartialApplicationNode(
            when (modulatorShape) {
                SimpleWaveType.SINE -> PulseWidthModulatorNode.sinePWM(modulationAmount, amplitude)
                SimpleWaveType.TRIANGLE -> PulseWidthModulatorNode.trianglePWM(modulationAmount, amplitude)
                else -> throw IllegalArgumentException("Unsupported modulator shape: $modulatorShape")
            }, mapOf(1 to modulationFreq)
        )
    }
}

data class WaveComponent(
    val data: IWaveData, val freqFactor: Double, val freqOffset: Double, val envelopes: List<EnvelopeNode>
)
