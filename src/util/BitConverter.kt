package util

class BitConverter {
    companion object {
        fun bitsToShort(byte1: Byte, byte2: Byte): Short {
            return (byte2.toInt() and 0xFF shl 8 or (byte1.toInt() and 0xFF)).toShort()
        }

        fun bitsToInt(byte1: Byte, byte2: Byte, byte3: Byte, byte4: Byte): Int {
            return (byte4.toInt() and 0xFF shl 24) + (byte3.toInt() and 0xFF shl 16) + (byte2.toInt() and 0xFF shl 8) + (byte1.toInt() and 0xFF)
        }

        fun bitsToLong(
            byte1: Byte, byte2: Byte, byte3: Byte, byte4: Byte, byte5: Byte, byte6: Byte, byte7: Byte, byte8: Byte
        ): Long {
            return (byte8.toLong() and 0xFF shl 56) or (byte7.toLong() and 0xFF shl 48) or (byte6.toLong() and 0xFF shl 40) or (byte5.toLong() and 0xFF shl 32) or (byte4.toLong() and 0xFF shl 24) or (byte3.toLong() and 0xFF shl 16) or (byte2.toLong() and 0xFF shl 8) or (byte1.toLong() and 0xFF)
        }

        fun shortToBits(value: Short): ByteArray {
            return byteArrayOf((value.toInt() and 0xFF).toByte(), (value.toInt() shr 8 and 0xFF).toByte())
        }

        fun intToBits(value: Int): ByteArray {
            return byteArrayOf(
                (value and 0xFF).toByte(),
                (value shr 8 and 0xFF).toByte(),
                (value shr 16 and 0xFF).toByte(),
                (value shr 24 and 0xFF).toByte()
            )
        }

        fun longToBits(value: Long): ByteArray {
            return byteArrayOf(
                (value and 0xFF).toByte(),
                (value shr 8 and 0xFF).toByte(),
                (value shr 16 and 0xFF).toByte(),
                (value shr 24 and 0xFF).toByte(),
                (value shr 32 and 0xFF).toByte(),
                (value shr 40 and 0xFF).toByte(),
                (value shr 48 and 0xFF).toByte(),
                (value shr 56 and 0xFF).toByte()
            )
        }
    }
}