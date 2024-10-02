package app

import effects.*
import node.AudioNode
import node.composite.Pipeline
import node.filter.SvfFilter
import node.filter.SvfFilterType
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

        "chorus" -> {
            val schema = ObjectSchema(
                "voiceCount" to NumberSchema() to false,
                "separation" to NumberSchema() to false,
                "variance" to NumberSchema() to false,
                "modulationSpeed" to NumberSchema() to false,
                "mix" to NumberSchema() to true
            )
            val parsed = schema.safeConvert(params).throwIfErr()
            val voiceCount = parsed["voiceCount"]!!.num().value.toInt()
            val separation = parsed["separation"]!!.num().value.toDouble()
            val variance = parsed["variance"]!!.num().value.toDouble()
            val modulationSpeed = parsed["modulationSpeed"]!!.num().value.toDouble()
            val mix = parsed["mix"]?.num()?.value?.toDouble() ?: 1.0
            return ChorusEffect(voiceCount, separation, variance, modulationSpeed, mix)
        }

        "svf" -> {
            val noGainFilterIds = SvfFilterType.entries.filter { !it.hasGain }.map { it.id }.toTypedArray()
            val gainFilterIds = SvfFilterType.entries.filter { it.hasGain }.map { it.id }.toTypedArray()

            val noGainSchema = ObjectSchema(
                "cutoff" to NumberSchema() to false,
                "q" to NumberSchema() to false,
                "type" to EnumSchema(noGainFilterIds) to false,
                "mix" to NumberSchema() to true
            )

            val gainSchema = ObjectSchema(
                "cutoff" to NumberSchema() to false,
                "q" to NumberSchema() to false,
                "type" to EnumSchema(gainFilterIds) to false,
                "gain" to NumberSchema() to false,
                "mix" to NumberSchema() to true
            )

            val schema = UnionSchema(noGainSchema, gainSchema)

            val parsed = schema.safeConvert(params).throwIfErr().union()
            val data = parsed.data

            val cutoff = data["cutoff"]!!.num().value.toDouble()
            val q = data["q"]!!.num().value.toDouble()
            val typeStr = (data["type"]!!.enum().value as JsonString).value
            val mix = data["mix"]?.num()?.value?.toDouble() ?: 1.0

            val gain = if (parsed.id == 1) {
                parsed["gain"]!!.num().value.toDouble()
            } else null

            val svfType = SvfFilterType.entries.find { it.id == typeStr }
                ?: throw IllegalArgumentException("Unknown filter type: $typeStr")
            if (svfType.hasGain && gain == null) {
                throw IllegalArgumentException("Filter type $typeStr requires gain")
            }

            val filter = SvfFilter.fromType(svfType, cutoff, q, gain)
            return Effect {
                StereoMixNode(
                    filter to filter.cloneSettings(), mix
                )
            }
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