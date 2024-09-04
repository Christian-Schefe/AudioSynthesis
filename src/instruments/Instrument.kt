package instruments

import nodes.SequencerNode
import nodes.*
import kotlin.math.pow

open class Instrument(val nodeFactory: () -> AudioNode) : AudioNode(0, 2) {
    private var sequencer = SequencerNode(outputCount)

    fun addNote(note: Int, time: Double, duration: Double, velocity: Double) {
        val node = nodeFactory()
        val instance = adsrNote(node, midiNoteToFreq(note), duration) * (ConstantNode(velocity) repeat 2)
        sequencer.addNode(instance, time)
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return sequencer.process(ctx, inputs)
    }

    override fun reset() {
        sequencer.reset()
    }

    override fun clone(): AudioNode {
        val cloned = Instrument(nodeFactory)
        cloned.sequencer = sequencer.clone() as SequencerNode
        return cloned
    }

    override fun init(ctx: Context) {
        sequencer.init(ctx)
    }

    companion object {
        fun adsrVolume(
            node: AudioNode, attack: Double, decay: Double, sustain: Double, release: Double
        ): AudioNode {
            val adsr = ADSRNode(attack, decay, sustain, release)
            return node * adsr
        }

        fun adsrNote(
            adsrNote: AudioNode, freq: Double, hold: Double
        ): AudioNode {
            return freq stack Envelope.hold(hold) pipe adsrNote
        }

        fun midiNoteToFreq(note: Int): Double {
            return 440.0 * 2.0.pow((note - 69.0) / 12.0)
        }
    }
}