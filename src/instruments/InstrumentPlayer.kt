package instruments

import nodes.AudioNode
import nodes.Context
import song.Song
import kotlin.math.pow
import kotlin.random.Random

data class InstrumentSettings(val maxRelease: Double, val preRelease: Double)
data class NoteFilter(val tracks: List<Int>, val channels: List<Int>?, val notes: List<Int>?)

class InstrumentPlayer(
    private val soundFactory: () -> AudioNode,
    private val random: Random,
    private val song: Song,
    private val noteFilter: NoteFilter,
    voiceCount: Int,
    private val instrumentSettings: InstrumentSettings
) : AudioNode(0, 2) {
    init {
        require(voiceCount > 0) { "Voice count must be greater than 0" }
        val testVoice = soundFactory()
        require(testVoice.outputCount == 2) { "Voice must have 2 outputs" }
        require(testVoice.inputCount == 4) { "Voice must have 3 inputs" }
    }

    private val voices = Array(voiceCount) { soundFactory() }
    private var freeVoicesSet = voices.indices.toMutableSet()
    private var freeVoicesQueue = ArrayDeque(freeVoicesSet)
    private val voiceData = Array<DoubleArray?>(voiceCount) { null }
    private var trackPointer = 0
    private var tempoPointer = 0

    private var beats = 0.0
    private var time = 0.0
    private var bps = 2.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        checkNewNote()
        val result = processVoices(ctx)
        tick(ctx)
        return result
    }

    private fun processVoices(ctx: Context): DoubleArray {
        val output = DoubleArray(2)
        for (i in voices.indices) {
            val voice = voices[i]
            val data = voiceData[i]
            if (data != null) {
                val (freq, velocity, endTime, randVal) = data
                if (time < endTime - instrumentSettings.preRelease) {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(freq, 1.0, velocity, randVal))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                } else {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(freq, 0.0, velocity, randVal))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                    if (freeVoicesSet.add(i)) {
                        freeVoicesQueue.add(i)
                    }
                }

                if (time >= endTime - instrumentSettings.preRelease + instrumentSettings.maxRelease) {
                    voiceData[i] = null
                }
            }
        }
        return output
    }

    private fun tick(ctx: Context) {
        val tempoChanges = song.tempoTrack.tempoChanges
        if (tempoPointer < tempoChanges.size) {
            var tempoChange = tempoChanges[tempoPointer]
            while (tempoChange.beat <= beats) {
                tempoPointer++
                if (tempoPointer >= tempoChanges.size) {
                    break
                }
                tempoChange = tempoChanges[tempoPointer]
            }
            bps = tempoChange.bpm / 60.0
        }

        beats += ctx.timeStep * bps
        time += ctx.timeStep
    }

    private fun checkNewNote() {
        for (trackNumber in noteFilter.tracks) {
            val track = song.tracks[trackNumber]
            if (trackPointer >= track.notes.size) {
                return
            }

            var note = track.notes[trackPointer]
            while (note.beat <= beats) {
                if ((noteFilter.channels == null || note.channel in noteFilter.channels) && (noteFilter.notes == null || note.key in noteFilter.notes)) {
                    if (freeVoicesSet.isEmpty()) {
                        break
                    }
                    val voice = freeVoicesQueue.removeFirst()
                    freeVoicesSet.remove(voice)
                    voices[voice].reset()

                    val endBeat = note.beat + note.duration
                    val endTime = song.tempoTrack.beatToTime(endBeat)
                    voiceData[voice] = doubleArrayOf(
                        midiNoteToFreq(note.key), note.velocity, endTime, random.nextDouble()
                    )
                }
                trackPointer++
                if (trackPointer >= track.notes.size) {
                    break
                }
                note = track.notes[trackPointer]
            }
        }
    }

    override fun reset() {
        voiceData.fill(null)
        beats = 0.0
        voices.forEach { it.reset() }
        freeVoicesSet = voices.indices.toMutableSet()
        freeVoicesQueue = ArrayDeque(freeVoicesSet)
    }

    override fun clone(): AudioNode {
        return InstrumentPlayer(soundFactory, random, song, noteFilter, voices.size, instrumentSettings)
    }

    override fun init(ctx: Context) {
        voices.forEach { it.init(ctx) }
    }

    private fun midiNoteToFreq(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69.0) / 12.0)
    }
}