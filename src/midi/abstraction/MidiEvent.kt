package midi.abstraction

abstract class MidiEvent {
    abstract fun toRawEvent(): midi.raw.MidiEvent
}

data class NoteOnEvent(
    val channel: Int, val key: Int, val velocity: Double
) : MidiEvent() {
    override fun toRawEvent(): midi.raw.MidiEvent {
        val vel = (velocity * 127).toInt()
        return midi.raw.NoteOnEvent(channel.toUByte(), key.toUByte(), vel.toUByte())
    }
}

data class NoteOffEvent(
    val channel: Int, val key: Int
) : MidiEvent() {
    override fun toRawEvent(): midi.raw.MidiEvent {
        return midi.raw.NoteOffEvent(channel.toUByte(), key.toUByte(), 0u)
    }
}

data class TempoChangeEvent(
    val bpm: Double
) : MidiEvent() {
    override fun toRawEvent(): midi.raw.MidiEvent {
        val microsecsPerQuarterNote = (60_000_000 / bpm).toUInt()
        return midi.raw.SetTempoEvent(microsecsPerQuarterNote)
    }
}