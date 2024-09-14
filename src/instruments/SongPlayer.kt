package instruments

import nodes.AudioNode
import nodes.Context
import song.Song

data class InstrumentSettings(val maxRelease: Double, val preRelease: Double)

class SongPlayer(
    val soundFactory: () -> AudioNode,
    val song: Song,
    val trackNumber: Int,
    voiceCount: Int,
    val instrumentSettings: InstrumentSettings
) : AudioNode(0, 2) {
    init {
        require(voiceCount > 0) { "Voice count must be greater than 0" }
        val testVoice = soundFactory()
        require(testVoice.outputCount == 2) { "Voice must have 2 outputs" }
        require(testVoice.inputCount == 3) { "Voice must have 3 inputs" }
    }

    val voices = Array(voiceCount) { soundFactory() }
    var freeVoicesSet = voices.indices.toMutableSet()
    var freeVoicesQueue = ArrayDeque(freeVoicesSet)
    val voiceData = Array<Triple<Double, Double, Double>?>(voiceCount) { null }
    var trackPointer = 0
    var tempoPointer = 0

    var beats = 0.0
    var bps = 2.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        checkNewNote()
        val result = processVoices(ctx)
        tick(ctx)
        return result
    }

    fun processVoices(ctx: Context): DoubleArray {
        val output = DoubleArray(2)
        for (i in voices.indices) {
            val voice = voices[i]
            val data = voiceData[i]
            if (data != null) {
                val (freq, velocity, endTime) = data
                if (beats < endTime - instrumentSettings.preRelease) {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(freq, 1.0, velocity))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                } else {
                    val voiceOutput = voice.process(ctx, doubleArrayOf(freq, 0.0, velocity))
                    output[0] += voiceOutput[0]
                    output[1] += voiceOutput[1]
                    if (freeVoicesSet.add(i)) {
                        freeVoicesQueue.add(i)
                    }
                }

                if (beats >= endTime + instrumentSettings.maxRelease) {
                    voiceData[i] = null
                }
            }
        }
        return output
    }

    fun tick(ctx: Context) {
        val tempoChanges = song.tempoTrack.tempoChanges
        if (tempoPointer < tempoChanges.size) {
            var tempoChange = tempoChanges[tempoPointer]
            while (tempoChange.time <= beats) {
                tempoPointer++
                if (tempoPointer >= tempoChanges.size) {
                    break
                }
                tempoChange = tempoChanges[tempoPointer]
            }
            bps = tempoChange.tempo / 60.0
        }

        val delta = 1.0 / ctx.sampleRate
        beats += delta * bps
    }

    fun checkNewNote() {
        val track = song.tracks[trackNumber]
        if (trackPointer >= track.notes.size) {
            return
        }

        var note = track.notes[trackPointer]
        while (note.time <= beats) {
            if (freeVoicesSet.isEmpty()) {
                break
            }
            val voice = freeVoicesQueue.removeFirst()
            freeVoicesSet.remove(voice)
            voices[voice].reset()

            voiceData[voice] = Triple(Instrument.midiNoteToFreq(note.key), note.velocity, note.time + note.duration)
            trackPointer++
            if (trackPointer >= track.notes.size) {
                break
            }
            note = track.notes[trackPointer]
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
        return SongPlayer(soundFactory, song, trackNumber, voices.size, instrumentSettings)
    }

    override fun init(ctx: Context) {
        voices.forEach { it.init(ctx) }
    }
}