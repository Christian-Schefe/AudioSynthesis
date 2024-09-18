package song

class TempoTrack(tempoChanges: List<TempoChange>) {
    val tempoChanges = tempoChanges.sortedBy { it.time }

    override fun toString(): String {
        return "TempoTrack(tempoChanges=$tempoChanges)"
    }

    fun beatToTime(beat: Double): Double {
        var time = 0.0
        var lastTime = 0.0
        var lastTempo = 120.0
        for (tempoChange in tempoChanges) {
            val nextTime = lastTime + (tempoChange.time - time) * lastTempo / 60.0
            if (nextTime >= beat) {
                return time + (beat - time) * lastTempo / 60.0
            }
            time = nextTime
            lastTime = tempoChange.time
            lastTempo = tempoChange.tempo
        }
        return time + (beat - time) * lastTempo / 60.0
    }
}

data class TempoChange(val time: Double, val tempo: Double) {
    override fun toString(): String {
        return "TempoChange: time=$time, tempo=$tempo"
    }
}