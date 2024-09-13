package song

import midi.abstraction.Midi

fun main() {
    val inputFile = "test_data/Am_I_Blue_AB.mid"
    //val inputFile = "test_data/simple.mid"
    val midi = Midi.readFromFile(inputFile)
    val song = SongConverter().fromMidi(midi)

    println(song)
}