package midi.raw

import util.ByteWriter
import util.Endianness
import java.io.FileOutputStream

class RawMidi(val headerChunk: RawHeaderChunk, val tracks: List<RawTrackChunk>) {
    init {
        when (headerChunk.format) {
            MidiFileFormat.SINGLE_TRACK -> require(tracks.size == 1) {
                "Single track format must have exactly one track"
            }

            MidiFileFormat.MULTIPLE_TRACKS -> require(tracks.isNotEmpty()) {
                "Multiple tracks format must have at least one track"
            }

            MidiFileFormat.MULTIPLE_SONGS -> require(tracks.isNotEmpty()) {
                "Multiple songs format must have least one track"
            }
        }
    }

    constructor(format: MidiFileFormat, division: Division, tracks: List<RawTrackChunk>) : this(
        RawHeaderChunk(format, tracks.size.toShort(), division), tracks
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
        fun readFromFile(filePath: String): RawMidi {
            val reader = RawMidiReader()
            val midiFileData = reader.readFromFile(filePath)
            return midiFileData
        }
    }
}