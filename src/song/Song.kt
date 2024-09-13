package song

class Song(val tracks: List<Track>, val tempoTrack: TempoTrack) {
    override fun toString(): String {
        return "Song(tracks=$tracks, tempoTrack=$tempoTrack)"
    }

    fun duration(): Double {
        return tracks.map { it.duration() }.maxOrNull() ?: 0.0
    }
}