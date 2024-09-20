package instruments

import nodes.*
import song.Song
import kotlin.random.Random

interface Synth {
    fun buildNode(random: Random): AudioNode
    fun buildAudio(song: Song, noteFilter: NoteFilter, random: Random): AudioNode
}

data class ADSR(val attack: Double, val decay: Double, val sustain: Double, val release: Double)
enum class WaveType(val id: String) {
    SINE("sine"), SQUARE("square"), SAWTOOTH("saw"), TRIANGLE("triangle"), SOFT_SQUARE("soft square"), SOFT_SAWTOOTH("soft saw"), NOISE(
        "noise"
    )
}

data class WaveData(val amplitude: Double, val phase: Double)

class SimpleSynth(
    private val vibrato: Double,
    private val adsr: ADSR,
    private val detune: Double,
    private val freqOverride: Double?,
    private val mix: Map<WaveType, WaveData>
) : Synth {
    override fun buildNode(random: Random): AudioNode {
        val vibratoFreq = random.nextDouble(4.0, 5.0)
        val phaseOffset = random.nextDouble(0.0, 1.0)
        return SynthNode(vibratoFreq, phaseOffset, vibrato, detune, freqOverride, adsr, mix)
    }

    override fun buildAudio(song: Song, noteFilter: NoteFilter, random: Random): AudioNode {
        val factory = { buildNode(random) }
        val instrumentPlayer =
            InstrumentPlayer(factory, random, song, noteFilter, 36, InstrumentSettings(adsr.release, 0.0))
        return instrumentPlayer
    }

    class SynthNode(
        private val vibratoFreq: Double,
        private val phaseOffset: Double,
        private val vibrato: Double,
        private val detune: Double,
        private val freqOverride: Double?,
        private val adsr: ADSR,
        private val mix: Map<WaveType, WaveData>
    ) : AudioNode(4, 2) {
        private val osc = buildOscillator()
        private val vibratoNode = vibrato(vibrato, vibratoFreq)
        private val adsrNode = ADSRNode(adsr.attack, adsr.decay, adsr.sustain, adsr.release)

        private fun buildOscillator(): AudioNode {
            var node: AudioNode? = null
            for ((waveType, waveData) in mix) {
                val phase = waveData.phase + phaseOffset
                val wave = when (waveType) {
                    WaveType.SINE -> OscillatorNode.sine(initialPhase = phase)
                    WaveType.SQUARE -> OscillatorNode.square(initialPhase = phase)
                    WaveType.SAWTOOTH -> OscillatorNode.saw(initialPhase = phase)
                    WaveType.TRIANGLE -> OscillatorNode.triangle(initialPhase = phase)
                    WaveType.SOFT_SQUARE -> OscillatorNode.softSquare(initialPhase = phase)
                    WaveType.SOFT_SAWTOOTH -> OscillatorNode.softSaw(initialPhase = phase)
                    WaveType.NOISE -> CustomNode.sink(1) stack NoiseNode()
                }
                node = if (node == null) {
                    wave * waveData.amplitude
                } else {
                    node bus (wave * waveData.amplitude)
                }
            }
            return node!!
        }

        override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
            val (freq, noteOn, velocity, randVal) = inputs
            val actualFreq = freqOverride ?: freq
            val detuneAmount = 1.0 + detune * (randVal * 2 - 1)
            val modFreq = vibratoNode.process(ctx, doubleArrayOf(actualFreq))[0] * detuneAmount
            val adsrVolume = adsrNode.process(ctx, doubleArrayOf(noteOn))[0]
            val output = osc.process(ctx, doubleArrayOf(modFreq, 1.0, velocity))[0] * velocity * adsrVolume

            return doubleArrayOf(output, output)
        }

        override fun clone(): AudioNode {
            return SynthNode(vibratoFreq, phaseOffset, vibrato, detune, freqOverride, adsr, mix)
        }

        override fun init(ctx: Context) {
            osc.init(ctx)
        }

        override fun reset() {
            osc.reset()
        }
    }
}