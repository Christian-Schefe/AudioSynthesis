package nodes

import kotlin.math.PI
import kotlin.math.sin

fun sineNode(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
    OscillatorNode({ sin(2 * PI * it) }, initialPhase).run {
        return if (freq != null) (ConstantNode(freq) pipe this) else this
    }
}

fun squareNode(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
    OscillatorNode({ if (it < 0.5) 1.0 else -1.0 }, initialPhase).run {
        return if (freq != null) (ConstantNode(freq) pipe this) else this
    }
}

fun triangleNode(freq: Double? = null, initialPhase: Double = 0.0): AudioNode {
    OscillatorNode({ if (it < 0.25) 4 * it else if (it < 0.75) 2 - 4 * it else -4 + 4 * it }, initialPhase).run {
        return if (freq != null) (ConstantNode(freq) pipe this) else this
    }
}

fun adsrVolume(
    node: AudioNode, attack: Double, decay: Double, sustain: Double, release: Double
): AudioNode {
    val adsr = ADSRNode(attack, decay, sustain, release)
    return node * adsr
}

fun adsrNote(
    adsrNote: AudioNode, freq: Double, hold: Double
): AudioNode {
    return freq stack hold(hold) pipe adsrNote
}

fun midiNoteToFreq(note: Int): Double {
    return 440.0 * Math.pow(2.0, (note - 69.0) / 12.0)
}

infix fun Double.pipe(node: AudioNode): AudioNode {
    return ConstantNode(this) pipe node
}

infix fun Double.stack(node: AudioNode): AudioNode {
    return ConstantNode(this) stack node
}

abstract class AudioNode(val inputCount: Int, val outputCount: Int) {
    abstract fun process(ctx: Context, inputs: DoubleArray): DoubleArray
    abstract fun clone(): AudioNode
    open fun reset() {}

    infix fun stack(other: AudioNode): AudioNode {
        return StackNode(arrayOf(this, other))
    }

    operator fun plus(other: AudioNode): AudioNode {
        return MixerNode(arrayOf(this, other), outputCount, MixerNode.Mode.SUM)
    }

    infix fun pipe(other: AudioNode): AudioNode {
        return PipeNode(this, other)
    }
    
    infix fun bus(other: AudioNode): AudioNode {
        return BusNode(inputCount, outputCount, this, other)
    }

    operator fun times(factor: Double): AudioNode {
        return this * ConstantNode(factor)
    }

    operator fun times(other: AudioNode): AudioNode {
        return MixerNode(arrayOf(this, other), outputCount, MixerNode.Mode.PRODUCT)
    }
}

class BusNode(inputCount: Int, outputCount: Int, private vararg val nodes: AudioNode) :
    AudioNode(inputCount, outputCount) {
    init {
        require(nodes.all { it.inputCount == inputCount })
        require(nodes.all { it.outputCount == outputCount })
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val outputs = DoubleArray(outputCount)
        for (node in nodes) {
            val nodeOutputs = node.process(ctx, inputs)
            for (i in 0..<outputCount) {
                outputs[i] += nodeOutputs[i]
            }
        }
        return outputs
    }

    override fun reset() {
        nodes.forEach { it.reset() }
    }

    override fun clone(): AudioNode {
        val cloned = BusNode(inputCount, outputCount, *nodes.map { it.clone() }.toTypedArray())
        return cloned
    }
}

class DuplicatorNode(inputCount: Int, private val count: Int) : AudioNode(inputCount, inputCount * count) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val outputs = DoubleArray(outputCount)
        for (i in 0..<count) {
            inputs.copyInto(outputs, i * inputCount)
        }
        return outputs
    }

    override fun clone(): AudioNode = DuplicatorNode(inputCount, count)
}

class PipeNode(private val left: AudioNode, private val right: AudioNode) :
    AudioNode(left.inputCount, right.outputCount) {
    init {
        require(left.outputCount == right.inputCount)
    }

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val leftOutputs = left.process(ctx, inputs)
        return right.process(ctx, leftOutputs)
    }

    override fun reset() {
        left.reset()
        right.reset()
    }

    override fun clone(): AudioNode = PipeNode(left.clone(), right.clone())
}

