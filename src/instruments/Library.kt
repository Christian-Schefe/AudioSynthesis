package instruments

import instruments.Instrument.Companion.adsrVolume
import nodes.*
import kotlin.random.Random


fun flute(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)
    val sound = (OscillatorNode.triangle(initialPhase = phase) * 0.4) bus (OscillatorNode.sine(
        initialPhase = phase
    ) * 0.6)
    val vibratoFreqMod = vibrato2(0.015, 5.0)
    val node = vibratoFreqMod stack CustomNode.pass(1) pipe adsrVolume(sound, 0.05, 0.5, 0.9, 0.5)
    return ((node pipe ChorusNode(0.03, 3, 0.2)) * CustomNode.pass(1)) pipe CustomNode.duplicate(1)
}

fun piano(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)
    val sound = OscillatorNode.softSaw(initialPhase = phase)
    val adsrSound = sound * ADSRNode(0.02, 1.0, 0.1, 0.5)
    val velocitySound = adsrSound * CustomNode.pass(1) * 2.0
    return velocitySound pipe BiquadFilter.lowpass(2000.0, 1.0, 0.0) pipe CustomNode.duplicate(1)
}

fun violin(random: Random): AudioNode {
    val phase = random.nextDouble(0.0, 1.0)
    val sound = (OscillatorNode.softSquare(initialPhase = phase) * 0.3) bus (OscillatorNode.saw(
        initialPhase = phase
    ) * 0.7)
    val vibratoFreqMod = vibrato2(0.01, 5.0)
    val node = vibratoFreqMod stack CustomNode.pass(1) pipe adsrVolume(sound, 0.1, 1.5, 0.9, 0.2)
    return (node * CustomNode.pass(1)) pipe Distortion.tanh(3.0) pipe BiquadFilter.lowpass(
        2000.0, 1.0, 0.0
    ) pipe CustomNode.duplicate(1)
}