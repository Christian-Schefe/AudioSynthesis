package network

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

class Pass<T : UInt>(inputSize: T) : Node<T, T>(inputSize, inputSize) {
    override fun process(input: Vec<UInt>): Vec<T> {
        return Vec(inputSize, input)
    }
}

fun pass1() = Pass(U1())
fun pass2() = Pass(U2())
fun pass3() = Pass(U3())
fun pass4() = Pass(U4())
fun pass5() = Pass(U5())

class Constant<TOut : UInt>(private val value: Vec<TOut>) : Node<U0, TOut>(U0(), value.dim) {
    override fun process(input: Vec<UInt>): Vec<TOut> {
        return value
    }
}

fun const1(x0: Double) = Constant(vec1(x0))
fun const2(x0: Double, x1: Double) = Constant(vec2(x0, x1))
fun const3(x0: Double, x1: Double, x2: Double) = Constant(vec3(x0, x1, x2))
fun const4(x0: Double, x1: Double, x2: Double, x3: Double) = Constant(vec4(x0, x1, x2, x3))
fun const5(x0: Double, x1: Double, x2: Double, x3: Double, x4: Double) = Constant(vec5(x0, x1, x2, x3, x4))

class Sink<TIn : UInt>(inputSize: TIn) : Node<TIn, U0>(inputSize, U0()) {
    override fun process(input: Vec<UInt>): Vec<U0> {
        return Vec(U0())
    }
}

fun sink1() = Sink(U1())
fun sink2() = Sink(U2())
fun sink3() = Sink(U3())
fun sink4() = Sink(U4())
fun sink5() = Sink(U5())

class Map<TIn : UInt, TOut : UInt>(inputSize: TIn, outputSize: TOut, val transform: (Vec<UInt>) -> Vec<TOut>) :
    Node<TIn, TOut>(inputSize, outputSize) {
    override fun process(input: Vec<UInt>): Vec<TOut> {
        return transform(input)
    }
}

fun <T : UInt> map(inputSize: T, vararg transforms: (Double) -> Double) = Map(inputSize, inputSize) { input ->
    Vec(
        inputSize, input.values.mapIndexed { i, x -> transforms[i](x) }.toDoubleArray()
    )
}

fun map1(transform: (Double) -> Double) = map(U1(), transform)

fun <TIn : UInt, TMiddle : UInt, TOut : UInt> Node<TIn, TMiddle>.map(
    outputSize: TOut, transform: (Vec<UInt>) -> Vec<TOut>
): Node<TIn, TOut> {
    return this pipe Map(this.outputSize, outputSize, transform)
}


class Pipe<TIn : UInt, TMiddle : UInt, TOut : UInt>(
    private val left: Node<TIn, TMiddle>, private val right: Node<TMiddle, TOut>
) : Node<TIn, TOut>(left.inputSize, right.outputSize) {
    override fun process(input: Vec<UInt>): Vec<TOut> {
        val middle = left.process(input)
        return right.process(middle)
    }

    override fun init(sampleRate: Double) {
        left.init(sampleRate)
        right.init(sampleRate)
    }
}

infix fun <TIn : UInt, TMiddle : UInt, TOut : UInt> Node<TIn, TMiddle>.pipe(right: Node<TMiddle, TOut>): Pipe<TIn, TMiddle, TOut> {
    return Pipe(this, right)
}

class Time(val transform: (Double) -> Double) : Node<U0, U1>(U0(), U1()) {
    private var time = 0.0
    private var step: Double? = null

    override fun init(sampleRate: Double) {
        time = 0.0
        step = 1.0 / sampleRate
    }

    override fun process(input: Vec<UInt>): Vec<U1> {
        requireNotNull(step) { "Sample rate was not set." }
        val result = vec1(transform(time))
        time += step!!
        return result
    }
}

fun time() = Time { it }

class Oscillator(val shape: (Double) -> Double) : Node<U1, U1>(U1(), U1()) {
    private var phase = 0.0
    private var step: Double? = null

    override fun init(sampleRate: Double) {
        phase = 0.0
        step = 1.0 / sampleRate
    }

