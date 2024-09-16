package util.bytes

class ByteConverter {
    companion object {
        fun shiftLeft(value: Byte, shift: Int): Int {
            return (value.toInt() and 0xFF) shl shift
        }

        fun bytesToShort(lowByte: Byte, highByte: Byte): Short {
            return (shiftLeft(lowByte, 0) or shiftLeft(highByte, 8)).toShort()
        }

        fun bytesToShort(array: ByteArray, endianness: Endianness): Short {
            val arr = if (endianness == Endianness.BIG) array.reversedArray() else array
            val b0 = if (arr.size > 0) arr[0] else 0
            val b1 = if (arr.size > 1) arr[1] else 0
            return bytesToShort(b0, b1)
        }

        fun bytesToInt(lowByte: Byte, lowMiddleByte: Byte, highMiddleByte: Byte, highByte: Byte): Int {
            return (shiftLeft(lowByte, 0) or shiftLeft(lowMiddleByte, 8) or shiftLeft(highMiddleByte, 16) or shiftLeft(
                highByte, 24
            ))
        }

        fun bytesToInt(array: ByteArray, endianness: Endianness): Int {
            val arr = if (endianness == Endianness.BIG) array.reversedArray() else array
            val b0 = if (arr.size > 0) arr[0] else 0
            val b1 = if (arr.size > 1) arr[1] else 0
            val b2 = if (arr.size > 2) arr[2] else 0
            val b3 = if (arr.size > 3) arr[3] else 0
            return bytesToInt(b0, b1, b2, b3)
        }

        fun shortToBytes(value: Short, endianness: Endianness, byteCount: Int = 2): ByteArray {
            val bytes = byteArrayOf((value.toInt()).toByte(), (value.toInt() shr 8).toByte()).sliceArray(0..<byteCount)
            return when (endianness) {
                Endianness.LITTLE -> bytes
                Endianness.BIG -> bytes.reversedArray()
            }
        }

        fun intToBytes(value: Int, endianness: Endianness, byteCount: Int = 4): ByteArray {
            val bytes = byteArrayOf(
                (value).toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()
            ).sliceArray(0..<byteCount)
            return when (endianness) {
                Endianness.LITTLE -> bytes
                Endianness.BIG -> bytes.reversedArray()
            }
        }

        fun intToVarInt(value: Int): ByteArray {
            val bytes = mutableListOf((value and 0x7F).toByte())
            var v = value ushr 7
            while (v != 0) {
                bytes.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            bytes.reverse()
            return bytes.toByteArray()
        }
    }
}