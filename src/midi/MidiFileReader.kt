package midi

import util.ByteReader
import util.Endianness
import java.io.FileInputStream

class MidiFileReader {
    fun readFromFile(filePath: String): MidiFileData {
        val inputStream = FileInputStream(filePath)
        val reader = ByteReader(Endianness.LITTLE, inputStream.readAllBytes())
        val headerChunk = readHeaderChunk(reader)
        val tracks = mutableListOf<TrackChunk>()
        while (reader.bytesLeft() > 0) {
            tracks.add(readTrackChunk(reader))
        }
        return MidiFileData(headerChunk, tracks)
    }

    private fun readHeaderChunk(reader: ByteReader): HeaderChunk {
        val id = reader.readString(4)
        require(id == "MThd") { "Invalid header chunk ID: $id" }
        val size = reader.readUInt()
        require(size == 6u) { "Invalid header chunk size: $size" }
        val format = reader.readUShort()
        val numTracks = reader.readUShort()
        val division = readDivision(reader)
        return HeaderChunk(FormatType.fromCode(format), numTracks, division)
    }

    private fun readDivision(reader: ByteReader): Division {
        val division = reader.readUShort()
        val divisionType = division.toInt() and 0x8000
        val divisionValue = division.toInt() and 0x7FFF
        return if (divisionType == 0) {
            TicksPerQuarterNote(divisionValue.toUShort())
        } else {
            val framesPerSecond = divisionValue shr 8
            val ticksPerFrame = divisionValue and 0xFF
            SMPTE(framesPerSecond.toUByte(), ticksPerFrame.toUByte())
        }
    }

    private fun readTrackChunk(reader: ByteReader): TrackChunk {
        val id = reader.readString(4)
        require(id == "MTrk") { "Invalid track chunk ID: $id" }
        val size = reader.readUInt()
        val endBytesLeft = reader.bytesLeft() - size.toInt()
        val events = mutableListOf<TrackEvent>()
        while (reader.bytesLeft() > endBytesLeft) {
            events.add(readTrackEvent(reader))
        }
        return TrackChunk(events)
    }

    private fun readTrackEvent(reader: ByteReader): TrackEvent {
        val deltaTime = reader.readVarUInt()
        val statusByte = reader.readUByte()
        if (statusByte.toInt() == 0xFF) {
            return TrackEvent(deltaTime, readMetaEvent(reader))
        }
        val midiEvent = when (statusByte.toInt() and 0xFF) {
            0x80 -> NoteOffEvent(reader)
            0x90 -> NoteOnEvent(reader)
            0xFF -> readMetaEvent(reader)
            else -> UnknownEvent.read(reader)
        }
        return TrackEvent(deltaTime, midiEvent)
    }

    private fun readMetaEvent(reader: ByteReader): MidiEvent {
        val type = reader.readUByte()
        val length = reader.readVarUInt()
        val data = reader.readBytes(length.toInt())

        return when (type.toInt()) {
            0x51 -> SetTempoEvent.fromData(data)
            else -> UnknownEvent(data)
        }
    }
}