package song

class Track(notes: List<Note>) {
    val notes = notes.sortedBy { it.time }
}