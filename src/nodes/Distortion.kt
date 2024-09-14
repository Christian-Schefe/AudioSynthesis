package nodes

import kotlin.math.abs

class Distortion(val shape: (Double) -> Double) : AudioNode(1, 1) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return doubleArrayOf(shape(inputs[0]))
    }

    override fun clone(): AudioNode {
        return Distortion(shape)
    }

    companion object {
        fun hardClip(hardness: Double): Distortion {
            val shape = { x: Double -> (x * hardness).coerceIn(-1.0, 1.0) }
            return Distortion(shape)
        }

        fun softClip(): Distortion {
            val shape = { x: Double -> x / (1 + abs(x)) }
            return Distortion(shape)
        }

        fun tanh(hardness: Double): Distortion {
            val shape = { x: Double -> kotlin.math.tanh(x * hardness) }
            return Distortion(shape)
        }
    }
}