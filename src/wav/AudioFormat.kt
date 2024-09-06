package wav

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
        { reader -> wav.fromIeeeFloat(reader.readFloat()) }),
    A_LAW(6u, 1u, { sample, writer ->
        writer.addByte(toALaw(sample))
    }, { reader -> fromALaw(reader.readUByte()) });

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
    return pcmToALaw(toPcm(sample).toInt())
}

fun fromALaw(sample: UByte): Double {
    return fromPcm(aLawToPcm(sample).toShort())
}

fun pcmToALaw(pcmSample: Int): UByte {
    val aLawMax = 0xFFF
    var mask = 0x800
    var sample = pcmSample

    // Handle sign
    val sign = if (pcmSample < 0) {
        sample = -pcmSample
        0x80
    } else 0

    // Clamp to the maximum A-Law value
    if (sample > aLawMax) {
        sample = aLawMax
    }

    // Find the position
    var position = 11
    while ((sample and mask) != mask && position >= 5) {
        mask = mask shr 1
        position--
    }

    // Calculate the least significant bits
    val lsb = (sample shr (if (position == 4) 1 else position - 4)) and 0x0F

    // Return the A-Law byte
    return ((sign or ((position - 4) shl 4) or lsb) xor 0x55).toUByte()
}

fun aLawToPcm(number: UByte): Int {
    // XOR the input with 0x55 as in A-Law encoding
    var sample = number.toInt() xor 0x55

    // Determine the sign
    val sign = if (sample and 0x80 != 0) {
        sample = sample and (1 shl 7).inv() // Remove the sign bit
        -1
    } else 0

    // Extract the exponent (position) and mantissa
    val position = ((sample and 0xF0) shr 4) + 4

    // Decode based on the position
    val decoded = if (position != 4) {
        ((1 shl position) or ((sample and 0x0F) shl (position - 4)) or (1 shl (position - 5)))
    } else {
        (sample shl 1) or 1
    }

    // Apply the sign
    return if (sign == 0) decoded else (-decoded)
}
