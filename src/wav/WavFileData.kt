package wav

import util.bytes.ByteWriter

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

    fun write(byteWriter: ByteWriter) {
        riffChunk.write(byteWriter)
    }
}

open class Chunk(
    var type: WavChunkType
) {
    open fun write(byteWriter: ByteWriter) {
        byteWriter.addString(type.id)
    }
}

enum class WavChunkType(val id: String) {
    FMT("fmt "), FACT("fact"), DATA("data"), RIFF("RIFF");

    init {
        require(id.length == 4) {
            "Chunk ID must be 4 characters long"
        }
    }
}

class RiffChunk(
    private val chunks: Map<WavChunkType, Chunk>
) : Chunk(WavChunkType.RIFF) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        val subWriter = ByteWriter(byteWriter.endianness)
        chunks.forEach { (_, chunk) -> chunk.write(subWriter) }
        val data = subWriter.toByteArray()
        byteWriter.addInt(data.size + 4)
        byteWriter.addString("WAVE")
        byteWriter.addBytes(data)
    }

    fun getChunk(id: WavChunkType): Any {
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
) : Chunk(WavChunkType.FMT) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addInt(16 + (fmtChunkExtension?.size() ?: 0))
        byteWriter.addShort(formatType.code)
        byteWriter.addShort(numChannels)
        byteWriter.addInt(sampleRate)
        byteWriter.addInt(byteRate)
        byteWriter.addShort(blockAlign)
        byteWriter.addShort(bitsPerSample)
        fmtChunkExtension?.write(byteWriter)
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

    fun write(byteWriter: ByteWriter) {
        byteWriter.addShort(extensionSize)
        if (extensionSize > 0) {
            byteWriter.addShort(validBitsPerSample)
            byteWriter.addInt(channelMask)
            byteWriter.addBytes(subFormat)
        }
    }

    fun size() = if (extensionSize > 0) 24 else 2

    constructor() : this(0, 0, 0, byteArrayOf())
}

class FactChunk(
    private val sampleLength: Int
) : Chunk(WavChunkType.FACT) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addInt(4)
        byteWriter.addInt(sampleLength)
    }
}

class DataChunk(
    val audioData: ByteArray
) : Chunk(WavChunkType.DATA) {
    override fun write(byteWriter: ByteWriter) {
        super.write(byteWriter)
        byteWriter.addInt(audioData.size)
        byteWriter.addBytes(audioData)
        if (audioData.size % 2 != 0) {
            byteWriter.addByte(0)
        }
    }
}