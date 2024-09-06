package midi

import util.ByteWriter
import util.Endianness
import java.io.FileOutputStream

class MidiFile(private val data: MidiFileData) {
    private fun buildMidiFileData(): MidiFileData {
        return data
    }

    fun writeToFile(filePath: String) {
        val byteWriter = ByteWriter(Endianness.LITTLE)

        buildMidiFileData().write(byteWriter)

        val fileOutputStream = FileOutputStream(filePath)
        fileOutputStream.write(byteWriter.toByteArray())
        fileOutputStream.close()
    }

    companion object {
        private fun fromMidiFileData(midiFileData: MidiFileData): MidiFile {
            return MidiFile(midiFileData)
        }

        fun readFromFile(filePath: String): MidiFile {
            val reader = MidiFileReader()
            val midiFileData = reader.readFromFile(filePath)
            return fromMidiFileData(midiFileData)
        }
    }
}