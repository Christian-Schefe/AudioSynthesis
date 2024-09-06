import instruments.Flute
import nodes.*
import wav.*
import kotlin.time.measureTimedValue

fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)
    val flute = Flute(ctx.random)

    flute.addNote(60, 0.0, 5.0, 0.5)
    flute.addNote(64, 1.0, 1.0, 0.5)
    flute.addNote(67, 2.0, 1.0, 0.5)
    flute.addNote(65, 3.0, 1.0, 0.5)
    flute.addNote(64, 4.0, 1.0, 0.5)
    flute.addNote(62, 5.0, 1.0, 0.5)
    flute.addNote(60, 6.0, 1.0, 0.5)
    flute.addNote(60, 7.0, 2.0, 0.5)
    flute.addNote(64, 8.0, 2.0, 0.5)
    flute.addNote(67, 9.0, 2.0, 0.5)
    flute.addNote(65, 10.0, 2.0, 0.5)
    flute.addNote(64, 11.0, 2.0, 0.5)
    flute.addNote(62, 12.0, 2.0, 0.5)
    flute.addNote(60, 13.0, 2.0, 0.5)
    flute.addNote(50, 14.0, 1.0, 0.5)
    flute.addNote(54, 15.0, 1.0, 0.5)
    flute.addNote(57, 16.0, 1.0, 0.5)
    flute.addNote(55, 17.0, 1.0, 0.5)
    flute.addNote(54, 18.0, 1.0, 0.5)
    flute.addNote(52, 19.0, 1.0, 0.5)
    flute.addNote(50, 20.0, 1.0, 0.5)

    val renderer = AudioRenderer(ctx, flute, sampleRate)

    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(5.0, 2)
    }

    println("Time taken: $timeTaken")

    /*val samples = arrayOf(
        doubleArrayOf(0.0, 1.0, -0.5, 0.33, -1.0),
        doubleArrayOf(1.0, 0.1, -0.2, 0.3, -1.0),
        doubleArrayOf(1.0, 0.1, -0.2, 0.3, -1.0)
    )*/

    val wavFile = WavFile(
        AudioFormat.MU_LAW, samples, sampleRate.toUInt()
    ).withNormalizedSamples(1.0)

    wavFile.writeToFile("output.wav")

    val wavFile2 = WavFile.readFromFile("output.wav")

    for (i in 0..<wavFile.samples.size) {
        for (j in 0..<wavFile.samples[i].size) {
            if (wavFile.samples[i][j] - wavFile2.samples[i][j] > 1e-2) {
                println("Mismatch at $i, $j: ${wavFile.samples[i][j]} != ${wavFile2.samples[i][j]}")
            }
        }
    }

    for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
        val decoded = decodeMuLaw(i.toByte())
        val encoded = encodeMuLaw(decoded)
        if (encoded != i.toByte()) println("Mismatch at $i: $decoded, $encoded")
    }
}

