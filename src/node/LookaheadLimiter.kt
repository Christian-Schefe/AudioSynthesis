package node

import kotlin.math.abs
import kotlin.math.min

class LookaheadLimiter(
    channels: Int, private val threshold: Double, private val lookaheadTime: Double, private val releaseTime: Double
) : AudioNode(channels, channels) { // 1 input, 1 output node
    private var lookaheadSamples: Int = (lookaheadTime * 44100.0).toInt()
    private var releaseSamples: Int = (releaseTime * 44100.0).toInt()
    private var lookaheadBuffer = DoubleArray(lookaheadSamples)
    private var writeIndex = 0
    private var gain: Double = 1.0
    private var releaseCounter = 0

    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        val output = DoubleArray(outputCount)

        for (i in 0..<inputCount) {
            // Lookahead sample is from the future (circular buffer)
            val lookaheadSample = lookaheadBuffer[writeIndex]

            // Check if the sample exceeds the threshold
            if (abs(lookaheadSample) > threshold) {
                // Calculate new gain reduction
                gain = min(gain, threshold / abs(lookaheadSample))
                releaseCounter = releaseSamples // Reset release counter
            }

            // Apply the current gain
            output[i] = inputs[i] * gain

            // Handle release (smooth recovery to normal gain)
            if (releaseCounter > 0) {
                releaseCounter--
            } else if (gain < 1.0) {
                gain += (1.0 - gain) / releaseSamples
                if (gain > 1.0) gain = 1.0 // Ensure gain doesn't exceed 1.0
            }

            // Store the current input in the lookahead buffer
            lookaheadBuffer[writeIndex] = inputs[i]

            // Increment and wrap the write index for the circular buffer
            writeIndex = (writeIndex + 1) % lookaheadSamples
        }

        return output
    }

    override fun init(ctx: Context) {
        lookaheadSamples = (lookaheadTime * ctx.sampleRate).toInt()
        releaseSamples = (releaseTime * ctx.sampleRate).toInt()
        reset()
    }

    override fun cloneSettings(): AudioNode {
        return LookaheadLimiter(inputCount, threshold, lookaheadTime, releaseTime)
    }

    override fun reset() {
        lookaheadBuffer = DoubleArray(lookaheadSamples)
        writeIndex = 0
        gain = 1.0
        releaseCounter = 0
    }
}