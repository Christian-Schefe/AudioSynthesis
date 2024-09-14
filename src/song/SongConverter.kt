package song

import midi.abstraction.Midi
import midi.abstraction.NoteOnEvent
import midi.abstraction.TempoChangeEvent
import midi.abstraction.Timed

class SongConverter {
    fun fromMidi(midi: Midi): Song {
        val tempoTrack = fromTempoChanges(midi.tickRate, midi.tempoChanges)
        val tracks = mutableListOf<Track>()
        for (track in midi.events) {
            val notes = mutableListOf<Note>()
            val activeKeys = mutableMapOf<Int, Pair<Int, Double>>()
            for ((time, event) in track.events) {
                if (event is NoteOnEvent) {
                    if (activeKeys.containsKey(event.key) && event.velocity == 0.0) {
                        val (startTime, velocity) = activeKeys[event.key] ?: continue
                        val start = startTime.toDouble() / midi.tickRate
                        val duration = (time - startTime).toDouble() / midi.tickRate
                        notes.add(Note(event.key, start, duration, velocity))
                        activeKeys.remove(event.key)
                    } else if (event.velocity > 0.0) {
                        activeKeys[event.key] = time to event.velocity
                    }
                } else if (event is midi.abstraction.NoteOffEvent) {
                    val (startTime, velocity) = activeKeys[event.key] ?: continue
                    val start = startTime.toDouble() / midi.tickRate
                    val duration = (time - startTime).toDouble() / midi.tickRate
                    notes.add(Note(event.key, start, duration, velocity))
                    activeKeys.remove(event.key)
                }
            }
            tracks.add(Track(track.name, notes))
        }
        return Song(tracks, tempoTrack)
    }

    fun fromTempoChanges(tickRate: Int, tempoChanges: List<Timed<TempoChangeEvent>>): TempoTrack {
        return TempoTrack(tempoChanges.map { tempoChange ->
            TempoChange(tempoChange.time.toDouble() / tickRate, tempoChange.value.bpm)
        })
    }
}