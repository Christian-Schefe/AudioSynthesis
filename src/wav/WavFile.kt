package wav

import util.ByteWriter
import util.ByteReader
import util.Endianness
import java.io.FileOutputStream

class WavFile(
    private val audioFormat: AudioFormat, val samples: Array<DoubleArray>, private val sampleRate: UInt
) {
    private val channelCount = samples.size
    private val channelSampleCount = samples[0].size

    init {
        require(samples.all { it.size == channelSampleCount }) { "All channels must have the same number of samples" }
    }

    private fun computeAudioData(): ByteArray {
        val byteWriter = ByteWriter(Endianness.LITTLE)
        for (i in 0..<channelSampleCount) {
            for (j in samples.indices) {
                audioFormat.writeSample(samples[j][i].coerceIn(-1.0, 1.0), byteWriter)
            }
        }
        return byteWriter.toByteArray()
    }

    private fun createFmtChunk(): FmtChunk {
        return FmtChunk(
            formatType = audioFormat,
            numChannels = channelCount.toUShort(),
            sampleRate = sampleRate,
            byteRate = sampleRate * channelCount.toUInt() * audioFormat.bytesPerSample.toUInt(),
            blockAlign = (channelCount.toUInt() * audioFormat.bytesPerSample).toUShort(),
            bitsPerSample = (8u * audioFormat.bytesPerSample).toUShort(),
            fmtChunkExtension = FmtChunkExtension()
        )
    }

    private fun buildWavFileData(): WavFileData {
        val fmtChunk = createFmtChunk()
        val audioData = computeAudioData()
        val createFactChunk = audioFormat != AudioFormat.PCM
        return WavFileData(fmtChunk, audioData, createFactChunk)
    }

    fun withNormalizedSamples(factor: Double = 1.0): WavFile {
        val maxAmplitude = samples.mapNotNull { it.maxOrNull() }.maxOrNull() ?: 1.0
        return WavFile(
            audioFormat,
            samples.map { channel -> channel.map { (it / maxAmplitude) * factor }.toDoubleArray() }.toTypedArray(),
            sampleRate
        )
    }

    fun writeToFile(filePath: String) {
        val byteWriter = ByteWriter(Endianness.LITTLE)

        buildWavFileData().write(byteWriter)

        val fileOutputStream = FileOutputStream(filePath)
        fileOutputStream.write(byteWriter.toByteArray())
        fileOutputStream.close()
    }

    companion object {
        private fun parseSamples(audioFormat: AudioFormat, reader: ByteReader, channelCount: Int): Array<DoubleArray> {
            val samples = Array(channelCount) { mutableListOf<Double>() }
            var i = 0
            while (reader.bytesLeft() > 0) {
                samples[i % channelCount].add(audioFormat.readSample(reader).coerceIn(-1.0, 1.0))
                i++
            }
            return samples.map { it.toDoubleArray() }.toTypedArray()
        }

        private fun fromWavFileData(data: WavFileData): WavFile {
            val fmtChunk = data.riffChunk.getChunk(WavChunkType.FMT) as FmtChunk
            val dataChunk = data.riffChunk.getChunk(WavChunkType.DATA) as DataChunk
            val reader = ByteReader(Endianness.LITTLE, dataChunk.audioData)
            val channelCount = fmtChunk.numChannels.toInt()
            val samples = parseSamples(fmtChunk.formatType, reader, channelCount)
            return WavFile(fmtChunk.formatType, samples, fmtChunk.sampleRate)
        }

        fun readFromFile(filePath: String): WavFile {
            val reader = WavFileReader()
            val wavFileData = reader.readWavFile(filePath)
            return fromWavFileData(wavFileData)
        }
    }
}
