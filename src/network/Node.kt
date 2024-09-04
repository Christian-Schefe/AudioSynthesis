package network

abstract class Node<I : UInt, out O : UInt>(val inputSize: I, val outputSize: O) {
    open fun init(sampleRate: Double) {}
    abstract fun process(input: Vec<UInt>): Vec<O>
}

fun main() {
    val pass = pass1()
    val constant = Constant(vec1(1.0))
    val map = map1 { it + 3.3 }
    val val1 = (constant pipe pass pipe map)
    val val2 = const2(4.0, 2.0) pipe mul2()
    val val3 = val1 stack val2

    val sin = const1(20.0) pipe sine()
    val node = val3 stack sin

    node.init(2000.0)
    for (i in 0 until 10) {
        val result = node.process(vec0())
        println(result)
    }
}
