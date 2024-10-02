package instruments

import node.AudioNode
import node.Context
import kotlin.math.pow

class ShapeEnvelope(val shape: Shape) : EnvelopeNode() {
    private var sectionIndex = 0
    private var sectionStartTime = 0.0
    var startVal = 0.0

    override fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double {
        if (shape.sections.isEmpty()) return startVal

        var sectionTime = time - sectionStartTime
        while (sectionIndex > 0 && sectionTime < 0) {
            sectionIndex--
            sectionStartTime -= shape.sections[sectionIndex].duration
            sectionTime = time - sectionStartTime
        }
        while (sectionIndex < shape.sections.size - 1 && sectionTime >= shape.sections[sectionIndex].duration) {
            sectionStartTime += shape.sections[sectionIndex].duration
            sectionTime = time - sectionStartTime
            sectionIndex++
        }

        val section = shape.sections[sectionIndex]
        val prevEndVal = if (sectionIndex == 0) startVal else shape.sections[sectionIndex - 1].endVal
        val value = section.valueAt(sectionTime, prevEndVal)
        return value
    }

    override fun cloneSettings(): AudioNode {
        return ShapeEnvelope(shape.copy())
    }

    override fun timeToSilence(releaseMoment: Double): Double {
        return shape.duration - releaseMoment
    }
}

class ControlledShapeEnvelope(attackShape: Shape, releaseShape: Shape?) : EnvelopeNode() {
    private var prevGate = 0.0
    private var attackEnvelope = ShapeEnvelope(attackShape)
    private var releaseEnvelope = releaseShape?.let { ShapeEnvelope(it) }
    private var envelope: ShapeEnvelope? = null

    private var timeOffset = 0.0
    private var prevValue = 0.0

    override fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double {
        if (gate >= 0.5 && prevGate < 0.5) {
            attackEnvelope.startVal = prevValue
            envelope = attackEnvelope
            timeOffset = time
        } else if (gate < 0.5 && prevGate >= 0.5 && releaseEnvelope != null) {
            releaseEnvelope!!.startVal = prevValue
            envelope = releaseEnvelope
            timeOffset = time
        }

        val value = envelope?.applyEnvelope(ctx, time - timeOffset, gate) ?: 0.0

        prevGate = gate
        prevValue = value
        return value
    }

    override fun cloneSettings(): AudioNode {
        val clone = ControlledShapeEnvelope(attackEnvelope.shape, releaseEnvelope?.shape)
        clone.prevGate = prevGate
        clone.attackEnvelope = attackEnvelope.cloneSettings() as ShapeEnvelope
        clone.releaseEnvelope = releaseEnvelope?.cloneSettings() as ShapeEnvelope?
        clone.envelope = envelope?.cloneSettings() as ShapeEnvelope?
        clone.timeOffset = timeOffset
        return clone
    }

    override fun timeToSilence(releaseMoment: Double): Double {
        return if (releaseEnvelope != null) {
            releaseEnvelope!!.timeToSilence(0.0)
        } else {
            attackEnvelope.timeToSilence(releaseMoment)
        }
    }
}

class Shape(val sections: List<ShapeSection>) {
    val duration = sections.sumOf { it.duration }

    fun copy(): Shape {
        return Shape(sections.map { it.copy() })
    }

    override fun toString(): String {
        return sections.joinToString(", ", "Shape(", ")")
    }
}

data class ShapeSection(
    val endVal: Double, val duration: Double, val steepness: Double
) {
    fun valueAt(time: Double, prevEndVal: Double): Double {
        if (duration == 0.0) return endVal
        val alpha = time / duration
        val t = alpha.coerceIn(0.0, 1.0).pow(steepness)
        return prevEndVal + (endVal - prevEndVal) * t
    }
}
