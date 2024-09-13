package midi.raw

import java.io.File

fun main() {
    val inputFile = "test_data/simple.mid"
    val outputFile = "test_data/test_output.mid"
    val rawMidi = RawMidi.readFromFile(inputFile)
    println(rawMidi.tracks[1].size)
    rawMidi.writeToFile(outputFile)

    println(compare_files(inputFile, outputFile))
}

fun compare_files(file1: String, file2: String): Boolean {
    val f1 = File(file1)
    val f2 = File(file2)
    val bytes1 = f1.readBytes()
    val bytes2 = f2.readBytes()
    println("${bytes1.size} == ${bytes2.size}")
    for (i in bytes1.indices) {
        if (bytes1[i] != bytes2[i]) {
            println("Byte $i: ${bytes1[i]} != ${bytes2[i]}")
            return false
        }
    }
    return true
}