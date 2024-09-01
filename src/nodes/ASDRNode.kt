package nodes

import kotlin.math.min

class ADSRNode(val attack: Double, val decay: Double, val sustain: Double, val release: Double) : AudioNode(1, 1) {
    private var state = State.OFF

    private var time = 0.0
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
        if (state == State.ATTACK && time >= 1.0) {
            state = State.DECAY
            time = 0.0
            fromValue = 1.0
            toValue = sustain
            lerpDuration = decay
        } else if (state == State.DECAY && time >= 1.0) {
            state = State.SUSTAIN
            time = 0.0
            fromValue = sustain
            toValue = sustain
            lerpDuration = 0.0
        } else if (state == State.RELEASE && time >= 1.0) {
            state = State.OFF
            time = 0.0
            fromValue = 0.0
            toValue = 0.0
            lerpDuration = 0.0
        }
    }

    private fun initAttack() {
        state = State.ATTACK
        time = 0.0
        fromValue = value
        toValue = 1.0
        lerpDuration = attack
    }

    private fun initRelease() {
        state = State.RELEASE
        time = 0.0
        fromValue = value
        toValue = 0.0
        lerpDuration = release
    }


    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val gate = inputs[0]
        updateState(gate)
        handleTransition()

        value = lerp(fromValue, toValue, time)
        time = if (lerpDuration > 0) min(1.0, time + ctx.timeStep / lerpDuration) else 1.0

        return doubleArrayOf(value)
    }

    fun lerp(from: Double, to: Double, t: Double): Double {
        return from + (to - from) * t
    }

    override fun reset() {
        state = State.OFF
        time = 0.0
        value = 0.0
    }

    private enum class State {
        OFF,
        ATTACK,
        DECAY,
        SUSTAIN,
        RELEASE
    }

    override fun clone(): AudioNode {
        val cloned = ADSRNode(attack, decay, sustain, release)
        cloned.state = state
        cloned.time = time
        cloned.value = value
        cloned.fromValue = fromValue
        cloned.toValue = toValue
        cloned.lerpDuration = lerpDuration
        cloned.prevGate = prevGate
        return cloned
    }
}