package instruments

import nodes.*
import kotlin.math.min

fun vibrato(amount: Double): AudioNode {
    val lfo = OscillatorNode.sine(5.0)
    val amountEnvelope = Envelope { min(it, 1.0) }
    val mappedLFO = lfo * amount * amountEnvelope + 1.0
    return CustomNode.pass(1) * mappedLFO
}