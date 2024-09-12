package midi.raw

import util.BitConverter
import util.ByteWriter
import util.Endianness
import java.io.FileOutputStream

class MidiFile(val headerChunk: HeaderChunk, val tracks: List<TrackChunk>) {
    init {
        when (headerChunk.format) {
            FormatType.SINGLE_TRACK -> require(tracks.size == 1) {
                "Single track format must have exactly one track"
            }

            FormatType.MULTIPLE_TRACKS -> require(tracks.isNotEmpty()) {
                "Multiple tracks format must have at least one track"
            }

            FormatType.MULTIPLE_SONGS -> require(tracks.isNotEmpty()) {
                "Multiple songs format must have least one track"
            }
        }
    }

    constructor(format: FormatType, division: Division, tracks: List<TrackChunk>) : this(
        HeaderChunk(format, tracks.size.toUShort(), division), tracks
    )

    fun write(byteWriter: ByteWriter) {
        headerChunk.write(byteWriter)
        tracks.forEach { it.write(byteWriter) }
    }

    fun writeToFile(filePath: String) {
        val byteWriter = ByteWriter(Endianness.BIG)

        write(byteWriter)

        val fileOutputStream = FileOutputStream(filePath)
        fileOutputStream.write(byteWriter.toByteArray())
        fileOutputStream.close()
    }

    companion object {
        fun readFromFile(filePath: String): MidiFile {
            val reader = MidiFileReader()
            val midiFileData = reader.readFromFile(filePath)
            return midiFileData
        }
    }
}

open class Chunk(
    private var type: MidiChunkType, var size: UInt
) {
    open fun write(byteWriter: ByteWriter) {
        byteWriter.addString(type.id)
        byteWriter.addInt(size)
    }
}

class HeaderChunk(
    val format: FormatType, private val numTracks: UShort, val division: Division
) : Chunk(MidiChunkType.MThd, 6u) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addShort(format.code)
        byteWriter.addShort(numTracks)
        division.write(byteWriter)
    }
}

class TrackChunk(
    val events: List<TrackEvent>
) : Chunk(MidiChunkType.MTrk, events.sumOf { it.size }) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        events.forEach { it.write(byteWriter) }
    }
}

data class TrackEvent(
    val deltaTime: UInt, val event: MidiEvent
) {
    val size: UInt = BitConverter.intToVarInt(deltaTime).size.toUInt() + event.size

    fun write(byteWriter: ByteWriter) {
        byteWriter.addVarInt(deltaTime)
        event.write(byteWriter)
    }
}

enum class FormatType(val code: UShort) {
    SINGLE_TRACK(0u), MULTIPLE_TRACKS(1u), MULTIPLE_SONGS(2u);

    companion object {
        fun fromCode(format: UShort): FormatType {
            return entries.first { it.code == format }
        }
    }
}

abstract class Division {
    abstract fun write(byteWriter: ByteWriter)
}

data class TicksPerQuarterNote(private val ticks: UShort) : Division() {
    override fun write(byteWriter: ByteWriter) {
        byteWriter.addShort(ticks and 0x7FFFu)
    }
}

data class SMPTE(private val framesPerSecond: UByte, private val ticksPerFrame: UByte) : Division() {
    override fun write(byteWriter: ByteWriter) {
        byteWriter.addByte(framesPerSecond and 0x7Fu)
        byteWriter.addByte(ticksPerFrame)
    }
}

enum class MidiChunkType(val id: String) {
    MThd("MThd"), MTrk("MTrk");

    init {
        require(id.length == 4) {
            "Chunk ID must be 4 characters long"
        }
    }
}