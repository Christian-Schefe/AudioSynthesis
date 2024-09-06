fun encodeMuLaw(number: Short): UByte {
    var mask = 0x1000u
    var sample = number.toUInt() shr 3

    // Handle sign
    val sign = if (number < 0) {
        sample = (-number).toUInt() shr 3
        0x80.toUByte()
    } else 0x00.toUByte()

    // Add the bias
    sample += 33u

    // Find the position
    var position = 12
    while ((sample and mask) != mask && position >= 5) {
        mask = mask shr 1
        position--
    }

    // Calculate the least significant bits
    val lsb = ((sample shr (position - 4)) and 0x0Fu).toUByte()

    // Return the Mu-Law byte
    return (sign or (((position - 5) shl 4).toUByte()) or lsb) xor 0xFFu
}

fun decodeMuLaw(number: UByte): Short {
    var sample = number.inv().toUInt()

    val sign = if (sample and 0x80u != 0x00u) {
        sample = sample and 0x7Fu
        -1
    } else 0

    val position = ((sample and 0xF0u) shr 4).toInt() + 5

    val decoded = ((1u shl position) or ((sample and 0x0Fu) shl (position - 4)) or (1u shl (position - 5))).toInt() - 33

    return if (sign == 0) decoded.toShort() else (-decoded).toShort()
}

fun main() {
    println(encodeMuLaw(0x7FFF.toShort()))
}