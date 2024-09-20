package instruments

import nodes.*
import kotlin.math.*

fun vibrato(amount: Double, frequency: Double): AudioNode {
    val lfo = Envelope({ t ->
        val am = min(t, 1.0) * amount * 0.5
        xerp(1.0 / (1.0 + am), 1.0 + am, sin(frequency * t * 2 * PI))
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