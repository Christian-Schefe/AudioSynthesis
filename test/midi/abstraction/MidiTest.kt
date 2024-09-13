package midi.abstraction


fun main() {
    val inputFile = "test_data/Am_I_Blue_AB.mid"
    val outputFile = "test_data/test_output.mid"
    val midi = Midi.readFromFile(inputFile)
    midi.writeToFile(outputFile)
    println(midi)
}