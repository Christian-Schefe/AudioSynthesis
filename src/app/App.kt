package app

import nodes.MixerNode
import nodes.*
import playback.AudioPlayer
import song.Song
import wav.*
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlin.time.toDuration


fun app() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)

    val path = askInput(scanSongs("data/songs"))

    val json = FileInputStream(path).readAllBytes().decodeToString()
    val (song, mixer) = parse(ctx, json)

    val mixerClone = mixer.clone()
    val ctxClone = ctx.clone()
    val playDuration = song.duration() + 2


    val handle = thread {
        val shouldRender = askShouldRender()
        if (shouldRender) {
            val wavFile = render(ctxClone, mixerClone, playDuration)
            save(wavFile, "output.wav")
        }
    }

    val player = AudioPlayer()
    player.renderAndPlay(mixer, ctx, playDuration)
    handle.join()
}

fun parse(ctx: Context, json: String): Pair<Song, MixerNode> {
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
    return song to mixer
}

fun printSongInfo(song: Song) {
    println("Song duration: ${song.duration()} seconds")
    for (i in song.tracks.indices) {
        val track = song.tracks[i]
        println("Track $i (${track.metadata}): ${track.notes.size} notes")
    }
}

fun render(ctx: Context, node: AudioNode, duration: Double): WavFile {
    val dur = duration.toDuration(DurationUnit.SECONDS)
    println("Rendering $dur of audio...")

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