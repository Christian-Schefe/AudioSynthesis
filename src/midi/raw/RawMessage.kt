package midi.raw

import util.ByteConverter
import util.ByteWriter

open class RawMessage(val deltaTime: Int) {
    init {
        require(deltaTime >= 0) { "Delta time must be non-negative" }
    }

    open fun write(byteWriter: ByteWriter) {
        byteWriter.addVarInt(deltaTime)
    }
}

enum class ChannelMessageStatus(val intValue: Int, val dataSize: Int) {
    NOTE_OFF(0x80, 2), NOTE_ON(0x90, 2), POLYPHONIC_AFTERTOUCH(0xA0, 2), CONTROL_CHANGE(0xB0, 2), PROGRAM_CHANGE(
        0xC0, 1
    ),
    CHANNEL_PRESSURE(0xD0, 1), PITCH_BEND(0xE0, 2);

    fun getByte(channel: Byte): Byte {
        require(channel in 0..15) { "Channel must be in range 0..15" }
        return (intValue or channel.toInt()).toByte()
    }

    companion object {
        val codeMap = entries.associateBy(ChannelMessageStatus::intValue)

        fun fromByte(byte: Byte): ChannelMessageStatus {
            return codeMap[byte.toInt() and 0xF0] ?: error("Invalid channel message status byte")
        }
    }
}

class RawChannelVoiceMessage(deltaTime: Int, val status: ChannelMessageStatus, val channel: Byte, val data: ByteArray) :
    RawMessage(deltaTime) {

    init {
        require(data.size == status.dataSize) { "Data size must be ${status.dataSize}" }
        require(channel in 0..15) { "Channel must be in range 0..15" }
    }

    override fun toString(): String {
        return "RawChannelVoiceMessage(deltaTime=$deltaTime, status=$status, channel=$channel, data=${data.toList()})"
    }

    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addByte(status.getByte(channel))
        byteWriter.addBytes(data)
    }

    fun writeRunningStatus(byteWriter: ByteWriter, runningStatus: Byte): Byte {
        super.write(byteWriter)
        val statusByte = status.getByte(channel)
        if (runningStatus != statusByte) {
            byteWriter.addByte(statusByte)
        }
        byteWriter.addBytes(data)
        return statusByte
    }
}

enum class SystemMessageStatus(intValue: Int, val dataSize: Int?) {
    SYSTEM_EXCLUSIVE(0xF0, null), MIDI_TIME_CODE(0xF1, 1), SONG_POSITION_POINTER(0xF2, 2), SONG_SELECT(
        0xF3, 1
    ),
    TUNE_REQUEST(0xF6, 0), END_OF_EXCLUSIVE(0xF7, 0), TIMING_CLOCK(0xF8, 0), START(0xFA, 0), CONTINUE(0xFB, 0), STOP(
        0xFC, 0
    ),
    ACTIVE_SENSING(0xFE, 0), SYSTEM_RESET(0xFF, 0);

    val byte = intValue.toByte()

    companion object {
        val codeMap = entries.associateBy(SystemMessageStatus::byte)

        fun fromByte(byte: Byte): SystemMessageStatus {
            return codeMap[byte] ?: error("Invalid system message status byte")
        }
    }
}

class RawSystemMessage(deltaTime: Int, val status: SystemMessageStatus, val data: ByteArray) : RawMessage(deltaTime) {
    init {
        if (status.dataSize != null) {
            require(data.size == status.dataSize) { "Data size must be ${status.dataSize}" }
        }
    }

    override fun toString(): String {
        return "RawSystemMessage(deltaTime=$deltaTime, status=$status, data=${data.toList()})"
    }

    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addByte(status.byte)
        byteWriter.addBytes(data)
        if (status == SystemMessageStatus.SYSTEM_EXCLUSIVE) {
            byteWriter.addByte(SystemMessageStatus.END_OF_EXCLUSIVE.byte)
        }
    }
}

enum class MetaEventStatus(intValue: Int, val dataSize: Int?) {
    SEQUENCE_NUMBER(0x00, 2), TEXT_EVENT(0x01, null), COPYRIGHT_NOTICE(0x02, null), SEQUENCE_TRACK_NAME(
        0x03, null
    ),
    INSTRUMENT_NAME(0x04, null), LYRIC(0x05, null), MARKER(0x06, null), CUE_POINT(0x07, null), MIDI_CHANNEL_PREFIX(
        0x20, 1
    ),
    END_OF_TRACK(0x2F, 0), SET_TEMPO(0x51, 3), SMPTE_OFFSET(0x54, 5), TIME_SIGNATURE(0x58, 4), KEY_SIGNATURE(
        0x59, 2
    ),
    SEQUENCER_SPECIFIC(0x7F, null), UNKNOWN(0xFF, null);

    val byte = intValue.toByte()

    companion object {
        val codeMap = entries.associateBy(MetaEventStatus::byte)

        fun fromByte(byte: Byte): MetaEventStatus {
            return codeMap[byte] ?: UNKNOWN
        }
    }
}

class RawMetaEvent(deltaTime: Int, val type: MetaEventStatus, val data: ByteArray) : RawMessage(deltaTime) {
    init {
        if (type.dataSize != null) {
            require(data.size == type.dataSize) { "Data size must be ${type.dataSize}" }
        }
    }

    override fun toString(): String {
        return "RawMetaEvent(deltaTime=$deltaTime, type=$type, data=${data.toList()})"
    }

    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addByte(0xFF.toByte())
        byteWriter.addByte(type.byte)
        byteWriter.addVarInt(data.size)
        byteWriter.addBytes(data)
    }
}