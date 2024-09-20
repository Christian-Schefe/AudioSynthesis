package song

class TempoTrack(tempoChanges: List<TempoChange>) {
    val tempoChanges = tempoChanges.sortedBy { it.beat }
    val tempoChangeMoments = computeTempoChangeMoments()

    fun computeTempoChangeMoments(): Map<TempoChange, Double> {
        val result = mutableMapOf<TempoChange, Double>()
        var time = 0.0
        var lastTempoChangeBeat = 0.0
        var lastTempo = 120.0
        for (tempoChange in tempoChanges) {
            val nextTime = time + (tempoChange.beat - lastTempoChangeBeat) * (60.0 / lastTempo)
            result[tempoChange] = nextTime
            time = nextTime
            lastTempoChangeBeat = tempoChange.beat
            lastTempo = tempoChange.bpm
        }
        return result
    }

    override fun toString(): String {
        return "TempoTrack(tempoChanges=$tempoChanges)"
    }

    fun beatToTime(beat: Double): Double {
        val tempoChange = tempoChanges.lastOrNull { it.beat <= beat } ?: TempoChange(0.0, 120.0)
        val lastTempoChangeMoment = tempoChangeMoments[tempoChange] ?: 0.0
        return lastTempoChangeMoment + (beat - tempoChange.beat) * (60.0 / tempoChange.bpm)
    }
}

data class TempoChange(val beat: Double, val bpm: Double) {
    override fun toString(): String {
        return "TempoChange: beat=$beat, tempo=$bpm"
    }
}