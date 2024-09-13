package instruments

import instruments.Instrument.Companion.adsrVolume
import nodes.*
import kotlin.random.Random


fun flute(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)
    val sound = (OscillatorNode.triangle(initialPhase = phase) * 0.4) bus (OscillatorNode.sine(
        initialPhase = phase
    ) * 0.6)
    val vibratoFreqMod = vibrato2(0.015, random)
    val node = vibratoFreqMod stack CustomNode.pass(1) pipe adsrVolume(sound, 0.05, 0.5, 0.9, 0.5)
    return ((node pipe ChorusNode(0.03, 3, 0.2)) * CustomNode.pass(1)) pipe CustomNode.duplicate(1)
}

fun piano(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)/*val sound = (OscillatorNode.square(initialPhase = phase) * 0.8) bus (OscillatorNode.sine(
        initialPhase = phase
    ) * 0.2)*/
    val sound = OscillatorNode.softSaw(initialPhase = phase)
    val node = adsrVolume(sound, 0.02, 1.0, 0.1, 0.5)
    return (node * CustomNode.pass(1)) pipe CustomNode.duplicate(1)
}

fun violin(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)
    val sound = (OscillatorNode.softSquare(initialPhase = phase) * 0.7) bus (OscillatorNode.softSaw(
        initialPhase = phase
    ) * 0.3)
    val vibratoFreqMod = vibrato2(0.01, random)
    val node = vibratoFreqMod stack CustomNode.pass(1) pipe adsrVolume(sound, 0.1, 1.5, 0.9, 0.5)
    return (node * CustomNode.pass(1)) pipe CustomNode.duplicate(1)
}