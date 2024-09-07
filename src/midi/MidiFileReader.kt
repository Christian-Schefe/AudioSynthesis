package midi

import util.ByteReader
import util.Endianness
import java.io.FileInputStream

class MidiFileReader {
    private var runningStatus: UByte? = null

    fun readFromFile(filePath: String): MidiFileData {
        val inputStream = FileInputStream(filePath)
        val reader = ByteReader(Endianness.BIG, inputStream.readAllBytes())
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
        println("Format: $format, Num Tracks: $numTracks, Division: $division")
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
        require(id == "MTrk") { "Invalid track chunk ID: $id at ${reader.position()}" }
        val size = reader.readUInt().toInt()
        val subReader = reader.subReader(size)
        val events = mutableListOf<TrackEvent>()
        println("Track size: $size")
        while (subReader.bytesLeft() > 0) {
            events.add(readTrackEvent(subReader))
        }
        return TrackChunk(events)
    }

    private fun readTrackEvent(reader: ByteReader): TrackEvent {
        val deltaTime = reader.readVarUInt()
        var statusByte = reader.peekUByte()

        if (statusByte.toInt() and 0x80 == 0) {
            // Running status
            statusByte = runningStatus ?: throw IllegalArgumentException("Invalid running status")
        } else {
            runningStatus = statusByte
            reader.skipBytes(1)
        }

        val channel = statusByte and 0x0Fu

        val midiEvent = when (statusByte.toInt() and 0xF0) {
            0x80 -> NoteOffEvent(channel, reader.readUByte(), reader.readUByte())
            0x90 -> NoteOnEvent(channel, reader.readUByte(), reader.readUByte())
            0xA0 -> PolyphonicKeyPressureEvent(channel, reader.readUByte(), reader.readUByte())
            0xB0 -> ControlChangeEvent(channel, reader.readUByte(), reader.readUByte())
            0xC0 -> ProgramChangeEvent(channel, reader.readUByte())
            0xD0 -> ChannelPressureEvent(channel, reader.readUByte())
            0xE0 -> PitchBendEvent(channel, PitchBendEvent.combineValues(reader.readUByte(), reader.readUByte()))
            else -> readMetaEvent(reader)
        }
        println("Delta time: $deltaTime, status byte: $statusByte -> $midiEvent")
        return TrackEvent(deltaTime, midiEvent)
    }

    private fun readMetaEvent(reader: ByteReader): MidiEvent {
        val type = reader.readUByte()
        val length = reader.readVarUInt()
        println("type: $type, length: $length")
        val data = reader.readBytes(length.toInt())

        return when (type.toInt()) {
            0x51 -> SetTempoEvent.fromData(data)
            else -> UnknownMetaEvent(data)
        }
    }
}