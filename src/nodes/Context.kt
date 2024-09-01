package nodes

class Context(val sampleRate: Int) {
    var time = 0.0
        private set
    var sampleCount = 0
        private set
    val timeStep = 1.0 / sampleRate

    fun tick() {
        time += timeStep
        sampleCount += sampleRate
    }
}