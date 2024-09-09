package midi

fun main() {
    val midi = MidiFile.readFromFile("test_data/simple.mid")

    for (track in midi.tracks) {
        for (event in track.events) {
            println(event)
        }
    }
}