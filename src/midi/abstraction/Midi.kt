package midi.abstraction

import midi.raw.*

class Midi(val events: List<List<Pair<Int, MidiEvent>>>, val tempoChanges: List<Pair<Int, TempoChangeEvent>>) {
    fun toRawMidi(): MidiFile {
        val division = TicksPerQuarterNote(480u)
        val format = FormatType.MULTIPLE_TRACKS
        var prevEventTime = 0
        val tracks = events.map { trackEvents ->
            val events = trackEvents.toList().sortedBy { it.first }.map { (time, event) ->
                val delta = time - prevEventTime
                prevEventTime = time
                val rawMidiEvent = event.toRawEvent()
                TrackEvent(delta.toUInt(), rawMidiEvent)
            }
            TrackChunk(events)
        }
        return MidiFile(format, division, tracks)
    }

    fun writeToFile(filePath: String) {
        toRawMidi().writeToFile(filePath)
    }

    companion object {
        fun fromRawMidi(file: MidiFile): Midi {
            val tracks = mutableListOf<List<Pair<Int, MidiEvent>>>()
            val tempoChanges = mutableListOf<Pair<Int, TempoChangeEvent>>()

            for (track in file.tracks) {
                val events = mutableListOf<Pair<Int, MidiEvent>>()
                var time = 0
                for (event in track.events) {
                    time += event.deltaTime.toInt()
                    when (val rawEvent = event.event) {
                        is midi.raw.NoteOnEvent -> events.add(
                            time to NoteOnEvent(
                                rawEvent.channel.toInt(), rawEvent.key.toInt(), rawEvent.velocity.toDouble() / 127
                            )
                        )

                        is midi.raw.NoteOffEvent -> events.add(
                            time to NoteOffEvent(rawEvent.channel.toInt(), rawEvent.key.toInt())
                        )

                        is SetTempoEvent -> tempoChanges.add(
                            time to TempoChangeEvent(60_000_000.0 / rawEvent.microsecondsPerQuarterNote.toInt())
                        )
                    }
                }
                tracks.add(events)
            }

            return Midi(tracks, tempoChanges)
        }

        fun readFromFile(filePath: String): Midi {
            return fromRawMidi(MidiFile.readFromFile(filePath))
        }
    }
}