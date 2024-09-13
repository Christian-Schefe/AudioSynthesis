package util

fun main() {
    val bytes = byteArrayOf(0x01, 0xE0.toByte(), 0x4D, 0x54)
    println(bytes.toList())
    println(ByteConverter.bytesToShort(bytes, Endianness.BIG))
    println(ByteConverter.bytesToShort(0xE0.toByte(), 0x01).toUShort().toString(16))

    println(ByteConverter.intToVarInt(0x4000).map { it.toUByte().toString(16) })

    val reader = ByteReader(Endianness.BIG, byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x00))
    println(reader.readVarInt().toUInt().toString(16))
}