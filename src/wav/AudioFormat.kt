package wav

import util.bytes.ByteWriter
import util.bytes.ByteReader

enum class AudioFormat(
    val code: Short,
    val bytesPerSample: Int,
    val writeSample: (Double, ByteWriter) -> Unit,
    val readSample: (ByteReader) -> Double
) {
    PCM(1,
        2,
        { sample, writer -> writer.addShort(toPcm(sample)) },
        { reader -> fromPcm(reader.readShort()) }),
    IEEE_FLOAT(3,
        4,
        { sample, writer -> writer.addFloat(toIeeeFloat(sample)) },
        { reader -> fromIeeeFloat(reader.readFloat()) }),
    A_LAW(6, 1, { sample, writer ->
        writer.addByte(toALaw(sample))
    }, { reader -> fromALaw(reader.readByte()) }),
    MU_LAW(7, 1, { sample, writer ->
        writer.addByte(toMuLaw(sample))
    }, { reader -> fromMuLaw(reader.readByte()) });

    companion object {
        fun fromCode(code: Short): AudioFormat {
            return entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown audio format")
        }
    }
}

fun toPcm(sample: Double): Short {
    return (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
}

fun fromPcm(sample: Short): Double {
    return sample.toDouble() / Short.MAX_VALUE
}

fun toIeeeFloat(sample: Double): Float {
    return sample.toFloat()
}

fun fromIeeeFloat(sample: Float): Double {
    return sample.toDouble()
}

fun toALaw(sample: Double): Byte {
    return encodeALaw(sample.coerceIn(-1.0, 1.0))
}

fun fromALaw(sample: Byte): Double {
    return decodeALaw(sample)
}

fun toMuLaw(sample: Double): Byte {
    return encodeMuLaw(sample.coerceIn(-1.0, 1.0))
}

fun fromMuLaw(sample: Byte): Double {
    return decodeMuLaw(sample)
}

