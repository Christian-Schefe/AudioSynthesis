package midi.raw

import util.bytes.ByteWriter

open class RawChunk(
    private var type: RawChunkType
) {
    open fun write(byteWriter: ByteWriter) {
        byteWriter.addString(type.id)
    }
}

class RawHeaderChunk(
    val format: MidiFileFormat, private val numTracks: Short, val division: Division
) : RawChunk(RawChunkType.MThd) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addInt(6)
        byteWriter.addShort(format.code)
        byteWriter.addShort(numTracks)
        division.write(byteWriter)
    }

    override fun toString(): String {
        return "RawHeaderChunk(format=$format, numTracks=$numTracks, division=$division)"
    }
}

class RawTrackChunk(
    val events: List<RawMessage>
) : RawChunk(RawChunkType.MTrk) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        val subWriter = ByteWriter(byteWriter.endianness)
        var runningStatusByte = 0.toByte()
        events.forEach {
            when (it) {
                is RawChannelVoiceMessage -> runningStatusByte = it.writeRunningStatus(subWriter, runningStatusByte)
                else -> {
                    runningStatusByte = 0
                    it.write(subWriter)
                }
            }
        }
        val data = subWriter.toByteArray()
        byteWriter.addInt(data.size)
        byteWriter.addBytes(data)
    }

    override fun toString(): String {
        return "RawTrackChunk(events=$events)"
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
    abstract fun getTickRate(): Int
    abstract fun calculateBPM(tempo: Int): Double

    data class TicksPerQuarterNote(private val ticks: Short) : Division() {
        override fun write(byteWriter: ByteWriter) {
            byteWriter.addShort((ticks.toInt() and 0x7FFF).toShort())
        }

        override fun getTickRate(): Int {
            return ticks.toInt()
        }

        override fun calculateBPM(tempo: Int): Double {
            return 60_000_000.0 / (tempo * ticks)
        }
    }

    data class SMPTE(private val framesPerSecond: Byte, private val ticksPerFrame: Byte) : Division() {
        override fun write(byteWriter: ByteWriter) {
            byteWriter.addByte((framesPerSecond.toInt() and 0x7F).toByte())
            byteWriter.addByte(ticksPerFrame)
        }

        override fun getTickRate(): Int {
            throw NotImplementedError("SMPTE is not supported")
        }

        override fun calculateBPM(tempo: Int): Double {
            throw NotImplementedError("SMPTE is not supported")
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