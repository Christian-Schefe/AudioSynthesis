package nodes

class InstrumentNode(val nodeFactory: () -> AudioNode, outputCount: Int) : AudioNode(0, outputCount) {
    private var sequencer = SequencerNode(outputCount)

    fun addNote(note: Int, time: Double, duration: Double) {
        val node = nodeFactory()
        val instance = adsrNote(node, midiNoteToFreq(note), duration)
        sequencer.addNode(instance, time)
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return sequencer.process(ctx, inputs)
    }

    override fun reset() {
        sequencer.reset()
    }

    override fun clone(): AudioNode {
        val cloned = InstrumentNode(nodeFactory, outputCount)
        cloned.sequencer = sequencer.clone() as SequencerNode
        return cloned
    }
}