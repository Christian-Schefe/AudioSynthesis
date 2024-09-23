package midi.abstraction

import midi.raw.*

data class Timed<out T>(val time: Int, val value: T)

class MidiTrack(val name: String, val events: List<Timed<MidiEvent>>)

class Midi(
    val tickRate: Int, val events: List<MidiTrack>, val tempoChanges: List<Timed<TempoChangeEvent>>
) {
    fun toRawMidi(): RawMidi {
        val division = Division.TicksPerQuarterNote(tickRate.toShort())
        val format = MidiFileFormat.MULTIPLE_TRACKS
        val tracks = mutableListOf<RawTrackChunk>()
        for (i in events.indices) {
            val eventList = mutableListOf<Timed<MidiEvent>>()
            eventList.addAll(events[i].events)
            if (i == 0) {
                eventList.addAll(tempoChanges)
            }
            eventList.sortBy { it.time }

            val trackEvents = mutableListOf<RawMessage>()
            var prevEventTime = 0
            for (event in eventList) {
                val deltaTime = event.time - prevEventTime
                prevEventTime = event.time
                trackEvents.add(event.value.toRawEvent(deltaTime))
            }
            tracks.add(RawTrackChunk(trackEvents))
        }
        return RawMidi(format, division, tracks)
    }

    fun writeToFile(filePath: String) {
        toRawMidi().writeToFile(filePath)
    }

    override fun toString(): String {
        return "Midi(events=$events, tempoChanges=$tempoChanges)"
    }

    companion object {
        fun fromRawMidi(file: RawMidi): Midi {
            val tracks = mutableListOf<MidiTrack>()
            val tempoChanges = mutableListOf<Timed<TempoChangeEvent>>()

            for (track in file.tracks) {
                val events = mutableListOf<Timed<MidiEvent>>()
                var time = 0
                var trackName = "Undefined"
                for (event in track.events) {
                    time += event.deltaTime
                    MidiEvent.fromRawMessage(event)?.let { midiEvent ->
                        if (midiEvent is TempoChangeEvent) {
                            tempoChanges.add(Timed(time, midiEvent))
                        } else {
                            events.add(Timed(time, midiEvent))
                        }
                        if (midiEvent is TrackNameEvent) {
                            trackName = midiEvent.name
                        }
                    }
                }
                tracks.add(MidiTrack(trackName, events))
            }

            val tickRate = file.headerChunk.division.getTickRate()

            return Midi(tickRate, tracks, tempoChanges)
        }

        fun readFromFile(filePath: String): Midi {
            return fromRawMidi(RawMidi.readFromFile(filePath))
        }
    }
}