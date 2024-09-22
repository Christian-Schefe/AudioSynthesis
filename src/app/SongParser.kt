package app

import instruments.NoteFilter
import midi.abstraction.Midi
import song.Song
import song.SongConverter
import util.json.*

class VoiceData(
    val noteFilter: NoteFilter,
    val instrument: String,
    val volume: Double,
    val pan: Double,
    val effects: List<SchemaData>
)

fun parseSong(json: String): Pair<Song, List<VoiceData>> {
    val schema = ObjectSchema(
        "midiFile" to StringSchema() to false, "tracks" to ArraySchema(
            ObjectSchema(
                "tracks" to ArraySchema(IntSchema()) to true,
                "channels" to ArraySchema(IntSchema()) to true,
                "notes" to ArraySchema(IntSchema()) to true,
                "instrument" to StringSchema() to false,
                "volume" to NumberSchema() to true,
                "pan" to NumberSchema() to true,
                "effects" to effectsSchema() to true
            )
        ) to false
    )
    val parsed = schema.safeParse(json).throwIfErr()

    val midiFile = parsed["midiFile"]!!.str().value
    val midi = Midi.readFromFile(midiFile)
    val song = SongConverter().fromMidi(midi)

    val tracks = parsed["tracks"]!!.arr().elements

    return song to tracks.map { track ->
        val trackNums = track["tracks"]?.arr()?.elements?.map { it.integer().value } ?: emptyList()
        val channels = track["channels"]?.arr()?.elements?.map { it.integer().value }
        val notes = track["notes"]?.arr()?.elements?.map { it.integer().value }
        val instrument = track["instrument"]!!.str().value
        val volume = track["volume"]?.num()?.value?.toDouble() ?: 1.0
        val pan = track["pan"]?.num()?.value?.toDouble() ?: 0.0
        val effectData = track["effects"]?.arr()?.elements ?: emptyList()
        VoiceData(NoteFilter(trackNums, channels, notes), instrument, volume, pan, effectData)
    }
}