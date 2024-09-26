package app

import effects.*
import nodes.AudioNode
import nodes.Pipeline
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
    when (type.lowercase()) {
        "highpass" -> {
            val schema = ObjectSchema(
                "cutoff" to NumberSchema() to false, "mix" to NumberSchema() to true, "q" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val cutoff = parsed["cutoff"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            val q = parsed["q"]?.num()?.value?.toDouble() ?: 1.0
            return HighpassFilter(cutoff, q, mix)
        }

        "lowpass" -> {
            val schema = ObjectSchema(
                "cutoff" to NumberSchema() to false, "mix" to NumberSchema() to true, "q" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val cutoff = parsed["cutoff"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            val q = parsed["q"]?.num()?.value?.toDouble() ?: 1.0
            return LowpassFilter(cutoff, q, mix)
        }

        "distortion" -> {
            val schema = ObjectSchema(
                "hardness" to NumberSchema() to false, "mix" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val hardness = parsed["hardness"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            return DistortionEffect(hardness, mix)
        }

        "gain" -> {
            val schema = ObjectSchema("gain" to NumberSchema() to false)
            val parsed = schema.safeConvert(params).throwIfErr()
            val gain = parsed["gain"]!!.num().value.toDouble()
            return GainEffect(gain)
        }

        else -> throw IllegalArgumentException("Unknown effect type: $type")
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
    val effectNodes = effects.map { it.buildNode() }
    return Pipeline(listOf(node) + effectNodes)
}