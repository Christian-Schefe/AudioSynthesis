package instruments

import nodes.AudioNode
import nodes.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


abstract class EnvelopeNode : AudioNode(1, 1) {
    private var time = 0.0

    abstract fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val result = applyEnvelope(ctx, time, inputs[0])
        time += ctx.timeStep
        return doubleArrayOf(result)
    }

    override fun reset() {
        time = 0.0
    }

    abstract fun timeToSilence(releaseMoment: Double): Double
}

class FadeInEnvelope(private val fadeTime: Double) : EnvelopeNode() {
    override fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double {
        return (time / fadeTime).coerceIn(0.0, 1.0)
    }

    override fun clone(): AudioNode {
        return FadeInEnvelope(fadeTime)
    }

    override fun timeToSilence(releaseMoment: Double): Double {
        return fadeTime
    }
}

class PolynomialEnvelope(private val fadeTime: Double, private val power: Double) : EnvelopeNode() {
    override fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double {
        return (1.0 - (time / fadeTime)).coerceIn(0.0, 1.0).pow(power)
    }

    override fun clone(): AudioNode {
        return PolynomialEnvelope(fadeTime, power)
    }

    override fun timeToSilence(releaseMoment: Double): Double {
        return max(0.0, releaseMoment - fadeTime)
    }
}

class ADSREnvelope(
    private val attack: Double, private val decay: Double, private val sustain: Double, private val release: Double
) : EnvelopeNode() {
    private var state = State.OFF

    private var lerpTime = 0.0
    private var value = 0.0

    private var fromValue = 0.0
    private var toValue = 0.0
    private var lerpDuration = 0.0

    private var prevGate = 0.0

    private fun updateState(gate: Double) {
        if (gate > 0.5 && prevGate <= 0.5) {
            initAttack()
        } else if (gate <= 0.5 && prevGate > 0.5) {
            initRelease()
        }
        prevGate = gate
    }

    private fun handleTransition() {
        if (state == State.ATTACK && lerpTime >= 1.0) {
            state = State.DECAY
            lerpTime = 0.0
            fromValue = 1.0
            toValue = sustain
            lerpDuration = decay
        } else if (state == State.DECAY && lerpTime >= 1.0) {
            state = State.SUSTAIN
            lerpTime = 0.0
            fromValue = sustain
            toValue = sustain
            lerpDuration = 0.0
        } else if (state == State.RELEASE && lerpTime >= 1.0) {
            state = State.OFF
            lerpTime = 0.0
            fromValue = 0.0
            toValue = 0.0
            lerpDuration = 0.0
        }
    }

    private fun initAttack() {
        state = State.ATTACK
        lerpTime = 0.0
        fromValue = value
        toValue = 1.0
        lerpDuration = attack
    }

    private fun initRelease() {
        state = State.RELEASE
        lerpTime = 0.0
        fromValue = value
        toValue = 0.0
        lerpDuration = release
    }


    override fun applyEnvelope(ctx: Context, time: Double, gate: Double): Double {
        updateState(gate)
        handleTransition()

        value = lerp(fromValue, toValue, lerpTime)
        lerpTime = if (lerpDuration > 0) min(1.0, lerpTime + ctx.timeStep / lerpDuration) else 1.0

        return value
    }

    private fun lerp(from: Double, to: Double, t: Double): Double {
        return from + (to - from) * t
    }

    override fun reset() {
        super.reset()
        state = State.OFF
        lerpTime = 0.0
        value = 0.0
    }

    override fun timeToSilence(releaseMoment: Double): Double {
        return release
    }

    private enum class State {
        OFF, ATTACK, DECAY, SUSTAIN, RELEASE
    }

    override fun clone(): AudioNode {
        val cloned = ADSREnvelope(attack, decay, sustain, release)
        cloned.state = state
        cloned.lerpTime = lerpTime
        cloned.value = value
        cloned.fromValue = fromValue
        cloned.toValue = toValue
        cloned.lerpDuration = lerpDuration
        cloned.prevGate = prevGate
        return cloned
    }
}