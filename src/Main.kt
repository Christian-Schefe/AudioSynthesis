import nodes.*
import playback.AudioPlayer
import wav.*
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.time.measureTimedValue


fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)

    val json = FileInputStream("songInfo/lonely.json").readAllBytes().decodeToString()
    val (song, voiceData) = parseSong(json)

    val instruments = readInstruments()

    var masterNode: AudioNode? = null

    for (data in voiceData) {
        val (instrumentSynth, instrumentEffects) = instruments[data.instrument]
            ?: throw IllegalArgumentException("Unknown instrument ${data.instrument}")

        val audioNode = instrumentSynth.buildAudio(song, data.noteFilter, ctx.random)
        val effects = buildEffects(data.effects)
        val node =
            applyEffects(applyEffects(audioNode, instrumentEffects), effects) gain data.volume pipe StereoPan(data.pan)

        if (masterNode == null) {
            masterNode = node
        } else {
            masterNode += node
        }
    }

    if (masterNode == null) {
        println("No tracks found in song")
        return
    }

    val clone = masterNode.clone()

    val handle = thread {
        val player = AudioPlayer()
        player.renderAndPlay(clone, ctx, song.duration() + 2)
    }

    val wavFile = render(ctx, masterNode, song.duration() + 2)
    save(wavFile, "output.wav")
    handle.join()
}

fun render(ctx: Context, node: AudioNode, duration: Double): WavFile {
    println("Rendering $duration seconds of audio...")

    val renderer = AudioRenderer(ctx, node)
    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(duration + 5, 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.IEEE_FLOAT, samples, ctx.sampleRate
    ).withNormalizedSamples(1.0)

    return wavFile
}

fun save(wavFile: WavFile, filename: String) {
    println("Writing $filename...")
    wavFile.writeToFile(filename)
    println("Done!")
}