package song

class Track(val name: String, notes: List<Note>) {
    val notes = notes.sortedBy { it.beat }

    override fun toString(): String {
        return "Track(notes=$notes)"
    }

    fun duration(tempoTrack: TempoTrack): Double {
        val last = notes.lastOrNull() ?: return 0.0
        return tempoTrack.beatToTime(last.beat + last.duration)
    }
}