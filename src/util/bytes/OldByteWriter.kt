package util.bytes

class OldByteWriter(private val endianness: Endianness) {
    private val bytes = mutableListOf<Byte>()

    fun addByte(value: Byte) {
        bytes.add(value)
    }

    fun addByte(value: UByte) {
        addByte(value.toByte())
    }

    fun addShort(value: Short) {
        val arr = OldBitConverter.shortToBits(value)
        if (endianness == Endianness.LITTLE) {
            addBytes(arr)
        } else {
            addBytes(arr.reversedArray())
        }
    }

    fun addShort(value: UShort) {
        addShort(value.toShort())
    }

    fun addInt(value: Int, byteLimit: Int = 4) {
        val arr = OldBitConverter.intToBits(value).slice(0..<byteLimit).toByteArray()
        if (endianness == Endianness.LITTLE) {
            addBytes(arr)
        } else {
            addBytes(arr.reversedArray())
        }
    }

    fun addInt(value: UInt, byteLimit: Int = 4) {
        addInt(value.toInt(), byteLimit)
    }

    fun addLong(value: Long) {
        val arr = OldBitConverter.longToBits(value)
        if (endianness == Endianness.LITTLE) {
            addBytes(arr)
        } else {
            addBytes(arr.reversedArray())
        }
    }

    fun addLong(value: ULong) {
        addLong(value.toLong())
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

    fun addVarInt(value: Int) {
        addBytes(OldBitConverter.intToVarInt(value))
    }

    fun addVarInt(value: UInt) {
        addVarInt(value.toInt())
    }

    fun toByteArray(): ByteArray {
        return bytes.toByteArray()
    }
}