import effects.Effect
import instruments.*
import util.json.*
import java.io.FileInputStream


fun readInstruments(): Map<String, Pair<Synth, List<Effect>>> {
    val json = FileInputStream("songInfo/instruments.json").readAllBytes().decodeToString()
    val schema = ArraySchema(
        ObjectSchema(
            "name" to StringSchema() to false,
            "synth" to StringSchema() to false,
            "params" to AnySchema() to false,
            "effects" to effectsSchema() to true
        )
    )

    val parsed = schema.safeParse(json).throwIfErr()
    val instruments = mutableMapOf<String, Pair<Synth, List<Effect>>>()
    for (instrument in parsed.arr()) {
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
        val adsrSchema = ObjectSchema(
            "attack" to NumberSchema() to false,
            "decay" to NumberSchema() to false,
            "sustain" to NumberSchema() to false,
            "release" to NumberSchema() to false
        )

        val waveDataSchema = ObjectSchema(
            "amplitude" to NumberSchema() to false, "phase" to NumberSchema() to true
        )

        val schema = ObjectSchema(
            "vibrato" to NumberSchema() to false,
            "detune" to NumberSchema() to true,
            "adsr" to adsrSchema to false,
            "frequencyOverride" to NumberSchema() to true,
            "wave" to ObjectSchema(
                *WaveType.entries.map { it.id to waveDataSchema to true }.toTypedArray()
            ) to false
        )

        val parsed = schema.safeConvert(params).throwIfErr()
        val vibrato = parsed["vibrato"]!!.num().value.toDouble()
        val detune = parsed["detune"]?.num()?.value?.toDouble() ?: 0.0
        val freqOverride = parsed["frequencyOverride"]?.num()?.value?.toDouble()
        val adsrObj = parsed["adsr"]!!.obj()
        val adsr = ADSR(
            adsrObj["attack"]!!.num().value.toDouble(),
            adsrObj["decay"]!!.num().value.toDouble(),
            adsrObj["sustain"]!!.num().value.toDouble(),
            adsrObj["release"]!!.num().value.toDouble()
        )
        val wave = parsed["wave"]!!.obj()
        val waves = mutableMapOf<WaveType, WaveData>()
        for (waveType in WaveType.entries) {
            val waveData = wave[waveType.id]?.obj()
            if (waveData != null) {
                val amplitude = waveData["amplitude"]!!.num().value.toDouble()
                val phase = waveData["phase"]?.num()?.value?.toDouble() ?: 0.0
                waves[waveType] = WaveData(amplitude, phase)
            }
        }
        val synth = SimpleSynth(vibrato, adsr, detune, freqOverride, waves)
        return synth
    } else {
        throw IllegalArgumentException("Unknown synth $synthName")
    }
}