class MixerNode(private val nodes: Array<AudioNode>, outputCount: Int, private val mode: Mode) :
    AudioNode(nodes.sumOf { it.inputCount }, outputCount) {
    enum class Mode {
        SUM, PRODUCT
    }

    init {
        require(nodes.all { it.outputCount == outputCount })
    }

    fun withAddedNode(node: AudioNode) = MixerNode(nodes + node, outputCount, mode)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        var inputPointer = 0
        val outputs = DoubleArray(outputCount, {
            when (mode) {
                Mode.SUM -> 0.0; Mode.PRODUCT -> 1.0
            }
        })

        for (node in nodes) {
            val nodeInputs = inputs.copyOfRange(inputPointer, inputPointer + node.inputCount)
            val nodeOutputs = node.process(ctx, nodeInputs)
            for (i in 0..<outputCount) {
                when (mode) {
                    Mode.SUM -> outputs[i] += nodeOutputs[i]
                    Mode.PRODUCT -> outputs[i] *= nodeOutputs[i]
                }
            }
            inputPointer += node.inputCount
        }
        return outputs
    }

    override fun reset() {
        nodes.forEach { it.reset() }
    }

    override fun clone(): AudioNode = MixerNode(nodes.map { it.clone() }.toTypedArray(), outputCount, mode)
}

class StackNode(private val nodes: Array<AudioNode>) :
    AudioNode(nodes.sumOf { it.inputCount }, nodes.sumOf { it.outputCount }) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        var inputPointer = 0
        var outputPointer = 0
        val outputs = DoubleArray(outputCount)
        for (node in nodes) {
            val nodeInputs = inputs.copyOfRange(inputPointer, inputPointer + node.inputCount)
            val nodeOutputs = node.process(ctx, nodeInputs)
            nodeOutputs.copyInto(outputs, outputPointer)
            inputPointer += node.inputCount
            outputPointer += node.outputCount
        }
        return outputs
    }

    override fun reset() {
        nodes.forEach { it.reset() }
    }

    override fun clone(): AudioNode = StackNode(nodes.map { it.clone() }.toTypedArray())
}

class PassThroughNode(channels: Int) : AudioNode(channels, channels) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return inputs
    }

    override fun clone(): AudioNode = PassThroughNode(inputCount)
}

class OscillatorNode(private val oscillator: (Double) -> Double, private val initialPhase: Double = 0.0) :
    AudioNode(1, 1) {
    private var phase = initialPhase

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val freq = inputs[0]
        phase += ctx.timeStep * freq
        while (phase >= 1.0) phase -= 1.0
        return doubleArrayOf(oscillator(phase))
    }

    override fun reset() {
        phase = initialPhase
    }

    override fun clone(): AudioNode {
        val cloned = OscillatorNode(oscillator, initialPhase)
        cloned.phase = phase
        return cloned
    }
}

class ConstantNode(private vararg val values: Double) : AudioNode(0, values.size) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return values
    }

    override fun clone(): AudioNode = ConstantNode(*values)
}

class SinkNode(channels: Int) : AudioNode(channels, 0) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return DoubleArray(0)
    }

    override fun clone(): AudioNode = SinkNode(inputCount)
}

class CustomNode(inputCount: Int, outputCount: Int, private val mapper: (DoubleArray) -> DoubleArray) :
    AudioNode(inputCount, outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return mapper(inputs)
    }

    constructor(vararg mappers: (Double) -> Double) : this(mappers.size,
        mappers.size,
        { it.mapIndexed { i, v -> mappers[i](v) }.toDoubleArray() })

    constructor(channelCount: Int, mapper: (Double) -> Double) : this(channelCount,
        channelCount,
        { it.map(mapper).toDoubleArray() })

    override fun clone(): AudioNode = CustomNode(inputCount, outputCount, mapper)
}

class DelayedNode(private val delay: Double, private val node: AudioNode) :
    AudioNode(node.inputCount, node.outputCount) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        if (ctx.time < delay) return DoubleArray(node.outputCount)
        return node.process(ctx, inputs)
    }

    override fun reset() {
        node.reset()
    }

    override fun clone(): AudioNode = DelayedNode(delay, node.clone())
}

class DelayNode(private val delay: Double, private val sampleRate: Int, channels: Int) : AudioNode(channels, channels) {
    private val buffer = DoubleArray((delay * sampleRate).toInt())
    private var bufferPointer = 0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = buffer[bufferPointer]
        buffer[bufferPointer] = inputs[0]
        bufferPointer = (bufferPointer + 1) % buffer.size
        return doubleArrayOf(output)
    }

    override fun reset() {
        buffer.fill(0.0)
        bufferPointer = 0
    }

    override fun clone(): AudioNode {
        val cloned = DelayNode(delay, sampleRate, inputCount)
        cloned.bufferPointer = bufferPointer
        buffer.copyInto(cloned.buffer)
        return cloned
    }
}