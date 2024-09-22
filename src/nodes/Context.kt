package nodes

import kotlin.random.Random

class Context(val seed: Int, val sampleRate: Int) {
    var time = 0.0
        private set
    var sampleCount = 0
        private set
    val timeStep = 1.0 / sampleRate
    val random = Random(seed)

    fun tick() {
        time += timeStep
        sampleCount += sampleRate
    }

    fun clone(): Context {
        return Context(seed, sampleRate).also {
            it.time = time
            it.sampleCount = sampleCount
        }
    }
}