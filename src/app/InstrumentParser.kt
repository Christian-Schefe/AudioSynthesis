package app

import effects.Effect
import instruments.*
import util.json.*
import java.io.File
import java.io.FileInputStream

fun readInstrumentFiles(folder: String): List<String> {
    val files = mutableListOf<String>()
    val dir = File(folder)
    for (file in dir.listFiles()!!) {
        if (file.isFile && file.extension == "json") {
            files.add(file.absolutePath)
        }
    }
    return files.map { FileInputStream(it).readAllBytes().decodeToString() }
}

fun readInstruments(): Map<String, Pair<Synth, List<Effect>>> {
    val json = readInstrumentFiles("data/instruments")
    val schema = ObjectSchema(
        "name" to StringSchema() to false,
        "envelopes" to AnySchema() to false,
        "synth" to StringSchema() to false,
        "params" to AnySchema() to false,
        "effects" to effectsSchema() to true
    )

    val parsed = json.mapNotNull { schema.safeParse(it).ok() }

    val instruments = mutableMapOf<String, Pair<Synth, List<Effect>>>()
    for (instrument in parsed) {
        val name = instrument["name"]!!.str().value
        val synthName = instrument["synth"]!!.str().value
        val params = instrument["params"]!!.any().json
        val effects = instrument["effects"]?.arr()?.elements ?: emptyList()
        val envelopeJson = instrument["envelopes"]!!.any().json
        val envelopes = parseEnvelopes(envelopeJson)
        val effectsList = buildEffects(effects)
        val synth = buildSynth(synthName, params, envelopes)
        instruments[name] = synth to effectsList
    }
    return instruments
}

fun buildSynth(synthName: String, params: JsonElement, envelopeMap: Map<String, () -> EnvelopeNode>): Synth {
    if (synthName == "simpleSynth") {
        val waveDataSchema = ObjectSchema(
            "type" to StringSchema() to false,
            "amplitude" to NumberSchema() to false,
            "phase" to NumberSchema() to true,
            "freqFactor" to NumberSchema() to true,
            "freqOffset" to NumberSchema() to true,
            "envelopes" to ArraySchema(StringSchema()) to false
        )

        val schema = ObjectSchema(
            "vibrato" to NumberSchema() to false, "waves" to ArraySchema(waveDataSchema) to false
        )

        val parsed = schema.safeConvert(params).throwIfErr()
        val vibrato = parsed["vibrato"]!!.num().value.toDouble()
        val wave = parsed["waves"]!!.arr().elements
        val waves = wave.map { data ->
            val type = data.obj()["type"]!!.str().value
            val amplitude = data["amplitude"]!!.num().value.toDouble()
            val phase = data["phase"]?.num()?.value?.toDouble() ?: 0.0
            val freqFactor = data["freqFactor"]?.num()?.value?.toDouble() ?: 1.0
            val freqOffset = data["freqOffset"]?.num()?.value?.toDouble() ?: 0.0
            val envelopes = data["envelopes"]!!.arr().elements.map { envelopeMap[it.str().value]!!() }
            val waveType =
                WaveType.entries.find { it.id == type } ?: throw IllegalArgumentException("Unknown wave type $type")
            WaveData(waveType, amplitude, phase, freqFactor, freqOffset, envelopes)
        }
        val synth = SimpleSynth(vibrato, waves)
        return synth
    } else {
        throw IllegalArgumentException("Unknown synth $synthName")
    }
}