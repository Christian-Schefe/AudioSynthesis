package util

class OldBitConverter {
    companion object {
        fun bitsToUShort(byte1: UByte, byte2: UByte): UShort {
            return (byte2.toInt() shl 8 or byte1.toInt()).toUShort()
        }

        fun bitsToUShort(array: ByteArray) = bitsToUShort(array[0].toUByte(), array[1].toUByte())

        fun bitsToUInt(byte1: UByte, byte2: UByte, byte3: UByte, byte4: UByte): UInt {
            return (byte4.toUInt() shl 24) + (byte3.toUInt() shl 16) + (byte2.toUInt() shl 8) + byte1.toUInt()
        }

        fun bitsToUInt(array: ByteArray) =
            bitsToUInt(array[0].toUByte(), array[1].toUByte(), array[2].toUByte(), array[3].toUByte())

        fun bitsToInt(array: ByteArray) = bitsToUInt(array).toInt()


        fun bitsToULong(
            byte1: UByte,
            byte2: UByte,
            byte3: UByte,
            byte4: UByte,
            byte5: UByte,
            byte6: UByte,
            byte7: UByte,
            byte8: UByte
        ): ULong {
            return (byte8.toULong() shl 56) or (byte7.toULong() shl 48) or (byte6.toULong() shl 40) or (byte5.toULong() shl 32) or (byte4.toULong() shl 24) or (byte3.toULong() shl 16) or (byte2.toULong() shl 8) or byte1.toULong()
        }

        fun bitsToULong(array: ByteArray) = bitsToULong(
            array[0].toUByte(),
            array[1].toUByte(),
            array[2].toUByte(),
            array[3].toUByte(),
            array[4].toUByte(),
            array[5].toUByte(),
            array[6].toUByte(),
            array[7].toUByte()
        )

        fun shortToBits(value: Short): ByteArray {
            return byteArrayOf((value.toInt()).toByte(), (value.toInt() shr 8).toByte())
        }

        fun shortToBits(value: UShort): ByteArray {
            return shortToBits(value.toShort())
        }

        fun intToBits(value: Int): ByteArray {
            return byteArrayOf(
                (value).toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()
            )
        }

        fun intToBits(value: UInt): ByteArray {
            return intToBits(value.toInt())
        }

        fun longToBits(value: Long): ByteArray {
            return byteArrayOf(
                (value).toByte(),
                (value shr 8).toByte(),
                (value shr 16).toByte(),
                (value shr 24).toByte(),
                (value shr 32).toByte(),
                (value shr 40).toByte(),
                (value shr 48).toByte(),
                (value shr 56).toByte()
            )
        }

        fun longToBits(value: ULong): ByteArray {
            return longToBits(value.toLong())
        }

        fun intToVarInt(value: Int): ByteArray {
            val bytes = mutableListOf<Byte>()
            var v = value
            while (true) {
                val byte = (v and 0x7F).toUByte()
                v = v ushr 7
                if (v == 0) {
                    bytes.add(byte.toByte())
                    break
                } else {
                    bytes.add((byte or 0x80u).toByte())
                }
            }
            return bytes.toByteArray()
        }

        fun intToVarInt(value: UInt): ByteArray {
            return intToVarInt(value.toInt())
        }

        fun intToBytes(value: Int, endianness: Endianness = Endianness.LITTLE, byteCount: Int = 4): ByteArray {
            val bytes = ByteArray(byteCount)
            var current = value
            for (i in 0 until byteCount) {
                bytes[i] = current.toByte()
                current = current shr 8
            }
            return when (endianness) {
                Endianness.BIG -> bytes.reversedArray()
                Endianness.LITTLE -> bytes
            }
        }
    }
}