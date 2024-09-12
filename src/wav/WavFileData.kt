package wav

import util.OldByteWriter

class WavFileData(val riffChunk: RiffChunk) {
    constructor(fmtChunk: FmtChunk, audioData: ByteArray, createFactChunk: Boolean = false) : this(
        RiffChunk(
            listOfNotNull(
                fmtChunk,
                if (createFactChunk) FactChunk(audioData.size.toUInt() / fmtChunk.blockAlign) else null,
                DataChunk(audioData)
            )
        )
    )

    fun write(byteWriter: OldByteWriter) {
        riffChunk.write(byteWriter)
    }
}

open class Chunk(
    var type: WavChunkType, var size: UInt
) {
    open val totalSize = size + 8u
    open fun write(byteWriter: OldByteWriter) {
        byteWriter.addString(type.id)
        byteWriter.addInt(size)
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
) : Chunk(WavChunkType.RIFF, 4u + chunks.values.sumOf { it.totalSize }) {
    override fun write(byteWriter: OldByteWriter) {
        super.write(byteWriter)
        byteWriter.addString("WAVE")
        chunks.forEach { (_, chunk) -> chunk.write(byteWriter) }
    }

    fun getChunk(id: WavChunkType): Any {
        return chunks[id] ?: throw IllegalArgumentException("Chunk not found")
    }

    constructor(chunks: List<Chunk>) : this(chunks.associateBy { it.type })
}

open class FmtChunk(
    val formatType: AudioFormat,
    val numChannels: UShort,
    val sampleRate: UInt,
    val byteRate: UInt,
    val blockAlign: UShort,
    val bitsPerSample: UShort,
    val fmtChunkExtension: FmtChunkExtension? = null
) : Chunk(WavChunkType.FMT, 16u + (fmtChunkExtension?.size() ?: 0u)) {
    override fun write(byteWriter: OldByteWriter) {
        super.write(byteWriter)
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
    private val extensionSize: UShort,
    private val validBitsPerSample: UShort,
    private val channelMask: UInt,
    private val subFormat: ByteArray
) {
    init {
        if (extensionSize > 0u && subFormat.size != 16) {
            throw IllegalArgumentException("SubFormat size must be 16")
        }
    }

    fun write(byteWriter: OldByteWriter) {
        byteWriter.addShort(extensionSize)
        if (extensionSize > 0u) {
            byteWriter.addShort(validBitsPerSample)
            byteWriter.addInt(channelMask)
            byteWriter.addBytes(subFormat)
        }
    }

    fun size() = if (extensionSize > 0u) 24u else 2u

    constructor() : this(0u, 0u, 0u, byteArrayOf())
}

class FactChunk(
    private val sampleLength: UInt
) : Chunk(WavChunkType.FACT, 4u) {
    override fun write(byteWriter: OldByteWriter) {
        super.write(byteWriter)
        byteWriter.addInt(sampleLength)
    }
}

class DataChunk(
    val audioData: ByteArray
) : Chunk(WavChunkType.DATA, audioData.size.toUInt()) {
    override val totalSize = size + 8u + (size % 2u)
    override fun write(byteWriter: OldByteWriter) {
        super.write(byteWriter)
        byteWriter.addBytes(audioData)
        if (audioData.size % 2 != 0) {
            byteWriter.addByte(0u)
        }
    }
}