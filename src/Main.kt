import nodes.*
import wav.*
import kotlin.random.Random

fun main() {
    val sampleRate = 44100

    val note = { flute() }
    val sequencer = InstrumentNode(note, 1)

    sequencer.addNote(60, 0.0, 2.5)

    val renderer = AudioRenderer(sequencer * 0.2, sampleRate)
    val samples = renderer.renderMono(15.0, 2)

    val wavFile = WavFile(
        AudioFormat.PCM, samples, sampleRate
    ).buildWavFile()

    wavFile.writeToFile("output.wav")
}

fun flute(): AudioNode {
    val phase = Random.nextDouble(0.0, 1.0)
    val sound = (triangleNode(initialPhase = phase) * 0.2) bus (sineNode(
        initialPhase = phase
    ) * 0.8)
    return adsrVolume(sound, 0.05, 0.0, 1.0, 0.5)
}