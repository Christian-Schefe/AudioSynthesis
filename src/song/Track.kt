package song

class Track(notes: List<Note>) {
    val notes = notes.sortedBy { it.time }

    override fun toString(): String {
        return "Track(notes=$notes)"
    }

    fun duration(): Double {
        return notes.map { it.time + it.duration }.maxOrNull() ?: 0.0
    }
}