package midi

fun main() {
    val midi = MidiFile.readFromFile("test_data/Ambrosia.mid")

    for (track in midi.data.tracks) {
        for (event in track.events) {
            println(event)
        }
    }
}