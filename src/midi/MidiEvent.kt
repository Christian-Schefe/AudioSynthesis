package midi

import util.BitConverter
import util.ByteReader
import util.ByteWriter

abstract class MidiEvent {
    abstract val size: UInt
    abstract fun write(byteWriter: ByteWriter)
}

data class NoteOffEvent(
    private val channel: UByte, private val key: UByte, private val velocity: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0x80u or channel.toUInt()).toUByte())
        byteWriter.addByte(key)
        byteWriter.addByte(velocity)
    }
}

data class NoteOnEvent(
    private val channel: UByte, private val key: UByte, private val velocity: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0x90u or channel.toUInt()).toUByte())
        byteWriter.addByte(key)
        byteWriter.addByte(velocity)
    }
}

data class PolyphonicKeyPressureEvent(
    private val channel: UByte, private val key: UByte, private val pressure: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0xA0u or channel.toUInt()).toUByte())
        byteWriter.addByte(key)
        byteWriter.addByte(pressure)
    }
}

data class ControlChangeEvent(
    private val channel: UByte, private val controller: UByte, private val value: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0xB0u or channel.toUInt()).toUByte())
        byteWriter.addByte(controller)
        byteWriter.addByte(value)
    }
}

data class ProgramChangeEvent(
    private val channel: UByte, private val program: UByte
) : MidiEvent() {
    override val size: UInt = 2u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0xC0u or channel.toUInt()).toUByte())
        byteWriter.addByte(program)
    }
}

data class ChannelPressureEvent(
    private val channel: UByte, private val pressure: UByte
) : MidiEvent() {
    override val size: UInt = 2u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0xD0u or channel.toUInt()).toUByte())
        byteWriter.addByte(pressure)
    }
}

data class PitchBendEvent(
    private val channel: UByte, private val value: UShort
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0xE0u or channel.toUInt()).toUByte())
        byteWriter.addByte(value.toUByte() and 0x7Fu)
        byteWriter.addByte((value.toUInt() shr 7).toUByte() and 0x7Fu)
    }

    companion object {
        fun combineValues(lsb: UByte, msb: UByte): UShort {
            return (msb.toUInt() shl 7 or lsb.toUInt()).toUShort()
        }
    }
}

data class SetTempoEvent(
    private val microsecondsPerQuarterNote: UInt
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte(0xFFu)
        byteWriter.addByte(0x51u)
        byteWriter.addByte(3u)
        byteWriter.addInt(microsecondsPerQuarterNote, 3)
    }

    companion object {
        fun fromData(data: ByteArray): SetTempoEvent {
            require(data.size == 3) { "Invalid tempo data size" }
            return SetTempoEvent(BitConverter.bitsToInt(data.reversedArray() + byteArrayOf(0)))
        }
    }
}

data class UnknownMetaEvent(
    private val data: ByteArray
) : MidiEvent() {
    override val size: UInt = data.size.toUInt()

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addBytes(data)
    }
}