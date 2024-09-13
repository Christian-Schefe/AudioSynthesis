package midi.raw

import util.ByteReader
import util.Endianness
import java.io.FileInputStream

class RawMidiReader {
    private var runningStatus: Int? = null

    fun readFromFile(filePath: String): RawMidi {
        val inputStream = FileInputStream(filePath)
        val reader = ByteReader(Endianness.BIG, inputStream.readAllBytes())
        val headerChunk = readHeaderChunk(reader)
        val tracks = mutableListOf<RawTrackChunk>()
        while (reader.bytesLeft() > 0) {
            tracks.add(readTrackChunk(reader))
        }
        return RawMidi(headerChunk, tracks)
    }

    private fun readHeaderChunk(reader: ByteReader): RawHeaderChunk {
        val id = reader.readString(4)
        require(id == "MThd") { "Invalid header chunk ID: $id" }
        val size = reader.readInt()
        require(size == 6) { "Invalid header chunk size: $size" }
        val format = reader.readShort()
        val numTracks = reader.readShort()
        val division = readDivision(reader)
        return RawHeaderChunk(MidiFileFormat.fromCode(format), numTracks, division)
    }

    private fun readDivision(reader: ByteReader): Division {
        val division = reader.readShort().toInt()
        val divisionType = division and 0x8000
        val divisionValue = division and 0x7FFF
        return if (divisionType == 0) {
            Division.TicksPerQuarterNote(divisionValue.toShort())
        } else {
            val framesPerSecond = divisionValue shr 8
            val ticksPerFrame = divisionValue and 0xFF
            Division.SMPTE(framesPerSecond.toByte(), ticksPerFrame.toByte())
        }
    }

    private fun readTrackChunk(reader: ByteReader): RawTrackChunk {
        val id = reader.readString(4)
        require(id == "MTrk") { "Invalid track chunk ID: $id at ${reader.position()}" }
        val size = reader.readInt()
        val subReader = reader.subReader(size)
        val events = mutableListOf<RawMessage>()
        while (subReader.bytesLeft() > 0) {
            events.add(readTrackEvent(subReader))
        }
        return RawTrackChunk(events)
    }

    private fun readTrackEvent(reader: ByteReader): RawMessage {
        val deltaTime = reader.readVarInt()
        var statusByte = reader.peekByte().toInt() and 0xFF

        if (statusByte and 0x80 == 0) {
            statusByte = runningStatus ?: throw IllegalArgumentException("Invalid running status")
        } else {
            runningStatus = statusByte
            reader.skipBytes(1)
        }

        if (statusByte == 0xFF) {
            val type = MetaEventStatus.fromByte(reader.readByte())
            val length = reader.readVarInt()
            val data = reader.readBytes(length)
            return RawMetaEvent(deltaTime, type, data)
        } else if (statusByte and 0xF0 != 0xF0) {
            val channel = statusByte and 0x0F
            val status = ChannelMessageStatus.fromByte(statusByte.toByte())
            val size = status.dataSize
            val data = reader.readBytes(size)
            return RawChannelVoiceMessage(deltaTime, status, channel.toByte(), data)
        } else {
            val status = SystemMessageStatus.fromByte(statusByte.toByte())
            val size = status.dataSize
            if (size != null) {
                val data = reader.readBytes(size)
                return RawSystemMessage(deltaTime, status, data)
            } else {
                val data = reader.readUntil(SystemMessageStatus.END_OF_EXCLUSIVE.byte, false)
                return RawSystemMessage(deltaTime, status, data)
            }
        }
    }
}