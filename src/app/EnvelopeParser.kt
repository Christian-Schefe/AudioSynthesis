package app

import instruments.ControlledShapeEnvelope
import instruments.EnvelopeNode
import instruments.Shape
import instruments.ShapeSection
import util.json.*

fun parseEnvelopes(envelopes: JsonElement): Map<String, () -> EnvelopeNode> {
    val shapeSectionSchema = ObjectSchema(
        "duration" to NumberSchema() to false,
        "endValue" to NumberSchema() to false,
        "steepness" to NumberSchema() to true
    )
    val shapeSchema = ArraySchema(shapeSectionSchema)
    val envelopeSchema = ObjectSchema(
        "attack" to shapeSchema to false, "release" to shapeSchema to true
    )
    val envelopesSchema = MapSchema(envelopeSchema)

    val converted = envelopesSchema.safeConvert(envelopes).throwIfErr().map()

    val result = mutableMapOf<String, () -> EnvelopeNode>()

    for ((name, envelope) in converted.properties) {
        result[name] = convertEnvelope(envelope)
    }

    return result
}

fun convertEnvelope(envelope: SchemaData): () -> ControlledShapeEnvelope {
    val attack = convertShape(envelope["attack"]!!)
    val release = envelope["release"]?.let { convertShape(it) }
    return {
        ControlledShapeEnvelope(attack, release)
    }
}

fun convertShape(shape: SchemaData): Shape {
    val arr = shape.arr()
    val sections = arr.elements.map { convertShapeSection(it.obj()) }
    return Shape(sections)
}

fun convertShapeSection(section: SchemaData): ShapeSection {
    val duration = section["duration"]!!.num().value.toDouble()
    val endValue = section["endValue"]!!.num().value.toDouble()
    val steepness = section["steepness"]?.num()?.value?.toDouble() ?: 1.0
    return ShapeSection(endValue, duration, steepness)
}