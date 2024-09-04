package nodes


infix fun AudioNode.repeat(other: Int): AudioNode {
    if (other == 1) return this
    return this stack (this repeat (other - 1))
}

infix fun AudioNode.pipe(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.PIPE, this, other)
}

infix fun AudioNode.pipe(other: Double): AudioNode {
    return this pipe ConstantNode(other)
}

infix fun Double.pipe(other: AudioNode): AudioNode {
    return ConstantNode(this) pipe other
}


infix fun AudioNode.stack(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.STACK, this, other)
}

infix fun AudioNode.stack(other: Double): AudioNode {
    return this stack ConstantNode(other)
}

infix fun Double.stack(other: AudioNode): AudioNode {
    return ConstantNode(this) stack other
}


operator fun AudioNode.plus(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.STACK_ADD, this, other)
}

operator fun AudioNode.plus(other: Double): AudioNode {
    return this + ConstantNode(other)
}

operator fun Double.plus(other: AudioNode): AudioNode {
    return ConstantNode(this) + other
}


infix fun AudioNode.branch(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.BUS_SPLIT, this, other)
}

infix fun AudioNode.branch(other: Double): AudioNode {
    return this branch ConstantNode(other)
}

infix fun Double.branch(other: AudioNode): AudioNode {
    return ConstantNode(this) branch other
}


infix fun AudioNode.bus(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.BUS_ADD, this, other)
}

infix fun AudioNode.bus(other: Double): AudioNode {
    return this bus ConstantNode(other)
}

infix fun Double.bus(other: AudioNode): AudioNode {
    return ConstantNode(this) bus other
}


operator fun AudioNode.times(other: AudioNode): AudioNode {
    return ConnectorNode(ConnectorNode.Mode.STACK_MULTIPLY, this, other)
}

operator fun AudioNode.times(factor: Double): AudioNode {
    return this * ConstantNode(factor)
}

operator fun Double.times(other: AudioNode): AudioNode {
    return ConstantNode(this) * other
}