package app

import instruments.MixerNode
import nodes.*
import playback.AudioPlayer
import song.Song
import wav.*
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.time.measureTimedValue


fun app() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)

    val json = FileInputStream("data/songs/castle.json").readAllBytes().decodeToString()
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

    printSongInfo(song)

    val clone = mixer.clone()
    val ctxClone = ctx.clone()
    val playDuration = song.duration() + 2

    val handle = thread {
        val wavFile = render(ctxClone, clone, playDuration)
        save(wavFile, "output.wav")
    }

    val player = AudioPlayer()
    player.renderAndPlay(mixer, ctx, playDuration)
    handle.join()
}

fun printSongInfo(song: Song) {
    println("Song duration: ${song.duration()} seconds")
    for (i in song.tracks.indices) {
        val track = song.tracks[i]
        println("Track $i (${track.name}): ${track.notes.size} notes")
    }
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