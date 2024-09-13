package instruments

import nodes.*
import kotlin.math.*
import kotlin.random.Random

fun vibrato(amount: Double): AudioNode {
    val lfo = OscillatorNode.sine(5.0)
    val amountEnvelope = Envelope { min(it, 1.0) }
    val mappedLFO = lfo * amount * amountEnvelope + 1.0
    return CustomNode.pass(1) * mappedLFO
}

fun vibrato2(amount: Double, random: Random): AudioNode {
    val freq = 5.0 + random.nextDouble(-1.0, 1.0)
    val lfo = Envelope({ t ->
        val am = min(t, 1.0) * amount * 0.5
        xerp(1.0 / (1.0 + am), 1.0 + am, sin(freq * t * 2 * PI))
    })
    return CustomNode.pass(1) * lfo
}

fun lerp(a: Double, b: Double, t: Double): Double {
    return a * (1 - t) + b * t
}

fun xerp(a: Double, b: Double, t: Double): Double {
    require(a > 0 && b > 0)
    return exp(lerp(ln(a), ln(b), t))
}