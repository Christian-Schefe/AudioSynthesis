package util

class ByteConverter {
    companion object {
        fun bytesToShort(lowByte: Byte, highByte: Byte): Short {
            return (lowByte.toInt() or (highByte.toInt() shl 8)).toShort()
        }

        fun bytesToShort(array: ByteArray, endianness: Endianness): Short {
            return when (endianness) {
                Endianness.LITTLE -> bytesToShort(array[0], array[1])
                Endianness.BIG -> bytesToShort(array[1], array[0])
            }
        }

        fun bytesToInt(lowByte: Byte, lowMiddleByte: Byte, highMiddleByte: Byte, highByte: Byte): Int {
            return lowByte.toInt() or (lowMiddleByte.toInt() shl 8) or (highMiddleByte.toInt() shl 16) or (highByte.toInt() shl 24)
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
            val bytes = mutableListOf<Byte>()
            var v = value
            while (true) {
                val byte = (v and 0x7F).toByte()
                v = v ushr 7
                if (v == 0) {
                    bytes.add(byte)
                    break
                } else {
                    bytes.add((byte.toInt() or 0x80).toByte())
                }
            }
            return bytes.toByteArray()
        }
    }
}