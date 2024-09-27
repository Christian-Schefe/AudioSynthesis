package song

class TrackMetadata(
    val trackName: String,
    val instrumentName: String,
    val textMessages: List<String>,
    val programChanges: List<Pair<String, Int>>
) {
    override fun toString(): String {
        return "TrackMetadata(trackName='$trackName', instrumentName='$instrumentName', textMessages=$textMessages, programChanges=$programChanges)"
    }
}

class Track(val metadata: TrackMetadata, notes: List<Note>) {
    val notes = notes.sortedBy { it.beat }

    override fun toString(): String {
        return "Track(metadata=$metadata, notes=$notes)"
    }

    fun duration(tempoTrack: TempoTrack): Double {
        val last = notes.lastOrNull() ?: return 0.0
        return tempoTrack.beatToTime(last.beat + last.duration)
    }
}