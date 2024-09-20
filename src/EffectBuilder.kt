import effects.*
import nodes.AudioNode
import nodes.pipe
import util.json.*


fun buildEffects(effects: List<SchemaData>): List<Effect> {
    val effectList = mutableListOf<Effect>()
    for (effect in effects) {
        val effectObj = effect.obj()
        val type = effectObj["type"]!!.str().value
        val params = effectObj["params"]!!.any().json
        val effectInstance = buildEffect(type, params)
        effectList.add(effectInstance)
    }
    return effectList
}

fun buildEffect(type: String, params: JsonElement): Effect {
    when (type) {
        "Highpass" -> {
            val schema = ObjectSchema(
                "cutoff" to NumberSchema() to false, "mix" to NumberSchema() to true, "q" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val cutoff = parsed["cutoff"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            val q = parsed["q"]?.num()?.value?.toDouble() ?: 1.0
            return HighpassFilter(cutoff, q, mix)
        }

        "Lowpass" -> {
            val schema = ObjectSchema(
                "cutoff" to NumberSchema() to false, "mix" to NumberSchema() to true, "q" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val cutoff = parsed["cutoff"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            val q = parsed["q"]?.num()?.value?.toDouble() ?: 1.0
            return LowpassFilter(cutoff, q, mix)
        }

        "Distortion" -> {
            val schema = ObjectSchema(
                "hardness" to NumberSchema() to false, "mix" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val hardness = parsed["hardness"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            return DistortionEffect(hardness, mix)
        }

        "Gain" -> {
            val schema = ObjectSchema("gain" to NumberSchema() to false)
            val parsed = schema.safeConvert(params).throwIfErr()
            val gain = parsed["gain"]!!.num().value.toDouble()
            return GainEffect(gain)
        }

        else -> return NoEffect()
    }
}

fun effectsSchema(): JsonSchema {
    val schema = ArraySchema(
        ObjectSchema(
            "type" to StringSchema() to false, "params" to AnySchema() to false
        )
    )
    return schema
}

fun applyEffects(node: AudioNode, effects: List<Effect>): AudioNode {
    var result = node
    for (effect in effects) {
        result = result pipe effect.buildNode()
    }
    return result
}