import instruments.SongPlayer
import instruments.flute
import instruments.piano
import midi.abstraction.Midi
import nodes.*
import song.SongConverter
import wav.*
import kotlin.time.measureTimedValue

fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)
    val midi = Midi.readFromFile("test_data/Etude_Heroique.mid")
    val song = SongConverter().fromMidi(midi)

    var node: AudioNode? = null

    for (track in song.tracks.indices) {
        val trackNode = SongPlayer({ piano(ctx.random) }, song, track, 36)
        if (node == null) {
            node = trackNode
        } else {
            node += trackNode
        }
    }

    if (node == null) {
        println("No tracks found in song")
        return
    }

    val renderer = AudioRenderer(ctx, node, sampleRate)

    println("Rendering ${song.tracks.size} tracks with ${song.duration()} seconds of audio...")

    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(song.duration(), 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.IEEE_FLOAT, samples, sampleRate.toUInt()
    ).withNormalizedSamples(1.0)

    wavFile.writeToFile("output.wav")
}

