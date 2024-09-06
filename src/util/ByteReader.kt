package util

class ByteReader(private val endianness: Endianness, private val bytes: ByteArray) {
    private var index = 0

    fun readByte(): Byte {
        val result = bytes[index]
        index++
        return result
    }

    fun readUByte(): UByte {
        return readByte().toUByte()
    }

    fun readUShort(): UShort {
        val byte1 = readUByte()
        val byte2 = readUByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToShort(byte1, byte2)
        } else {
            BitConverter.bitsToShort(byte2, byte1)
        }
    }

    fun readShort(): Short {
        return readUShort().toShort()
    }

    fun readUInt(): UInt {
        val byte1 = readUByte()
        val byte2 = readUByte()
        val byte3 = readUByte()
        val byte4 = readUByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToInt(byte1, byte2, byte3, byte4)
        } else {
            BitConverter.bitsToInt(byte4, byte3, byte2, byte1)
        }
    }

    fun readInt(): Int {
        return readUInt().toInt()
    }

    fun readULong(): ULong {
        val byte1 = readUByte()
        val byte2 = readUByte()
        val byte3 = readUByte()
        val byte4 = readUByte()
        val byte5 = readUByte()
        val byte6 = readUByte()
        val byte7 = readUByte()
        val byte8 = readUByte()
        return if (endianness == Endianness.LITTLE) {
            BitConverter.bitsToLong(byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8)
        } else {
            BitConverter.bitsToLong(byte8, byte7, byte6, byte5, byte4, byte3, byte2, byte1)
        }
    }

    fun readLong(): Long {
        return readULong().toLong()
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readDouble(): Double {
        return Double.fromBits(readLong())
    }

    fun readVarUInt(): UInt {
        var value = 0u
        var byte: UInt
        do {
            byte = readUByte().toUInt()
            value = (value shl 7) or (byte and 0x7Fu)
        } while (byte and 0x80u != 0u)
        return value
    }

    fun readVarInt(): Int {
        return readVarUInt().toInt()
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
        require(length <= bytesLeft()) { "Not enough bytes left: ${bytesLeft()}/$length" }
        val byteArray = bytes.copyOfRange(index, index + length)
        index += length
        return byteArray
    }
}