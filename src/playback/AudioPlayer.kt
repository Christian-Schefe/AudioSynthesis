package playback

import nodes.*
import util.bytes.ByteConverter
import util.bytes.Endianness
import wav.WavFile
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

class AudioPlayer {
    fun play(file: WavFile) {
        val format = AudioFormat(file.sampleRate.toFloat(), 16, 2, true, false)

        val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(lineInfo) as SourceDataLine

        line.open(format)
        line.start()

        val data = ByteArray(file.channelSampleCount * file.channelCount * 2)
        for (i in 0..<file.channelSampleCount) {
            for (j in 0..<file.channelCount) {
                val doubleSample = file.samples[j][i]
                val sample = (doubleSample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                val (b1, b2) = ByteConverter.shortToBytes(sample, Endianness.LITTLE)
                data[i * file.channelCount * 2 + j * 2] = b1
                data[i * file.channelCount * 2 + j * 2 + 1] = b2
            }
        }

        line.write(data, 0, data.size)

        line.drain()
        line.stop()
    }

    fun renderAndPlay(audioNode: AudioNode, ctx: Context, seconds: Double, channels: Int = 2) {
        val format = AudioFormat(ctx.sampleRate.toFloat(), 16, 2, true, false)

        val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(lineInfo) as SourceDataLine

        val node = Pipeline(listOf(audioNode, LookaheadLimiter(channels, 0.99, 0.1, 2.0)))

        line.open(format)
        line.start()

        node.init(ctx)

        thread {
            while (ctx.time < seconds) {
                val buffer = renderSegment(node, 1024, ctx, channels)
                line.write(buffer, 0, buffer.size)
            }
        }

        Thread.sleep((seconds * 1000).toLong())

        line.drain()
        line.stop()
    }

    private fun renderSegment(audioNode: AudioNode, bufferSize: Int, ctx: Context, channels: Int = 2): ByteArray {
        val buffer = ByteArray(2 * bufferSize * channels)

        for (i in 0..<bufferSize) {
            val inputs = DoubleArray(0)
            val nodeOutputs = audioNode.process(ctx, inputs)
            for (channelIndex in 0..<channels) {
                val sample = nodeOutputs[channelIndex]
                val limitedSample = sample.coerceIn(-1.0, 1.0)
                val sampleInt = (limitedSample * Short.MAX_VALUE).toInt().toShort()
                val (b1, b2) = ByteConverter.shortToBytes(sampleInt, Endianness.LITTLE)
                buffer[i * channels * 2 + channelIndex * 2] = b1
                buffer[i * channels * 2 + channelIndex * 2 + 1] = b2
            }
            ctx.tick()
        }

        return buffer
    }
}