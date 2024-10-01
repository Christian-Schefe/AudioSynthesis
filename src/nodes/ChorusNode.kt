package nodes

import kotlin.random.Random

class ChorusNode(
    private val seed: Int,
    private val voiceCount: Int,
    private val separation: Double,
    private val variance: Double,
    private val modulationSpeed: Double
) : AudioNode(1, 1) {
    private val random = Random(seed)
    private var voices: List<AudioNode> = List(voiceCount) { SplineNoiseNode(random.nextInt(), 1.0, modulationSpeed) }
    private var delay = ModulatedDelay(voiceCount, voiceCount * separation + variance)

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val inputArr = DoubleArray(voiceCount + 1) { i ->
            if (i == 0) inputs[0] else {
                val minDelay = i * separation
                val maxDelay = minDelay + variance
                val t = voices[i - 1].process(ctx, doubleArrayOf())[0]
                lerp11(minDelay, maxDelay, t)
            }
        }
        return delay.process(ctx, inputArr)
    }

    override fun clone(): AudioNode {
        val node = ChorusNode(seed, voiceCount, separation, variance, modulationSpeed)
        node.voices = voices.map { it.clone() }
        node.delay = delay.clone() as ModulatedDelay
        return node
    }

    override fun reset() {
        voices.forEach { it.reset() }
        delay.reset()
    }

    override fun init(ctx: Context) {
        voices.forEach { it.init(ctx) }
        delay.init(ctx)
    }

    private fun lerp11(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * (t * 0.5 + 0.5)
    }
}