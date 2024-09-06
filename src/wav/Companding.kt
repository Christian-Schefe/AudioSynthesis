package wav

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.*

const val MU = 255.0
const val A = 87.6
val LN_A = ln(A)
const val A_THRESHOLD = 1.0 / A
val A_INV_THRESHOLD = 1.0 / (1.0 + LN_A)

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

fun encodeALaw(number: Double): Double {
    val num = A * abs(number)
    return sign(number) * (if (abs(number) < A_THRESHOLD) {
        num
    } else {
        1 + ln(num)
    }) / (1 + LN_A)
}

fun decodeALaw(number: Double): Double {
    val num = abs(number) * (1 + LN_A)
    return sign(number) * if (abs(number) < A_INV_THRESHOLD) {
        num
    } else {
        exp(num - 1)
    } / A
}
