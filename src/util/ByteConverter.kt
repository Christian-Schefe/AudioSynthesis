package util

class ByteConverter {
    companion object {
        fun shiftLeft(value: Byte, shift: Int): Int {
            return (value.toInt() and 0xFF) shl shift
        }

        fun bytesToShort(lowByte: Byte, highByte: Byte): Short {
            return (shiftLeft(lowByte, 0) or shiftLeft(highByte, 8)).toShort()
        }

        fun bytesToShort(array: ByteArray, endianness: Endianness): Short {
            return when (endianness) {
                Endianness.LITTLE -> bytesToShort(array[0], array[1])
                Endianness.BIG -> bytesToShort(array[1], array[0])
            }
        }

        fun bytesToInt(lowByte: Byte, lowMiddleByte: Byte, highMiddleByte: Byte, highByte: Byte): Int {
            return (shiftLeft(lowByte, 0) or shiftLeft(lowMiddleByte, 8) or shiftLeft(highMiddleByte, 16) or shiftLeft(
                highByte, 24
            ))
        }

        fun bytesToInt(array: ByteArray, endianness: Endianness): Int {
            return when (endianness) {
                Endianness.LITTLE -> bytesToInt(array[0], array[1], array[2], array[3])
                Endianness.BIG -> bytesToInt(array[3], array[2], array[1], array[0])
            }
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