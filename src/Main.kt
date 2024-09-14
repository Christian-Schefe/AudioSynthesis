import instruments.SongPlayer
import instruments.*
import midi.abstraction.Midi
import nodes.*
import song.SongConverter
import wav.*
import kotlin.time.measureTimedValue

fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)
    val midi = Midi.readFromFile("test_data/spring_rain.mid")
    val song = SongConverter().fromMidi(midi)

    var node: AudioNode? = null

    val factories = mapOf(
        0 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
        1 to (InstrumentSettings(1.0, 0.15) to { violin(ctx.random) }),
        2 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
        3 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
    )

    /*val factories = mutableMapOf<Int, Pair<InstrumentSettings, () -> AudioNode>>()
    for (i in 0 until song.tracks.size) {
        factories[i] = InstrumentSettings(0.5, 0.0) to { piano(ctx.random) }
    }*/

    var duration = 0.0

    for ((track, data) in factories) {
        val (settings, factory) = data
        println("Track $track: ${song.tracks[track].name} with ${song.tracks[track].notes.size} notes")
        val trackNode = SongPlayer(factory, song, track, 36, settings)
        if (node == null) {
            node = trackNode
        } else {
            node += trackNode
        }
        duration = maxOf(duration, song.tracks[track].duration(song.tempoTrack))
        //println("notes: ${song.tracks[track].notes}")
    }

    if (node == null) {
        println("No tracks found in song")
        return
    }

    val renderer = AudioRenderer(ctx, node, sampleRate)

    println("Rendering ${song.tracks.size} tracks with $duration seconds of audio...")

    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(duration + 5, 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.IEEE_FLOAT, samples, sampleRate.toUInt()
    ).withNormalizedSamples(1.0)

    println("Writing output.wav...")
    wavFile.writeToFile("output.wav")
    println("Done!")
}