    override fun process(input: Vec<UInt>): Vec<U1> {
        requireNotNull(step) { "Sample rate was not set." }
        val result = vec1(shape(phase))
        phase += step!! * input.values[0]
        phase -= floor(phase)
        return result
    }
}

fun oscillator(shape: (Double) -> Double) = Oscillator(shape)
fun sine() = oscillator { sin(it * 2.0 * PI) }
fun square() = oscillator { if (it < 0.5) 1.0 else -1.0 }
fun pulse(width: Double) = oscillator { if (it < width) 1.0 else -1.0 }
fun triangle() = oscillator { 2.0 * (if (it < 0.5) it else 1.0 - it) - 1.0 }
fun sawtooth() = oscillator { 2.0 * it - 1.0 }

class FixedOscillator(private val frequency: Double, val shape: (Double) -> Double) : Node<U0, U1>(U0(), U1()) {
    private var phase = 0.0
    private var step = 0.0

    override fun init(sampleRate: Double) {
        phase = 0.0
        step = 1.0 / sampleRate
    }

    override fun process(input: Vec<UInt>): Vec<U1> {
        val result = vec1(shape(phase))
        phase += step * frequency
        phase -= floor(phase)
        return result
    }
}

fun oscillator(frequency: Double, shape: (Double) -> Double) = FixedOscillator(frequency, shape)
fun sine(frequency: Double) = oscillator(frequency) { sin(it * 2.0 * PI) }
fun square(frequency: Double) = oscillator(frequency) { if (it < 0.5) 1.0 else -1.0 }
fun pulse(frequency: Double, width: Double) = oscillator(frequency) { if (it < width) 1.0 else -1.0 }
fun triangle(frequency: Double) = oscillator(frequency) { 2.0 * (if (it < 0.5) it else 1.0 - it) - 1.0 }
fun sawtooth(frequency: Double) = oscillator(frequency) { 2.0 * it - 1.0 }

class Reduce<TIn : UInt>(inputSize: TIn, private val operation: (Double, Double) -> Double) :
    Node<TIn, U1>(inputSize, U1()) {
    override fun process(input: Vec<UInt>): Vec<U1> {
        return vec1(input.values.reduce(operation))
    }
}

fun <TIn : UInt> add(inputSize: TIn) = Reduce(inputSize, Double::plus)
fun <TIn : UInt> mul(inputSize: TIn) = Reduce(inputSize, Double::times)
fun add2() = add(U2())
fun add3() = add(U3())
fun add4() = add(U4())
fun add5() = add(U5())
fun mul2() = mul(U2())
fun mul3() = mul(U3())
fun mul4() = mul(U4())
fun mul5() = mul(U5())

class Stack<LIn : UInt, RIn : UInt, LOut : UInt, ROut : UInt>(
    private val leftInputSize: LIn,
    private val rightInputSize: RIn,
    private val leftOutputSize: LOut,
    private val rightOutputSize: ROut,
    private val left: Node<LIn, LOut>,
    private val right: Node<RIn, ROut>
) : Node<Add<LIn, RIn>, Add<LOut, ROut>>(Add(leftInputSize, rightInputSize), Add(leftOutputSize, rightOutputSize)) {
    override fun process(input: Vec<UInt>): Vec<Add<LOut, ROut>> {
        val leftInput = input.slice(leftInputSize, 0)
        val rightInput = input.slice(rightInputSize, leftInputSize.value)
        val leftOutput = left.process(leftInput)
        val rightOutput = right.process(rightInput)
        return Vec(Add(leftOutputSize, rightOutputSize), leftOutput.values + rightOutput.values)
    }
    
    override fun init(sampleRate: Double) {
        left.init(sampleRate)
        right.init(sampleRate)
    }
}

infix fun <LIn : UInt, RIn : UInt, LOut : UInt, ROut : UInt> Node<LIn, LOut>.stack(right: Node<RIn, ROut>): Stack<LIn, RIn, LOut, ROut> {
    return Stack(inputSize, right.inputSize, outputSize, right.outputSize, this, right)
}