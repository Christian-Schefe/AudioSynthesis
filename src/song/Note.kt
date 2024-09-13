package song

data class Note(val key: Int, val time: Double, val duration: Double, val velocity: Double) {
    override fun toString(): String {
        return "Note(key=$key, time=$time, duration=$duration, velocity=$velocity)"
    }
}