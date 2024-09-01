package wav

import java.io.FileOutputStream

class WavFileData(fmtChunk: FmtChunk, audioData: ByteArray, createFactChunk: Boolean = false) {
    private val riffChunk = RiffChunk(
        listOfNotNull(
            fmtChunk,
            if (createFactChunk) FactChunk(audioData.size / fmtChunk.blockAlign) else null,
            DataChunk(audioData)
        )
    )

    fun writeToFile(filePath: String) {
        val byteArrayBuilder = ByteArrayBuilder(Endianness.LITTLE)

        riffChunk.write(byteArrayBuilder)

        val fileOutputStream = FileOutputStream(filePath)
        fileOutputStream.write(byteArrayBuilder.toByteArray())
        fileOutputStream.close()
    }
}

open class Chunk(
    private val id: String, val size: Int
) {
    open fun write(byteArrayBuilder: ByteArrayBuilder) {
        byteArrayBuilder.addString(id)
        byteArrayBuilder.addInt(size)
    }
}

class RiffChunk(
    private val chunks: List<Chunk>
) : Chunk("RIFF", 4 + chunks.sumOf { it.size }) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addString("WAVE")
        chunks.forEach { it.write(byteArrayBuilder) }
    }
}

open class FmtChunk(
    val formatType: AudioFormat,
    val numChannels: Short,
    val sampleRate: Int,
    val byteRate: Int,
    val blockAlign: Short,
    val bitsPerSample: Short,
    val fmtChunkExtension: FmtChunkExtension? = null
) : Chunk("fmt ", 16 + (fmtChunkExtension?.size() ?: 0)) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addShort(formatType.code)
        byteArrayBuilder.addShort(numChannels)
        byteArrayBuilder.addInt(sampleRate)
        byteArrayBuilder.addInt(byteRate)
        byteArrayBuilder.addShort(blockAlign)
        byteArrayBuilder.addShort(bitsPerSample)
        fmtChunkExtension?.write(byteArrayBuilder)
    }
}

class FmtChunkExtension(
    private val extensionSize: Short,
    private val validBitsPerSample: Short,
    private val channelMask: Int,
    private val subFormat: ByteArray
) {
    init {
        if (extensionSize > 0 && subFormat.size != 16) {
            throw IllegalArgumentException("SubFormat size must be 16")
        }
    }

    fun write(byteArrayBuilder: ByteArrayBuilder) {
        byteArrayBuilder.addShort(extensionSize)
        if (extensionSize > 0) {
            byteArrayBuilder.addShort(validBitsPerSample)
            byteArrayBuilder.addInt(channelMask)
            byteArrayBuilder.addBytes(subFormat)
        }
    }

    fun size() = if (extensionSize > 0) 24 else 2

    constructor() : this(0, 0, 0, byteArrayOf())
}

class FactChunk(
    private val sampleLength: Int
) : Chunk("fact", 4) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addInt(sampleLength)
    }
}

class DataChunk(
    private val audioData: ByteArray
) : Chunk("data", audioData.size) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addBytes(audioData)
        if (audioData.size % 2 != 0) {
            byteArrayBuilder.addByte(0)
        }
    }
}

enum class AudioFormat(val code: Short) {
    PCM(1),
    IEEE_FLOAT(3)
}