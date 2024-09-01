package wav

class WavIEEEFile(samples: Array<DoubleArray>, sampleRate: Int) : WavBaseFile(samples, sampleRate) {
    override fun computeAudioData(): ByteArray {
        val audioData = ByteArrayBuilder(Endianness.LITTLE)
        for (i in 0 until channelSampleCount) {
            for (j in 0 until channelCount) {
                audioData.addFloat(samples[j][i].toFloat())
            }
        }
        return audioData.toByteArray()
    }

    override fun createFmtChunk(): FmtChunk {
        return FmtChunk(
            formatType = AudioFormat.IEEE_FLOAT,
            numChannels = channelCount.toShort(),
            sampleRate = sampleRate,
            byteRate = sampleRate * channelCount * 4,
            blockAlign = (channelCount * 4).toShort(),
            bitsPerSample = 32,
            fmtChunkExtension = FmtChunkExtension()
        )
    }
}