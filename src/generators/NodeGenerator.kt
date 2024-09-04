package generators

import nodes.AudioNode

abstract class NodeGenerator {
    abstract fun generateNode(): AudioNode
}