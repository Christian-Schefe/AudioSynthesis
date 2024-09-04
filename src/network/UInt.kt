package network

interface UInt {
    val value: Int
}

class U0 : UInt {
    override val value = 0
}

class U1 : UInt {
    override val value = 1
}

class U2 : UInt {
    override val value = 2
}

class U3 : UInt {
    override val value = 3
}

class U4 : UInt {
    override val value = 4
}

class U5 : UInt {
    override val value = 5
}

data class Vec<out T : UInt>(val dim: T, val values: DoubleArray) {
    init {
        require(values.size == dim.value) { "Vector size ${values.size} does not match dimension ${dim.value}" }
    }

    val size = dim.value

    operator fun get(i: Int): Double {
        require(i in 0..<size) { "Index $i out of bounds for vector of size ${size}" }
        return values[i]
    }

    operator fun set(i: Int, value: Double) {
        require(i in 0..<size) { "Index $i out of bounds for vector of size ${size}" }
        values[i] = value
    }

    fun <TOut : UInt> slice(dim: TOut, start: Int): Vec<TOut> {
        val end = start + dim.value
        require(start >= 0 && end <= size) { "Cannot slice vector of size $size from $start to $end" }
        return Vec(dim, values.copyOfRange(start, end))
    }

    fun <TOut : UInt> combine(dim: TOut, other: Vec<UInt>): Vec<TOut> {
        require(size + other.size == dim.value) { "Cannot combine vectors of size $size and ${other.size} into vector of size ${dim.value}" }
        return Vec(dim, values + other.values)
    }

    constructor(dim: T, vec: Vec<UInt>) : this(dim, vec.values.copyOf())
    constructor(dim: T) : this(dim, DoubleArray(dim.value))
    constructor(dim: T, init: (Int) -> Double) : this(dim, DoubleArray(dim.value) { init(it) })
    constructor(dim: T, values: Collection<Double>) : this(dim, values.toDoubleArray())
}

fun vec0() = Vec(U0())
fun vec1(x0: Double) = Vec(U1(), doubleArrayOf(x0))
fun vec2(x0: Double, x1: Double) = Vec(U2(), doubleArrayOf(x0, x1))
fun vec3(x0: Double, x1: Double, x2: Double) = Vec(U3(), doubleArrayOf(x0, x1, x2))
fun vec4(x0: Double, x1: Double, x2: Double, x3: Double) = Vec(U4(), doubleArrayOf(x0, x1, x2, x3))
fun vec5(x0: Double, x1: Double, x2: Double, x3: Double, x4: Double) = Vec(U5(), doubleArrayOf(x0, x1, x2, x3, x4))

class Add<T1 : UInt, T2 : UInt>(a: T1, b: T2) : UInt {
    override val value = a.value + b.value
}