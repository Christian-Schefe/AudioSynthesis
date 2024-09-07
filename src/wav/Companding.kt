package wav

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.*


fun encodeMuLaw(number: Double): Byte {
    val max = 0x1FFF
    val bias = 33
    var mask = 0x1000
    val sign = if (number < 0) 0x80.toByte() else 0x00.toByte()
    var num = (abs(number) * max).toInt()
    num += bias
    if (num > max) num = max
    var position = 12
    while (num and mask != mask && position >= 5) {
        position--
        mask = mask shr 1
    }
    val lsb = ((num shr (position - 4)) and 0x0F).toByte()
    return (sign or ((position - 5) shl 4).toByte() or lsb) xor 0xFF.toByte()
}

fun decodeMuLaw(number: Byte): Double {
    val max = 0x1FFF
    val bias = 33
    val invNumber = number xor 0xFF.toByte()
    val sign = invNumber and 0x80.toByte()
    val position = ((invNumber and 0x70).toUInt() shr 4).toInt() + 5
    val decoded =
        ((1 shl position) or ((invNumber and 0x0F).toInt() shl (position - 4)) or (1 shl (position - 5))) - bias
    val double = decoded.toDouble() / max
    return if (sign.toInt() == 0) double else -double
}

fun encodeALaw(number: Double): Byte {
    val max = 0xFFF
    var mask = 0x800
    val sign = if (number >= 0) 0x80.toByte() else 0x00.toByte()
    var num = (abs(number) * max).toInt()
    if (num > max) num = max
    var position = 11
    while (num and mask != mask && position >= 5) {
        position -= 1
        mask = mask shr 1
    }
    val lsb = (num shr if (position == 4) {
        1
    } else {
        position - 4
    }).toByte() and 0x0F.toByte()
    return (sign or ((position - 4) shl 4).toByte() or lsb) xor 0x55.toByte()
}

fun decodeALaw(number: Byte): Double {
    val max = 0xFFF
    var num = number xor 0x55.toByte()
    val sign = num and 0x80.toByte()
    num = if (sign.toInt() == 0) num else (num and 0x7F.toByte())
    val position = (((num.toUInt() and 0xF0u) shr 4) + 4u).toInt()
    val decoded = if (position == 4) {
        (num.toInt() shl 1) or 1
    } else {
        (1 shl position) or ((num.toInt() and 0x0F) shl (position - 4)) or (1 shl (position - 5))
    }
    val double = decoded.toDouble() / max
    return if (sign.toInt() == 0) -double else double
}
