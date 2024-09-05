package util

class ByteReader(val endianness: Endianness, val bytes: ByteArray) {
    private var index = 0

    fun readByte(): Byte {
        val result = bytes[index]
        index++
        return result
    }

    fun readShort(): Short {
        val byte1 = readByte()
        val byte2 = readByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToShort(byte1, byte2)
        } else {
            BitConverter.bitsToShort(byte2, byte1)
        }
    }

    fun readInt(): Int {
        val byte1 = readByte()
        val byte2 = readByte()
        val byte3 = readByte()
        val byte4 = readByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToInt(byte1, byte2, byte3, byte4)
        } else {
            BitConverter.bitsToInt(byte4, byte3, byte2, byte1)
        }
    }

    fun readLong(): Long {
        val byte1 = readByte()
        val byte2 = readByte()
        val byte3 = readByte()
        val byte4 = readByte()
        val byte5 = readByte()
        val byte6 = readByte()
        val byte7 = readByte()
        val byte8 = readByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToLong(byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8)
        } else {
            BitConverter.bitsToLong(byte8, byte7, byte6, byte5, byte4, byte3, byte2, byte1)
        }
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readDouble(): Double {
        return Double.fromBits(readLong())
    }

    fun readVariableLengthQuantity(): Int {
        var value = 0
        var byte: Int
        do {
            byte = readByte().toInt()
            value = (value shl 7) or (byte and 0x7F)
        } while (byte and 0x80 != 0)
        return value
    }

    fun readString(length: Int): String {
        val string = bytes.copyOfRange(index, index + length).toString(Charsets.UTF_8)
        index += length
        return string
    }

    fun bytesLeft(): Int {
        return bytes.size - index
    }

    fun readBytes(length: Int): ByteArray {
        val byteArray = bytes.copyOfRange(index, index + length)
        index += length
        return byteArray
    }
}