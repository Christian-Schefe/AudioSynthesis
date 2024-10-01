package nodes

import kotlin.random.Random

class NoiseNode(val amplitude: Double = 1.0) : AudioNode(0, 1) {
    var random = Random(0)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return doubleArrayOf((random.nextDouble() * 2 - 1) * amplitude)
    }

    override fun clone(): AudioNode {
        return NoiseNode()
    }

    override fun init(ctx: Context) {
        random = ctx.random
    }
}

class SplineNoiseNode(val seed: Int, val amplitude: Double = 1.0, val samplesPerSecond: Double) : AudioNode(0, 1) {
    var time = 0.0
    var rand = Random(seed)
    var prev3 = rand.nextDouble() * 2 - 1
    var prev2 = rand.nextDouble() * 2 - 1
    var prev1 = rand.nextDouble() * 2 - 1
    var prev0 = rand.nextDouble() * 2 - 1

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        time += ctx.timeStep * samplesPerSecond
        val floorTime = time.toInt()
        val t = time - floorTime
        val value = cubicInterpolation(prev3, prev2, prev1, prev0, t) * amplitude
        if (time >= 1.0) {
            time -= 1.0
            prev3 = prev2
            prev2 = prev1
            prev1 = prev0
            prev0 = rand.nextDouble() * 2 - 1
        }
        return doubleArrayOf(value)
    }

    override fun clone(): AudioNode {
        return SplineNoiseNode(seed, amplitude, samplesPerSecond)
    }

    override fun reset() {
        time = 0.0
        rand = Random(seed)
        prev3 = rand.nextDouble() * 2 - 1
        prev2 = rand.nextDouble() * 2 - 1
        prev1 = rand.nextDouble() * 2 - 1
        prev0 = rand.nextDouble() * 2 - 1
    }

    private fun cubicInterpolation(x0: Double, x1: Double, x2: Double, x3: Double, t: Double): Double {
        return x1 + 0.5 * t * (x2 - x0 + t * (2.0 * x0 - 5.0 * x1 + 4.0 * x2 - x3 + t * (3.0 * (x1 - x2) + x3 - x0)))
    }
}