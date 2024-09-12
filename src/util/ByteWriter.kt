package util

class ByteWriter(private val endianness: Endianness) {
    private val bytes = mutableListOf<Byte>()

    fun addByte(value: Byte) {
        bytes.add(value)
    }

    fun addShort(value: Short, byteCount: Int = 2) {
        addBytes(ByteConverter.shortToBytes(value, endianness, byteCount))
    }

    fun addInt(value: Int, byteCount: Int = 4) {
        addBytes(ByteConverter.intToBytes(value, endianness, byteCount))
    }

    fun addFloat(value: Float) {
        addBytes(ByteConverter.intToBytes(value.toRawBits(), Endianness.LITTLE))
    }

    fun addString(value: String) {
        addBytes(value.toByteArray())
    }

    fun addBytes(value: ByteArray) {
        for (byte in value) {
            bytes.add(byte)
        }
    }

    fun addVarInt(value: Int) {
        addBytes(ByteConverter.intToVarInt(value))
    }

    fun toByteArray(): ByteArray {
        return bytes.toByteArray()
    }
}