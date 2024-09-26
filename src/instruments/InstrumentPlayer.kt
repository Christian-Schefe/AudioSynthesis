package instruments

import nodes.AudioNode
import nodes.Context
import song.Song
import kotlin.math.pow
import kotlin.random.Random

fun interface ReleaseTimeEvaluator {
    fun timeToSilence(releaseMoment: Double): Double
}

data class InstrumentSettings(val maxRelease: Double, val maxDecay: Double)
data class NoteFilter(val tracks: List<Int>, val channels: List<Int>?, val notes: List<Int>?)

class InstrumentPlayer(
    private val soundFactory: () -> AudioNode,
    private val random: Random,
    private val song: Song,
    private val noteFilter: NoteFilter,
    voiceCount: Int,
    private val releaseTimeEvaluator: ReleaseTimeEvaluator
) : AudioNode(0, 2) {
    init {
        require(voiceCount > 0) { "Voice count must be greater than 0" }
        val testVoice = soundFactory()
        require(testVoice.outputCount == 2) { "Voice must have 2 outputs" }
        require(testVoice.inputCount == 4) { "Voice must have 3 inputs" }
    }

    private data class VoiceData(
        val freq: Double, val velocity: Double, val releaseTime: Double, val endTime: Double, val randVal: Double
    )

    private val voices = Array(voiceCount) { soundFactory() }
    private var freeVoicesSet = voices.indices.toMutableSet()
    private var freeVoicesQueue = ArrayDeque(freeVoicesSet)
    private val voiceData = Array<VoiceData?>(voiceCount) { null }
    private var trackPointers = IntArray(noteFilter.tracks.size)

    private var time = 0.0

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
                if (time < data.releaseTime) {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(data.freq, 1.0, data.velocity, data.randVal))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                } else {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(data.freq, 0.0, data.velocity, data.randVal))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                    if (freeVoicesSet.add(i)) {
                        freeVoicesQueue.add(i)
                    }
                }

                if (time >= data.endTime) {
                    voiceData[i] = null
                    if (freeVoicesSet.add(i)) {
                        freeVoicesQueue.add(i)
                    }
                }
            }
        }
        return output
    }

    private fun tick(ctx: Context) {
        time += ctx.timeStep
    }

    private fun checkNewNote() {
        for (i in noteFilter.tracks.indices) {
            val trackNumber = noteFilter.tracks[i]
            val track = song.tracks[trackNumber]
            if (trackPointers[i] >= track.notes.size) {
                continue
            }

            var note = track.notes[trackPointers[i]]
            var noteStartTime = song.tempoTrack.beatToTime(note.beat)
            while (noteStartTime <= time) {
                val allowedChannel = noteFilter.channels == null || note.channel in noteFilter.channels
                val allowedNote = noteFilter.notes == null || note.key in noteFilter.notes
                if (allowedChannel && allowedNote) {
                    if (freeVoicesSet.isEmpty()) {
                        println("Warning: No free voices available")
                        break
                    }
                    val voice = freeVoicesQueue.removeFirst()
                    freeVoicesSet.remove(voice)
                    voices[voice].reset()

                    val endBeat = note.beat + note.duration
                    val releaseTime = song.tempoTrack.beatToTime(endBeat)
                    val releaseMoment = releaseTime - noteStartTime
                    val endTime = releaseTime + releaseTimeEvaluator.timeToSilence(releaseMoment)

                    voiceData[voice] = VoiceData(
                        midiNoteToFreq(note.key), note.velocity, releaseTime, endTime, random.nextDouble()
                    )
                }
                trackPointers[i]++
                if (trackPointers[i] >= track.notes.size) {
                    break
                }
                note = track.notes[trackPointers[i]]
                noteStartTime = song.tempoTrack.beatToTime(note.beat)
            }
        }
    }

    override fun reset() {
        voiceData.fill(null)
        freeVoicesSet = voices.indices.toMutableSet()
        freeVoicesQueue = ArrayDeque(freeVoicesSet)
        trackPointers.fill(0)
        time = 0.0
        voices.forEach { it.reset() }
    }

    override fun clone(): AudioNode {
        return InstrumentPlayer(soundFactory, random, song, noteFilter, voices.size, releaseTimeEvaluator)
    }

    override fun init(ctx: Context) {
        voices.forEach { it.init(ctx) }
    }

    private fun midiNoteToFreq(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69.0) / 12.0)
    }
}