package midi.abstraction

import midi.raw.*
import util.OldBitConverter
import util.Endianness

abstract class MidiEvent {
    abstract fun toRawEvent(deltaTime: Int): RawMessage
}

data class NoteOnEvent(
    val channel: Int, val key: Int, val velocity: Double
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val vel = (velocity * 127).toInt()
        return RawChannelVoiceMessage(
            deltaTime, ChannelMessageStatus.NOTE_ON, channel.toByte(), byteArrayOf(key.toByte(), vel.toByte())
        )
    }
}

data class NoteOffEvent(
    val channel: Int, val key: Int
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        return RawChannelVoiceMessage(
            deltaTime, ChannelMessageStatus.NOTE_OFF, channel.toByte(), byteArrayOf(key.toByte(), 0)
        )
    }
}

data class TempoChangeEvent(
    val bpm: Double
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val microsecondsPerQuarterNote = (60_000_000 / bpm).toInt()
        return RawMetaEvent(
            deltaTime, MetaEventStatus.SET_TEMPO, OldBitConverter.intToBytes(microsecondsPerQuarterNote, Endianness.BIG, 3)
        )
    }
}