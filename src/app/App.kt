package app

import instruments.MixerNode
import nodes.*
import playback.AudioPlayer
import wav.*
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.time.measureTimedValue


fun app() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)

    val json = FileInputStream("data/songs/palace.json").readAllBytes().decodeToString()
    val (song, voiceData) = parseSong(json)

    val instruments = readInstruments()

    val mixer = MixerNode()

    for (data in voiceData) {
        val (instrumentSynth, instrumentEffects) = instruments[data.instrument]
            ?: throw IllegalArgumentException("Unknown instrument ${data.instrument}")

        val audioNode = instrumentSynth.buildAudio(song, data.noteFilter, ctx.random)
        val effects = buildEffects(data.effects)
        val node = applyEffects(applyEffects(audioNode, instrumentEffects), effects)

        mixer.addNode(node, data.volume, data.pan)
    }

    val clone = mixer.clone()
    val ctxClone = ctx.clone()

    val handle = thread {
        val wavFile = render(ctxClone, clone, song.duration() + 2)
        save(wavFile, "output.wav")
    }

    val player = AudioPlayer()
    player.renderAndPlay(mixer, ctx, song.duration() + 2)
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