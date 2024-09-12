package util

class ByteReader(
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

    fun readShort(): Short {
        return ByteConverter.bytesToShort(readBytes(2), endianness)
    }

    fun readInt(): Int {
        return ByteConverter.bytesToInt(readBytes(4), endianness)
    }

    fun readFloat(): Float {
        return Float.fromBits(ByteConverter.bytesToInt(readBytes(4), Endianness.LITTLE))
    }

    fun readVarInt(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = readByte()
            result = result or ((byte.toInt() and 0x7F) shl shift)
            if ((byte.toInt() and 0x80) == 0) {
                return result
            }
            shift += 7
        }
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

    fun subReader(length: Int): ByteReader {
        val subReader = ByteReader(endianness, bytes, offset + index, length)
        index += length
        return subReader
    }
}