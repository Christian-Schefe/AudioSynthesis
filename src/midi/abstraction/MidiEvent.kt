package midi.abstraction

import midi.raw.*
import util.bytes.ByteConverter
import util.bytes.Endianness

abstract class MidiEvent {
    abstract fun toRawEvent(deltaTime: Int): RawMessage

    companion object {
        fun fromRawChannelVoiceMessage(message: RawChannelVoiceMessage): MidiEvent? {
            return when (message.status) {
                ChannelMessageStatus.NOTE_ON -> {
                    val key = message.data[0].toInt()
                    val velocity = message.data[1].toInt() / 127.0
                    NoteOnEvent(message.channel.toInt(), key, velocity)
                }

                ChannelMessageStatus.NOTE_OFF -> {
                    val key = message.data[0].toInt()
                    NoteOffEvent(message.channel.toInt(), key)
                }

                ChannelMessageStatus.PROGRAM_CHANGE -> {
                    val program = message.data[0].toInt()
                    ProgramChangeEvent(message.channel.toInt(), program)
                }

                else -> null
            }
        }

        fun fromRawMetaEvent(message: RawMetaEvent): MidiEvent? {
            return when (message.type) {
                MetaEventStatus.SET_TEMPO -> {
                    val bpm = 60_000_000.0 / ByteConverter.bytesToInt(message.data, Endianness.BIG)
                    TempoChangeEvent(bpm)
                }

                MetaEventStatus.SEQUENCE_TRACK_NAME -> {
                    val name = message.data.decodeToString()
                    TrackNameEvent(name)
                }

                MetaEventStatus.INSTRUMENT_NAME -> {
                    val name = message.data.decodeToString()
                    InstrumentNameEvent(name)
                }

                MetaEventStatus.TEXT_EVENT -> {
                    val text = message.data.decodeToString()
                    TextEvent(text)
                }

                else -> null
            }
        }

        fun fromRawMessage(message: RawMessage): MidiEvent? {
            return when (message) {
                is RawChannelVoiceMessage -> fromRawChannelVoiceMessage(message)
                is RawMetaEvent -> fromRawMetaEvent(message)
                else -> null
            }
        }
    }
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

    override fun toString(): String {
        return "NoteOnEvent(channel=$channel, key=$key, velocity=$velocity)"
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

    override fun toString(): String {
        return "NoteOffEvent(channel=$channel, key=$key)"
    }
}

data class ProgramChangeEvent(
    val channel: Int, val program: Int
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        return RawChannelVoiceMessage(
            deltaTime, ChannelMessageStatus.PROGRAM_CHANGE, channel.toByte(), byteArrayOf(program.toByte())
        )
    }

    override fun toString(): String {
        return "ProgramChangeEvent(channel=$channel, program=$program)"
    }
}

data class TempoChangeEvent(
    val bpm: Double
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val microsecondsPerQuarterNote = (60_000_000 / bpm).toInt()
        return RawMetaEvent(
            deltaTime,
            MetaEventStatus.SET_TEMPO,
            ByteConverter.intToBytes(microsecondsPerQuarterNote, Endianness.BIG, 3)
        )
    }

    override fun toString(): String {
        return "TempoChangeEvent(bpm=$bpm)"
    }
}

data class TrackNameEvent(
    val name: String
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val data = name.encodeToByteArray()
        return RawMetaEvent(deltaTime, MetaEventStatus.SEQUENCE_TRACK_NAME, data)
    }

    override fun toString(): String {
        return "TrackNameEvent(name='$name')"
    }
}

data class InstrumentNameEvent(
    val name: String
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val data = name.encodeToByteArray()
        return RawMetaEvent(deltaTime, MetaEventStatus.INSTRUMENT_NAME, data)
    }

    override fun toString(): String {
        return "InstrumentNameEvent(name='$name')"
    }
}

data class TextEvent(
    val text: String
) : MidiEvent() {
    override fun toRawEvent(deltaTime: Int): RawMessage {
        val data = text.encodeToByteArray()
        return RawMetaEvent(deltaTime, MetaEventStatus.TEXT_EVENT, data)
    }

    override fun toString(): String {
        return "TextEvent(text='$text')"
    }
}