package song

data class Note(val channel: Int, val key: Int, val beat: Double, val duration: Double, val velocity: Double) {
    override fun toString(): String {
        return "Note(channel=$channel, key=$key, beat=$beat, duration=$duration, velocity=$velocity)"
    }
}