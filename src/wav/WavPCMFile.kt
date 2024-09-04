package wav

class WavPCMFile(samples: Array<DoubleArray>, sampleRate: Int) : WavBaseFile(samples, sampleRate) {
    override fun computeAudioData(): ByteArray {
        val audioData = ByteArrayBuilder(Endianness.LITTLE)
        for (i in 0..<channelSampleCount) {
            for (j in 0..<channelCount) {
                audioData.addShort((samples[j][i].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt())
            }
        }
        return audioData.toByteArray()
    }

    override fun createFmtChunk(): FmtChunk {
        return FmtChunk(
            formatType = AudioFormat.PCM,
            numChannels = channelCount.toShort(),
            sampleRate = sampleRate,
            byteRate = sampleRate * channelCount * 2,
            blockAlign = (channelCount * 2).toShort(),
            bitsPerSample = 16
        )
    }
}