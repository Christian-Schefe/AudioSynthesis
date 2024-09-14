package midi.raw

import java.io.File

fun main() {
    val inputFile = "test_data/beethoven_romance.mid"
    val outputFile = "test_data/test_output.mid"
    val rawMidi = RawMidi.readFromFile(inputFile)
    rawMidi.tracks[0].events.forEach { println(it) }
    rawMidi.writeToFile(outputFile)
    val midi2 = RawMidi.readFromFile(outputFile)

    compare_midi(rawMidi, midi2)
    println(compare_files(inputFile, outputFile))
}

fun compare_midi(midi1: RawMidi, midi2: RawMidi) {
    println("Header: ${midi1.headerChunk} == ${midi2.headerChunk}")
    println("Tracks: ${midi1.tracks.size} == ${midi2.tracks.size}")
    for (i in midi1.tracks.indices) {
        val track1 = midi1.tracks[i]
        val track2 = midi2.tracks[i]
        if (track1.events.size != track2.events.size) {
            println("Track $i: ${track1.events.size} != ${track2.events.size}")
        }
        for (j in track1.events.indices) {
            val event1 = track1.events[j].toString()
            val event2 = track2.events[j].toString()
            if (event1 != event2) println("Event $j: $event1 != $event2")
        }
    }

    val midi1str = midi1.toString()
    val midi2str = midi2.toString()
    if (midi1str != midi2str) println("Midi: $midi1str != $midi2str")
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