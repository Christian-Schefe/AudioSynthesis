package song

import midi.abstraction.*

class SongConverter {
    fun fromMidi(midi: Midi): Song {
        val tempoTrack = fromTempoChanges(midi.tickRate, midi.tempoChanges)
        val tracks = mutableListOf<Track>()
        for (track in midi.events) {
            val notes = mutableListOf<Note>()
            val activeKeys = mutableMapOf<Pair<Int, Int>, Pair<Int, Double>>()
            val trackName = track.events.firstNotNullOfOrNull { it.value as? TrackNameEvent }?.name ?: "Undefined"
            val instrumentName =
                track.events.firstNotNullOfOrNull { it.value as? InstrumentNameEvent }?.name ?: "Undefined"
            val textMessages = track.events.mapNotNull { (it.value as? TextEvent)?.text }
            val programChanges = track.events.mapNotNull {
                val p = (it.value as? ProgramChangeEvent)
                p?.let { mapProgramChangeToInstrument(p.program) to p.channel }
            }

            for ((time, event) in track.events) {
                if (event is NoteOnEvent) {
                    val channelKey = event.channel to event.key
                    if (activeKeys.containsKey(channelKey) && event.velocity == 0.0) {
                        val (startTime, velocity) = activeKeys[channelKey] ?: continue
                        val start = startTime.toDouble() / midi.tickRate
                        val duration = (time - startTime).toDouble() / midi.tickRate
                        notes.add(Note(event.channel, event.key, start, duration, velocity))
                        activeKeys.remove(channelKey)
                    } else if (event.velocity > 0.0) {
                        activeKeys[channelKey] = time to event.velocity
                    }
                } else if (event is NoteOffEvent) {
                    val channelKey = event.channel to event.key
                    val (startTime, velocity) = activeKeys[channelKey] ?: continue
                    val start = startTime.toDouble() / midi.tickRate
                    val duration = (time - startTime).toDouble() / midi.tickRate
                    notes.add(Note(event.channel, event.key, start, duration, velocity))
                    activeKeys.remove(channelKey)
                }
            }
            tracks.add(Track(TrackMetadata(trackName, instrumentName, textMessages, programChanges), notes))
        }
        return Song(tracks, tempoTrack)
    }

    private fun fromTempoChanges(tickRate: Int, tempoChanges: List<Timed<TempoChangeEvent>>): TempoTrack {
        return TempoTrack(tempoChanges.map { tempoChange ->
            TempoChange(tempoChange.time.toDouble() / tickRate, tempoChange.value.bpm)
        })
    }

    private fun mapProgramChangeToInstrument(program: Int): String {
        return when (program) {
            in 0..7 -> "Piano"
            in 8..15 -> "Chromatic Percussion"
            in 16..23 -> "Organ"
            in 24..31 -> "Guitar"
            in 32..39 -> "Bass"
            in 40..47 -> "Strings"
            in 48..55 -> "Ensemble"
            in 56..63 -> "Brass"
            in 64..71 -> "Reed"
            in 72..79 -> "Pipe"
            in 80..87 -> "Synth Lead"
            in 88..95 -> "Synth Pad"
            in 96..103 -> "Synth Effects"
            in 104..111 -> "Ethnic"
            in 112..119 -> "Percussive"
            in 120..127 -> "Sound Effects"
            else -> "Undefined"
        } + if (program in instruments.indices) "(${instruments[program]})" else "(${program + 1})"
    }

    private val instruments = arrayOf(
        "Acoustic Grand Piano",
        "Bright Acoustic Piano",
        "Electric Grand Piano",
        "Honky-tonk Piano",
        "Electric Piano 1 (Rhodes Piano)",
        "Electric Piano 2 (Chorused Piano)",
        "Harpsichord",
        "Clavinet",
        "Celesta",
        "Glockenspiel",
        "Music Box",
        "Vibraphone",
        "Marimba",
        "Xylophone",
        "Tubular Bells",
        "Dulcimer (Santur)",
        "Drawbar Organ (Hammond)",
        "Percussive Organ",
        "Rock Organ",
        "Church Organ",
        "Reed Organ",
        "Accordion (French)",
        "Harmonica",
        "Tango Accordion (Band neon)",
        "Acoustic Guitar (nylon)",
        "Acoustic Guitar (steel)",
        "Electric Guitar (jazz)",
        "Electric Guitar (clean)",
        "Electric Guitar (muted)",
        "Overdriven Guitar",
        "Distortion Guitar",
        "Guitar harmonics",
        "Acoustic Bass",
        "Electric Bass (fingered)",
        "Electric Bass (picked)",
        "Fretless Bass",
        "Slap Bass 1",
        "Slap Bass 2",
        "Synth Bass 1",
        "Synth Bass 2",
        "Violin",
        "Viola",
        "Cello",
        "Contrabass",
        "Tremolo Strings",
        "Pizzicato Strings",
        "Orchestral Harp",
        "Timpani",
        "String Ensemble 1 (strings)",
        "String Ensemble 2 (slow strings)",
        "SynthStrings 1",
        "SynthStrings 2",
        "Choir Aahs",
        "Voice Oohs",
        "Synth Voice",
        "Orchestra Hit",
        "Trumpet",
        "Trombone",
        "Tuba",
        "Muted Trumpet",
        "French Horn",
        "Brass Section",
        "SynthBrass 1",
        "SynthBrass 2",
        "Soprano Sax",
        "Alto Sax",
        "Tenor Sax",
        "Baritone Sax",
        "Oboe",
        "English Horn",
        "Bassoon",
        "Clarinet",
        "Piccolo",
        "Flute",
        "Recorder",
        "Pan Flute",
        "Blown Bottle",
        "Shakuhachi",
        "Whistle",
        "Ocarina",
        "Lead 1 (square wave)",
        "Lead 2 (sawtooth wave)",
        "Lead 3 (calliope)",
        "Lead 4 (chiffer)",
        "Lead 5 (charang)",
        "Lead 6 (voice solo)",
        "Lead 7 (fifths)",
        "Lead 8 (bass + lead)",
        "Pad 1 (new age Fantasia)",
        "Pad 2 (warm)",
        "Pad 3 (polysynth)",
        "Pad 4 (choir space voice)",
        "Pad 5 (bowed glass)",
        "Pad 6 (metallic pro)",
        "Pad 7 (halo)",
        "Pad 8 (sweep)",
        "FX 1 (rain)",
        "FX 2 (soundtrack)",
        "FX 3 (crystal)",
        "FX 4 (atmosphere)",
        "FX 5 (brightness)",
        "FX 6 (goblins)",
        "FX 7 (echoes, drops)",
        "FX 8 (sci-fi, star theme)",
        "Sitar",
        "Banjo",
        "Shamisen",
        "Koto",
        "Kalimba",
        "Bag pipe",
        "Fiddle",
        "Shanai",
        "Tinkle Bell",
        "Agogo",
        "Steel Drums",
        "Woodblock",
        "Taiko Drum",
        "Melodic Tom",
        "Synth Drum",
        "Reverse Cymbal",
        "Guitar Fret Noise",
        "Breath Noise",
        "Seashore",
        "Bird Tweet",
        "Telephone Ring",
        "Helicopter",
        "Applause",
        "Gunshot"
    )
}