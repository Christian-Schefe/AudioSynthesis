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

    private fun readRiffChunk(reader: ByteReader): RiffChunk {
        val id = reader.readString(4)
        val size = reader.readUInt().toInt()
        require(size == reader.bytesLeft()) { "Invalid RIFF chunk size: $size - ${reader.bytesLeft()}" }
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

    private fun readFmtChunk(reader: ByteReader): FmtChunk {
        val size = reader.readUInt().toInt()
        require(size == 16 || size == 18 || size == 40) { "Invalid fmt chunk size" }
        val formatType = AudioFormat.fromCode(reader.readUShort())
        val numChannels = reader.readUShort()
        val sampleRate = reader.readUInt()
        val byteRate = reader.readUInt()
        val blockAlign = reader.readUShort()
        val bitsPerSample = reader.readUShort()
        val fmtChunkExtension = if (size > 16) {
            val extensionSize = reader.readUShort()
            require(extensionSize.toInt() == size - 18) { "Invalid fmt chunk extension size $extensionSize ${size - 18}" }
            if (extensionSize > 0u) {
                val validBitsPerSample = reader.readUShort()
                val channelMask = reader.readUInt()
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

    private fun readFactChunk(reader: ByteReader): FactChunk {
        val size = reader.readUInt().toInt()
        require(size == 4) { "Invalid fact chunk size" }
        val sampleLength = reader.readUInt()
        return FactChunk(sampleLength)
    }

    private fun readDataChunk(reader: ByteReader): DataChunk {
        val size = reader.readUInt().toInt()
        val data = reader.readBytes(size)
        if (size % 2 != 0) {
            reader.readUByte()
        }
        return DataChunk(data)
    }
}