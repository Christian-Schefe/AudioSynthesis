package midi.raw

import util.ByteWriter

open class RawChunk(
    private var type: RawChunkType, var size: Int
) {
    open fun write(byteWriter: ByteWriter) {
        byteWriter.addString(type.id)
        byteWriter.addInt(size)
    }
}

class RawHeaderChunk(
    val format: MidiFileFormat, private val numTracks: Short, val division: Division
) : RawChunk(RawChunkType.MThd, 6) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addShort(format.code)
        byteWriter.addShort(numTracks)
        division.write(byteWriter)
    }
}

class RawTrackChunk(
    val events: List<RawMessage>
) : RawChunk(RawChunkType.MTrk, events.sumOf { it.size }) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        events.forEach { it.write(byteWriter) }
    }
}

enum class MidiFileFormat(val code: Short) {
    SINGLE_TRACK(0), MULTIPLE_TRACKS(1), MULTIPLE_SONGS(2);

    companion object {
        val codeMap = entries.associateBy(MidiFileFormat::code)

        fun fromCode(format: Short): MidiFileFormat {
            return codeMap[format] ?: error("Invalid format code")
        }
    }
}

abstract class Division {
    abstract fun write(byteWriter: ByteWriter)

    data class TicksPerQuarterNote(private val ticks: Short) : Division() {
        override fun write(byteWriter: ByteWriter) {
            byteWriter.addShort((ticks.toInt() and 0x7FFF).toShort())
        }
    }

    data class SMPTE(private val framesPerSecond: Byte, private val ticksPerFrame: Byte) : Division() {
        override fun write(byteWriter: ByteWriter) {
            byteWriter.addByte((framesPerSecond.toInt() and 0x7F).toByte())
            byteWriter.addByte(ticksPerFrame)
        }
    }
}

enum class RawChunkType(val id: String) {
    MThd("MThd"), MTrk("MTrk");

    init {
        require(id.length == 4) {
            "Chunk ID must be 4 characters long"
        }
    }
}