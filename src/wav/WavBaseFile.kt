package wav

abstract class WavBaseFile(val samples: Array<DoubleArray>, val sampleRate: Int) {
    val channelCount = samples.size
    val channelSampleCount = samples[0].size

    abstract fun computeAudioData(): ByteArray
    abstract fun createFmtChunk(): FmtChunk

    fun buildWavFile(): WavFileData {
        val fmtChunk = createFmtChunk()
        val audioData = computeAudioData()
        return WavFileData(fmtChunk, audioData)
    }
}

class WavFile(private val format: AudioFormat, samples: Array<DoubleArray>, sampleRate: Int) :
    WavBaseFile(samples, sampleRate) {
    private val instance = when (format) {
        AudioFormat.PCM -> WavPCMFile(samples, sampleRate)
        AudioFormat.IEEE_FLOAT -> WavIEEEFile(samples, sampleRate)
    }

    fun withNormalizedSamples(factor: Double = 0.99): WavFile {
        val maxAmplitude = samples.mapNotNull { it.maxOrNull() }.maxOrNull() ?: 1.0
        return WavFile(
            format,
            samples.map { channel -> channel.map { (it / maxAmplitude) * factor }.toDoubleArray() }.toTypedArray(),
            sampleRate
        )
    }

    override fun computeAudioData(): ByteArray {
        return instance.computeAudioData()
    }

    override fun createFmtChunk(): FmtChunk {
        return instance.createFmtChunk()
    }
}