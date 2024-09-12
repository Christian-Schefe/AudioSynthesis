package midi.abstraction

import midi.raw.*
import util.OldBitConverter

class Midi(val events: List<List<Pair<Int, MidiEvent>>>, val tempoChanges: List<Pair<Int, TempoChangeEvent>>) {
    fun toRawMidi(): RawMidi {
        val division = Division.TicksPerQuarterNote(480)
        val format = MidiFileFormat.MULTIPLE_TRACKS
        var prevEventTime = 0
        val tracks = events.map { trackEvents ->
            val events = trackEvents.toList().sortedBy { it.first }.map { (time, event) ->
                val delta = time - prevEventTime
                prevEventTime = time
                val rawMidiEvent = event.toRawEvent(delta)
                rawMidiEvent
            }
            RawTrackChunk(events)
        }
        return RawMidi(format, division, tracks)
    }

    fun writeToFile(filePath: String) {
        toRawMidi().writeToFile(filePath)
    }

    companion object {
        fun fromRawMidi(file: RawMidi): Midi {
            val tracks = mutableListOf<List<Pair<Int, MidiEvent>>>()
            val tempoChanges = mutableListOf<Pair<Int, TempoChangeEvent>>()

            for (track in file.tracks) {
                val events = mutableListOf<Pair<Int, MidiEvent>>()
                var time = 0
                for (event in track.events) {
                    time += event.deltaTime
                    when (event) {
                        is RawChannelVoiceMessage -> {
                            when (event.status) {
                                ChannelMessageStatus.NOTE_ON -> {
                                    val key = event.data[0].toInt()
                                    val velocity = event.data[1].toInt() / 127.0
                                    events.add(time to NoteOnEvent(event.channel.toInt(), key, velocity))
                                }

                                ChannelMessageStatus.NOTE_OFF -> {
                                    val key = event.data[0].toInt()
                                    events.add(time to NoteOffEvent(event.channel.toInt(), key))
                                }

                                else -> {
                                    // Ignore other channel messages
                                }
                            }
                        }

                        is RawMetaEvent -> {
                            when (event.type) {
                                MetaEventStatus.SET_TEMPO -> {
                                    val bpm = 60_000_000.0 / OldBitConverter.bitsToInt(event.data)
                                    tempoChanges.add(time to TempoChangeEvent(bpm))
                                }

                                else -> {
                                    // Ignore other meta events
                                }
                            }
                        }

                        else -> {
                            // Ignore other events
                        }
                    }
                }
                tracks.add(events)
            }

            return Midi(tracks, tempoChanges)
        }

        fun readFromFile(filePath: String): Midi {
            return fromRawMidi(RawMidi.readFromFile(filePath))
        }
    }
}