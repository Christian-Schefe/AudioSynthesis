package wav

import decodeMuLaw
import encodeMuLaw
import util.ByteReader
import util.ByteWriter

enum class AudioFormat(
    val code: UShort,
    val bytesPerSample: UInt,
    val writeSample: (Double, ByteWriter) -> Unit,
    val readSample: (ByteReader) -> Double
) {
    PCM(1u,
        2u,
        { sample, writer -> writer.addShort(toPcm(sample)) },
        { reader -> fromPcm(reader.readShort()) }),
    IEEE_FLOAT(3u,
        4u,
        { sample, writer -> writer.addFloat(toIeeeFloat(sample)) },
        { reader -> wav.fromIeeeFloat(reader.readFloat()) }),/*A_LAW(6u, 1u, { sample, writer ->
        writer.addByte(toALaw(sample))
    }, { reader -> fromALaw(reader.readUByte()) }),
    MU_LAW(7u, 1u, { sample, writer ->
        writer.addByte(toMuLaw(sample))
    }, { reader -> fromMuLaw(reader.readUByte()) })*/;

    companion object {
        fun fromCode(code: UShort): AudioFormat {
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

fun toALaw(sample: Double): UByte {
    return encodeALaw(toPcm(sample).toInt() shr 4)
}

fun fromALaw(sample: UByte): Double {
    return fromPcm((decodeALaw(sample) shl 4).toShort())
}

fun toMuLaw(sample: Double): UByte {
    return encodeMuLaw(toPcm(sample))
}

fun fromMuLaw(sample: UByte): Double {
    return fromPcm(decodeMuLaw(sample))
}


fun encodeALaw(number: Int): UByte {
    val aLawMax = 0xFFF
    var mask = 0x800
    var sample = number

    val sign = if (number < 0) {
        sample = -number
        0x80
    } else 0

    if (sample > aLawMax) {
        sample = aLawMax
    }

    var position = 11
    while ((sample and mask) != mask && position >= 5) {
        mask = mask shr 1
        position--
    }

    val lsb = (sample shr (if (position == 4) 1 else position - 4)) and 0x0F

    return ((sign or ((position - 4) shl 4) or lsb) xor 0x55).toUByte()
}

fun decodeALaw(number: UByte): Int {
    var sample = number.toUInt() xor 0x55u

    val sign = if (sample and 0x80u != 0u) {
        sample = sample and (1u shl 7).inv()
        -1
    } else 0

    val position = ((sample and 0xF0u) shr 4).toInt() + 4

    val decoded = if (position != 4) {
        ((1u shl position) or ((sample and 0x0Fu) shl (position - 4)) or (1u shl (position - 5)))
    } else {
        (sample shl 1) or 1u
    }

    return if (sign == 0) decoded.toInt() else (-decoded.toInt())
}
