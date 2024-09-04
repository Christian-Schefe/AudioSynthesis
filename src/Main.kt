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
        renderer.renderStereo(25.0, 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.PCM, samples, sampleRate
    ).withNormalizedSamples().buildWavFile()

    wavFile.writeToFile("output.wav")
}

