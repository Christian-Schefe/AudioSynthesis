package util.bytes

class OldByteReader(
    private val endianness: Endianness, private val bytes: ByteArray, private val offset: Int, private val length: Int
) {
    init {
        require(offset >= 0) { "Offset must be non-negative" }
        require(length >= 0) { "Length must be non-negative" }
        require(offset + length <= bytes.size) { "Offset + length must be less than or equal to the byte array size" }
    }

    private var index = 0

    constructor(endianness: Endianness, bytes: ByteArray) : this(endianness, bytes, 0, bytes.size)

    fun readByte(): Byte {
        val result = peekByte()
        index++
        return result
    }

    fun peekByte(): Byte {
        if (index >= length) {
            throw IndexOutOfBoundsException("No more bytes to read")
        }
        return bytes[offset + index]
    }

    fun skipBytes(count: Int) {
        require(count <= bytesLeft()) { "Not enough bytes left: ${bytesLeft()}/$count" }
        index += count
    }

    fun readUByte(): UByte {
        return readByte().toUByte()
    }

    fun peekUByte(): UByte {
        return peekByte().toUByte()
    }

    fun readUShort(): UShort {
        val byte1 = readUByte()
        val byte2 = readUByte()
        return if (endianness == Endianness.LITTLE) {
            OldBitConverter.bitsToUShort(byte1, byte2)
        } else {
            OldBitConverter.bitsToUShort(byte2, byte1)
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
            OldBitConverter.bitsToUInt(byte1, byte2, byte3, byte4)
        } else {
            OldBitConverter.bitsToUInt(byte4, byte3, byte2, byte1)
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
            OldBitConverter.bitsToULong(byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8)
        } else {
            OldBitConverter.bitsToULong(byte8, byte7, byte6, byte5, byte4, byte3, byte2, byte1)
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
        var byte: UByte
        do {
            byte = readUByte()
            value = (value shl 7) or (byte and 0x7F.toUByte()).toUInt()
        } while (byte and 0x80.toUByte() != 0.toUByte())
        return value
    }

    fun readVarInt(): Int {
        return readVarUInt().toInt()
    }

    fun readString(length: Int): String {
        return readBytes(length).toString(Charsets.UTF_8)
    }

    fun bytesLeft(): Int {
        return length - index
    }

    fun position(): Int {
        return index
    }

    fun readBytes(length: Int): ByteArray {
        require(length <= bytesLeft()) { "Not enough bytes left: ${bytesLeft()}/$length" }
        val byteArray = bytes.copyOfRange(index + offset, index + offset + length)
        index += length
        return byteArray
    }

    fun readUntil(predicate: Byte, includeLast: Boolean): ByteArray {
        val readBytes = mutableListOf<Byte>()
        while (index < length) {
            val byte = peekByte()
            if (byte == predicate) {
                if (includeLast) {
                    readBytes.add(readByte())
                }
                return readBytes.toByteArray()
            }
            readBytes.add(readByte())
        }
        throw NoSuchElementException("No byte matching the predicate found")
    }

    fun subReader(length: Int): OldByteReader {
        val subReader = OldByteReader(endianness, bytes, offset + index, length)
        index += length
        return subReader
    }
}