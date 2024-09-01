package wav

enum class Endianness {
    LITTLE, BIG
}

class ByteArrayBuilder(val endianness: Endianness) {
    private val bytes = mutableListOf<Byte>()

    fun addByte(value: Byte) {
        bytes.add(value)
    }

    fun addShort(value: Short) {
        val byte1 = (value.toInt() and 0xFF).toByte()
        val byte2 = (value.toInt() shr 8).toByte()
        if (endianness == Endianness.LITTLE) {
            bytes.add(byte1)
            bytes.add(byte2)
        } else {
            bytes.add(byte2)
            bytes.add(byte1)
        }
    }

    fun addShort(value: Int) {
        addShort(value.toShort())
    }

    fun addInt(value: Int) {
        val byte1 = (value and 0xFF).toByte()
        val byte2 = (value shr 8 and 0xFF).toByte()
        val byte3 = (value shr 16 and 0xFF).toByte()
        val byte4 = (value shr 24).toByte()
        if (endianness == Endianness.LITTLE) {
            bytes.add(byte1)
            bytes.add(byte2)
            bytes.add(byte3)
            bytes.add(byte4)
        } else {
            bytes.add(byte4)
            bytes.add(byte3)
            bytes.add(byte2)
            bytes.add(byte1)
        }
    }

    fun addFloat(value: Float) {
        addInt(value.toRawBits())
    }

    fun addString(value: String) {
        addBytes(value.toByteArray())
    }

    fun addBytes(value: ByteArray) {
        for (byte in value) {
            bytes.add(byte)
        }
    }

    fun toByteArray(): ByteArray {
        return bytes.toByteArray()
    }
}