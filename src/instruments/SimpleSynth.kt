package instruments

import nodes.*
import song.Song
import kotlin.random.Random

interface Synth {
    fun buildNode(random: Random): AudioNode
    fun buildAudio(song: Song, noteFilter: NoteFilter, random: Random): AudioNode
}

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
            ModulatedOscillatorNode.fm(amplitude, modulationIndex), mapOf(1 to modulationFreqFactor)
        )
    }
}

data class WaveComponent(
    val data: IWaveData, val freqFactor: Double, val freqOffset: Double, val envelopes: List<EnvelopeNode>
)

class SimpleSynth(
    private val vibrato: Double, private val mix: List<WaveComponent>
) : Synth {
    override fun buildNode(random: Random): AudioNode {
        val vibratoFreq = random.nextDouble(4.0, 5.0)
        val phaseOffset = random.nextDouble(0.0, 1.0)
        return SynthNode(vibratoFreq, phaseOffset, vibrato, mix)
    }

    override fun buildAudio(song: Song, noteFilter: NoteFilter, random: Random): AudioNode {
        val factory = { buildNode(random) }
        val releaseTimeEvaluator = ReleaseTimeEvaluator { releaseMoment ->
            mix.maxOfOrNull {
                it.envelopes.maxOfOrNull { env ->
                    env.timeToSilence(releaseMoment)
                } ?: 0.0
            } ?: 0.0
        }
        val instrumentPlayer = InstrumentPlayer(factory, random, song, noteFilter, 36, releaseTimeEvaluator)
        return instrumentPlayer
    }

    class WaveNode(private val phaseOffset: Double, private val component: WaveComponent) : AudioNode(3, 2) {

        private val envelopeNodes = component.envelopes.map { it.clone() }

        private val oscillatorNode = component.data.buildOscillatorNode(phaseOffset)

        override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
            val freq = inputs[0] * component.freqFactor + component.freqOffset
            val noteOn = inputs[1]
            val velocity = inputs[2]

            val envelopeValues = envelopeNodes.map { it.process(ctx, doubleArrayOf(noteOn))[0] }
            val volume = envelopeValues.fold(1.0) { acc, value -> acc * value } * velocity
            val output = oscillatorNode.process(ctx, doubleArrayOf(freq))[0] * volume
            return doubleArrayOf(output, output)
        }

        override fun clone(): AudioNode {
            return WaveNode(phaseOffset, component)
        }

        override fun reset() {
            oscillatorNode.reset()
            for (envelope in envelopeNodes) {
                envelope.reset()
            }
        }

        override fun init(ctx: Context) {
            oscillatorNode.init(ctx)
            for (envelope in envelopeNodes) {
                envelope.init(ctx)
            }
        }
    }

    class SynthNode(
        private val vibratoFreq: Double,
        private val phaseOffset: Double,
        private val vibrato: Double,
        private val mix: List<WaveComponent>
    ) : AudioNode(4, 2) {
        private val vibratoNode = VibratoNode(1.0, vibrato, vibratoFreq)

        private val waveNodes = mix.map { data ->
            WaveNode(phaseOffset, data)
        }

        override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
            val (freq, noteOn, velocity, _) = inputs

            val modFreq = vibratoNode.process(ctx, doubleArrayOf(freq))[0]

            val outputs = waveNodes.map { it.process(ctx, doubleArrayOf(modFreq, noteOn, velocity)) }
            val output = outputs.fold(doubleArrayOf(0.0, 0.0)) { acc, value ->
                doubleArrayOf(acc[0] + value[0], acc[1] + value[1])
            }

            return output
        }

        override fun clone(): AudioNode {
            return SynthNode(vibratoFreq, phaseOffset, vibrato, mix)
        }

        override fun init(ctx: Context) {
            vibratoNode.init(ctx)
            for (waveNode in waveNodes) {
                waveNode.init(ctx)
            }
        }

        override fun reset() {
            vibratoNode.reset()
            for (waveNode in waveNodes) {
                waveNode.reset()
            }
        }
    }
}