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
        val effectsList = buildEffects(effects)
        val synth = buildSynth(synthName, params)
        instruments[name] = synth to effectsList
    }
    return instruments
}

fun buildSynth(synthName: String, params: JsonElement): Synth {
    if (synthName == "simpleSynth") {
        val envelopeSchema = ObjectSchema(
            "type" to StringSchema() to false, "params" to AnySchema() to false
        )

        val waveDataSchema = ObjectSchema(
            "type" to StringSchema() to false,
            "amplitude" to NumberSchema() to false,
            "phase" to NumberSchema() to true,
            "freqFactor" to NumberSchema() to true,
            "freqOffset" to NumberSchema() to true,
            "envelopes" to ArraySchema(envelopeSchema) to false
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
            val envelopes = data["envelopes"]!!.arr().elements.map { parseEnvelope(it) }
            val waveType = WaveType.entries.find { it.id == type }!!
            WaveData(waveType, amplitude, phase, freqFactor, freqOffset, envelopes)
        }
        val synth = SimpleSynth(vibrato, waves)
        return synth
    } else {
        throw IllegalArgumentException("Unknown synth $synthName")
    }
}

fun parseEnvelope(envelope: SchemaData): EnvelopeNode {
    val type = envelope["type"]!!.str().value
    val params = envelope["params"]!!.any().json
    return when (type) {
        "adsr" -> {
            val schema = ObjectSchema(
                "attack" to NumberSchema() to false,
                "decay" to NumberSchema() to false,
                "sustain" to NumberSchema() to false,
                "release" to NumberSchema() to false
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val attack = parsed["attack"]!!.num().value.toDouble()
            val decay = parsed["decay"]!!.num().value.toDouble()
            val sustain = parsed["sustain"]!!.num().value.toDouble()
            val release = parsed["release"]!!.num().value.toDouble()
            ADSREnvelope(attack, decay, sustain, release)
        }

        "polynomial" -> {
            val schema = ObjectSchema(
                "fadeTime" to NumberSchema() to false, "power" to NumberSchema() to false
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val fadeTime = parsed["fadeTime"]!!.num().value.toDouble()
            val power = parsed["power"]!!.num().value.toDouble()
            PolynomialEnvelope(fadeTime, power)
        }

        "fadeIn" -> {
            val schema = ObjectSchema("fadeTime" to NumberSchema() to false)
            val parsed = schema.safeConvert(params).throwIfErr()
            val fadeTime = parsed["fadeTime"]!!.num().value.toDouble()
            FadeInEnvelope(fadeTime)
        }

        else -> throw IllegalArgumentException("Unknown envelope type $type")
    }
}