package node

class SettingsNode(private var settings: DoubleArray) : AudioNode(0, settings.size) {
    override fun process(ctx: Context, inputs: DoubleArray): DoubleArray {
        return settings
    }

    override fun cloneSettings(): AudioNode {
        return SettingsNode(settings)
    }

    fun setSettings(settings: DoubleArray) {
        require(settings.size == this.settings.size)
        this.settings = settings
    }

    operator fun set(index: Int, value: Double) {
        settings[index] = value
    }
}