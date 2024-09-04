package instruments

import nodes.*
import kotlin.random.Random


class Flute(random: Random) : Instrument({
    val phase = random.nextDouble(0.0, 1.0)
    val sound = (OscillatorNode.triangle(initialPhase = phase) * 0.2) bus (OscillatorNode.sine(
        initialPhase = phase
    ) * 0.8)
    val vibratoFreqMod = vibrato(0.015)
    val node = vibratoFreqMod stack CustomNode.pass(1) pipe adsrVolume(sound, 0.05, 0.0, 1.0, 0.5)
    node pipe ChorusNode(0.03, 3, 0.2) pipe CustomNode.duplicate(1)
})
