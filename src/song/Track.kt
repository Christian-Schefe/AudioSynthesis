package song

class Track(val name: String, notes: List<Note>) {
    val notes = notes.sortedBy { it.beat }

    override fun toString(): String {
        return "Track(notes=$notes)"
    }

    fun duration(tempoTrack: TempoTrack): Double {
        val timeAtTempoChange = DoubleArray(tempoTrack.tempoChanges.size)

        for (i in 0..<tempoTrack.tempoChanges.size) {
            val prev = tempoTrack.tempoChanges.getOrNull(i - 1)
            val beatsSinceLastChange = tempoTrack.tempoChanges[i].beat - (prev?.beat ?: 0.0)
            val lastTempo = prev?.bpm ?: 120.0
            val timeSinceLastChange = beatsSinceLastChange * (60.0 / lastTempo)

            val prevTime = timeAtTempoChange.getOrNull(i - 1) ?: 0.0
            timeAtTempoChange[i] = prevTime + timeSinceLastChange
        }

        return notes.map {
            val endTime = it.beat + it.duration
            val lastTempoChange = tempoTrack.tempoChanges.indexOfLast { tempo -> tempo.beat <= endTime }
            val beatsSinceTempoChange = endTime - tempoTrack.tempoChanges[lastTempoChange].beat
            val lastTempo = tempoTrack.tempoChanges[lastTempoChange].bpm
            val timeSinceTempoChange = beatsSinceTempoChange / lastTempo * 60.0
            timeAtTempoChange[lastTempoChange] + timeSinceTempoChange
        }.maxOrNull() ?: 0.0
    }
}