package wav

import util.ByteArrayBuilder
import util.ByteReader
import util.Endianness
import java.io.FileOutputStream

class WavFileData(val riffChunk: RiffChunk) {
    constructor(fmtChunk: FmtChunk, audioData: ByteArray, createFactChunk: Boolean = false) : this(
        RiffChunk(
            listOfNotNull(
                fmtChunk,
                if (createFactChunk) FactChunk(audioData.size / fmtChunk.blockAlign) else null,
                DataChunk(audioData)
            )
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
    var type: ChunkType, var size: Int
) {
    open fun write(byteArrayBuilder: ByteArrayBuilder) {
        byteArrayBuilder.addString(type.id)
        byteArrayBuilder.addInt(size)
    }
}

enum class ChunkType(val id: String) {
    FMT("fmt "), FACT("fact"), DATA("data"), RIFF("RIFF");

    init {
        require(id.length == 4) {
            "Chunk ID must be 4 characters long"
        }
    }
}

class RiffChunk(
    private val chunks: Map<ChunkType, Chunk>
) : Chunk(ChunkType.RIFF, 4 + chunks.values.sumOf { it.size }) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addString("WAVE")
        chunks.forEach { id, chunk -> chunk.write(byteArrayBuilder) }
    }

    fun getChunk(id: ChunkType): Any {
        return chunks[id] ?: throw IllegalArgumentException("Chunk not found")
    }

    constructor(chunks: List<Chunk>) : this(chunks.associateBy { it.type })
}

open class FmtChunk(
    val formatType: AudioFormat,
    val numChannels: Short,
    val sampleRate: Int,
    val byteRate: Int,
    val blockAlign: Short,
    val bitsPerSample: Short,
    val fmtChunkExtension: FmtChunkExtension? = null
) : Chunk(ChunkType.FMT, 16 + (fmtChunkExtension?.size() ?: 0)) {
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
) : Chunk(ChunkType.FACT, 4) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addInt(sampleLength)
    }
}

class DataChunk(
    val audioData: ByteArray
) : Chunk(ChunkType.DATA, audioData.size) {
    override fun write(byteArrayBuilder: ByteArrayBuilder) {
        super.write(byteArrayBuilder)
        byteArrayBuilder.addBytes(audioData)
        if (audioData.size % 2 != 0) {
            byteArrayBuilder.addByte(0)
        }
    }
}

enum class AudioFormat(
    val code: Short,
    val bytesPerSample: Int = 0,
    val writeSample: (Double, ByteArrayBuilder) -> Unit,
    val readSample: (ByteReader) -> Double
) {
    PCM(1, 2, { sample, byteArrayBuilder ->
        byteArrayBuilder.addShort((sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt())
    }, { reader -> reader.readShort().toDouble() / Short.MAX_VALUE }),
    IEEE_FLOAT(3,
        4,
        { sample, byteArrayBuilder -> byteArrayBuilder.addFloat(sample.toFloat()) },
        { reader -> reader.readFloat().toDouble() });

    companion object {
        fun fromCode(code: Short): AudioFormat {
            return entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown audio format")
        }
    }
}