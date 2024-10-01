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
        } else if (file.isDirectory) {
            files.addAll(readInstrumentFiles(file.absolutePath))
        }
    }
    return files
}

fun readInstruments(): Map<String, Pair<Synth, List<Effect>>> {
    val json = readInstrumentFiles("data/instruments").map { FileInputStream(it).readAllBytes().decodeToString() }
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
            "settings" to AnySchema() to false,
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
            val type = data.obj()["type"]!!.str().value.lowercase()
            val settings = data.obj()["settings"]!!.any().json
            val freqFactor = data["freqFactor"]?.num()?.value?.toDouble() ?: 1.0
            val freqOffset = data["freqOffset"]?.num()?.value?.toDouble() ?: 0.0
            val envelopes = data["envelopes"]!!.arr().elements.map { envelopeMap[it.str().value]!!() }

            if (SimpleWaveType.entries.find { it.id == type } != null) {
                val settingsSchema = ObjectSchema(
                    "amplitude" to NumberSchema() to false, "phase" to NumberSchema() to true
                )
                val waveType = SimpleWaveType.entries.find { it.id == type }!!
                val settingsParsed = settingsSchema.safeConvert(settings).throwIfErr()
                val amplitude = settingsParsed["amplitude"]!!.num().value.toDouble()
                val phase = settingsParsed["phase"]?.num()?.value?.toDouble() ?: 0.0
                WaveComponent(SimpleWaveData(waveType, amplitude, phase), freqFactor, freqOffset, envelopes)
            } else if (NoiseType.entries.find { it.id == type } != null) {
                val noiseType = NoiseType.entries.find { it.id == type }!!
                val settingsSchema = ObjectSchema("amplitude" to NumberSchema() to false)
                val settingsParsed = settingsSchema.safeConvert(settings).throwIfErr()
                val amplitude = settingsParsed["amplitude"]!!.num().value.toDouble()
                WaveComponent(NoiseWaveData(noiseType, amplitude), freqFactor, freqOffset, envelopes)
            } else if (type == "fm") {
                val settingsSchema = ObjectSchema(
                    "amplitude" to NumberSchema() to false,
                    "modulationIndex" to NumberSchema() to false,
                    "modulationFreqFactor" to NumberSchema() to false
                )
                val settingsParsed = settingsSchema.safeConvert(settings).throwIfErr()
                val amplitude = settingsParsed["amplitude"]!!.num().value.toDouble()
                val modulationIndex = settingsParsed["modulationIndex"]!!.num().value.toDouble()
                val modulationFreqFactor = settingsParsed["modulationFreqFactor"]!!.num().value.toDouble()
                WaveComponent(
                    FMWaveData(amplitude, modulationIndex, modulationFreqFactor), freqFactor, freqOffset, envelopes
                )
            } else {
                throw IllegalArgumentException("Unknown wave type $type")
            }
        }
        val synth = SimpleSynth(vibrato, waves)
        return synth
    } else {
        throw IllegalArgumentException("Unknown synth $synthName")
    }
}