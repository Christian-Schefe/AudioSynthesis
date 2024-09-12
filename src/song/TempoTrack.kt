package song

class TempoTrack(tempoChanges: List<TempoChange>) {
    val tempoChanges = tempoChanges.sortedBy { it.time }
}

data class TempoChange(val time: Double, val tempo: Double) {
    override fun toString(): String {
        return "TempoChange: time=$time, tempo=$tempo"
    }
}