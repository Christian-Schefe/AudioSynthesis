package midi

import util.BitConverter
import util.ByteReader
import util.ByteWriter

abstract class MidiEvent {
    abstract val size: UInt
    abstract fun write(byteWriter: ByteWriter)
}

class NoteOffEvent(
    private val channel: UByte, private val key: UByte, private val velocity: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0x80u or channel.toUInt()).toUByte())
        byteWriter.addByte(key)
        byteWriter.addByte(velocity)
    }

    constructor(reader: ByteReader) : this(
        reader.readUByte() and 0x0Fu, reader.readUByte(), reader.readUByte()
    )
}

class NoteOnEvent(
    private val channel: UByte, private val key: UByte, private val velocity: UByte
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte((0x90u or channel.toUInt()).toUByte())
        byteWriter.addByte(key)
        byteWriter.addByte(velocity)
    }

    constructor(reader: ByteReader) : this(
        reader.readUByte() and 0x0Fu, reader.readUByte(), reader.readUByte()
    )
}

class SetTempoEvent(
    private val microsecondsPerQuarterNote: UInt
) : MidiEvent() {
    override val size: UInt = 3u

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte(0xFFu)
        byteWriter.addByte(0x51u)
        byteWriter.addByte(3u)
        byteWriter.addInt(microsecondsPerQuarterNote)
    }

    companion object {
        fun fromData(data: ByteArray): SetTempoEvent {
            require(data.size == 3) { "Invalid tempo data size" }
            return SetTempoEvent(BitConverter.bitsToInt(data + byteArrayOf(0)))
        }
    }
}

class UnknownEvent(
    private val data: ByteArray
) : MidiEvent() {
    override val size: UInt = data.size.toUInt()

    override fun write(byteWriter: ByteWriter) {
        byteWriter.addBytes(data)
    }

    companion object {
        fun read(reader: ByteReader): UnknownEvent {
            val size = reader.readVarInt()
            return UnknownEvent(reader.readBytes(size))
        }
    }
}