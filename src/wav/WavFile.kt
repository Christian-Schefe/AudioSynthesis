package wav

import util.ByteArrayBuilder
import util.ByteReader
import util.Endianness

class WavFile(private val audioFormat: AudioFormat, val samples: Array<DoubleArray>, private val sampleRate: Int) {
    private val channelCount = samples.size
    private val channelSampleCount = samples[0].size

    private fun computeAudioData(): ByteArray {
        val byteArrayBuilder = ByteArrayBuilder(Endianness.LITTLE)
        for (i in 0..<channelSampleCount) {
            for (j in samples.indices) {
                audioFormat.writeSample(samples[j][i].coerceIn(-1.0, 1.0), byteArrayBuilder)
            }
        }
        return byteArrayBuilder.toByteArray()
    }

    private fun createFmtChunk(): FmtChunk {
        return FmtChunk(
            formatType = audioFormat,
            numChannels = channelCount.toShort(),
            sampleRate = sampleRate,
            byteRate = sampleRate * channelCount * audioFormat.bytesPerSample,
            blockAlign = (channelCount * audioFormat.bytesPerSample).toShort(),
            bitsPerSample = (8 * audioFormat.bytesPerSample).toShort(),
            fmtChunkExtension = FmtChunkExtension()
        )
    }

    fun buildWavFileData(): WavFileData {
        val fmtChunk = createFmtChunk()
        val audioData = computeAudioData()
        return WavFileData(fmtChunk, audioData)
    }

    fun withNormalizedSamples(factor: Double = 0.99): WavFile {
        val maxAmplitude = samples.mapNotNull { it.maxOrNull() }.maxOrNull() ?: 1.0
        return WavFile(
            audioFormat,
            samples.map { channel -> channel.map { (it / maxAmplitude) * factor }.toDoubleArray() }.toTypedArray(),
            sampleRate
        )
    }

    companion object {
        private fun parseSamples(audioFormat: AudioFormat, reader: ByteReader, channelCount: Int): Array<DoubleArray> {
            val samples = Array(channelCount) { mutableListOf<Double>() }
            var i = 0
            while (reader.bytesLeft() > 0) {
                samples[i % channelCount].add(audioFormat.readSample(reader))
                i++
            }
            return samples.map { it.toDoubleArray() }.toTypedArray()
        }

        fun fromWavFileData(data: WavFileData): WavFile {
            val fmtChunk = data.riffChunk.getChunk(ChunkType.FMT) as FmtChunk
            val dataChunk = data.riffChunk.getChunk(ChunkType.DATA) as DataChunk
            val reader = ByteReader(Endianness.LITTLE, dataChunk.audioData)
            val channelCount = fmtChunk.numChannels.toInt()
            val samples = parseSamples(fmtChunk.formatType, reader, channelCount)
            return WavFile(fmtChunk.formatType, samples, fmtChunk.sampleRate)
        }
    }
}
