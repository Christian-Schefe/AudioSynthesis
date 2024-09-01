package nodes

fun expFalloff(decay: Double): Envelope {
    return Envelope({ Math.exp(-it / decay) })
}

fun hold(time: Double): Envelope {
    return Envelope({ if (it <= time) 1.0 else 0.0 })
}

class Envelope(val func: (Double) -> Double) : AudioNode(0, 1) {
    private var time = 0.0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        time += ctx.timeStep
        return doubleArrayOf(func(time))
    }

    override fun reset() {
        time = 0.0
    }

    override fun clone(): AudioNode {
        val cloned = Envelope(func)
        cloned.time = time
        return cloned
    }
}