package wav

import util.ByteReader
import util.Endianness
import java.io.FileInputStream


class WavFileReader {
    fun readWavFile(filePath: String): WavFileData {
        val inputStream = FileInputStream(filePath)
        val reader = ByteReader(Endianness.LITTLE, inputStream.readAllBytes())
        val riffChunk = readRiffChunk(reader)
        return WavFileData(riffChunk)
    }

    fun readRiffChunk(reader: ByteReader): RiffChunk {
        val id = reader.readString(4)
        val size = reader.readInt()
        //require(size == reader.bytesLeft()) { "Invalid RIFF chunk size: $size - ${reader.bytesLeft()}" }
        val format = reader.readString(4)
        require(id == "RIFF" && format == "WAVE") { "Invalid RIFF chunk" }
        val chunks = mutableListOf<Chunk>()
        while (reader.bytesLeft() > 0) {
            chunks.add(
                when (reader.readString(4)) {
                    "fmt " -> readFmtChunk(reader)
                    "fact" -> readFactChunk(reader)
                    "data" -> readDataChunk(reader)
                    else -> throw IllegalArgumentException("Unknown chunk type")
                }
            )
        }
        require(chunks.any { it is DataChunk }) { "Missing data chunk" }
        require(chunks.any { it is FmtChunk }) { "Missing fmt chunk" }
        return RiffChunk(chunks)
    }

    fun readFmtChunk(reader: ByteReader): FmtChunk {
        val size = reader.readInt()
        require(size == 16 || size == 18 || size == 40) { "Invalid fmt chunk size" }
        val formatType = AudioFormat.fromCode(reader.readShort())
        val numChannels = reader.readShort()
        val sampleRate = reader.readInt()
        val byteRate = reader.readInt()
        val blockAlign = reader.readShort()
        val bitsPerSample = reader.readShort()
        val fmtChunkExtension = if (size > 16) {
            val extensionSize = reader.readShort()
            require(extensionSize.toInt() == size - 18) { "Invalid fmt chunk extension size $extensionSize ${size - 18}" }
            if (extensionSize > 0) {
                val validBitsPerSample = reader.readShort()
                val channelMask = reader.readInt()
                val subFormat = reader.readBytes(16)
                FmtChunkExtension(extensionSize, validBitsPerSample, channelMask, subFormat)
            } else {
                FmtChunkExtension()
            }
        } else {
            null
        }
        return FmtChunk(formatType, numChannels, sampleRate, byteRate, blockAlign, bitsPerSample, fmtChunkExtension)
    }

    fun readFactChunk(reader: ByteReader): FactChunk {
        val size = reader.readInt()
        require(size == 4) { "Invalid fact chunk size" }
        val sampleLength = reader.readInt()
        return FactChunk(sampleLength)
    }

    fun readDataChunk(reader: ByteReader): DataChunk {
        val size = reader.readInt()
        return DataChunk(reader.readBytes(size))
    }
}