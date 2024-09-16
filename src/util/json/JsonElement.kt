package util.json

abstract class JsonElement {
    abstract fun contentEquals(other: JsonElement): Boolean
    open operator fun get(key: String): JsonElement? = null
    open operator fun get(index: Int): JsonElement = throw UnsupportedOperationException()
}

data class JsonObject(val map: MutableMap<String, JsonElement>) : JsonElement() {
    val keys get() = map.keys
    val size get() = map.size

    operator fun set(key: String, value: JsonElement) {
        map[key] = value
    }

    override operator fun get(key: String): JsonElement? {
        return map[key]
    }

    fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    operator fun iterator(): Iterator<Map.Entry<String, JsonElement>> {
        return map.iterator()
    }

    override fun contentEquals(other: JsonElement): Boolean {
        if (other !is JsonObject) return false
        if (size != other.size) return false
        for ((key, value) in map) {
            if (!other.containsKey(key)) return false
            if (!value.contentEquals(other[key]!!)) return false
        }
        return true
    }

    fun getFromPath(path: List<String>, index: Int): JsonElement? {
        var current: JsonElement = this
        for (key in path) {
            if (current !is JsonObject) return null
            current = current[key] ?: return null
        }
        return current
    }
}

data class JsonArray(val list: MutableList<JsonElement>) : JsonElement() {
    val size get() = list.size

    fun add(element: JsonElement) {
        list.add(element)
    }

    override operator fun get(index: Int): JsonElement {
        return list[index]
    }

    operator fun iterator(): Iterator<JsonElement> {
        return list.iterator()
    }

    override fun contentEquals(other: JsonElement): Boolean {
        if (other !is JsonArray) return false
        if (size != other.size) return false
        for (i in list.indices) {
            if (!list[i].contentEquals(other[i])) return false
        }
        return true
    }
}

data class JsonString(val value: String) : JsonElement() {
    override fun contentEquals(other: JsonElement): Boolean {
        return other is JsonString && value == other.value
    }
}

data class JsonNumber(val value: Number) : JsonElement() {
    override fun contentEquals(other: JsonElement): Boolean {
        return other is JsonNumber && value == other.value
    }
}

data class JsonBoolean(val value: Boolean) : JsonElement() {
    override fun contentEquals(other: JsonElement): Boolean {
        return other is JsonBoolean && value == other.value
    }
}

object JsonNull : JsonElement() {
    override fun contentEquals(other: JsonElement): Boolean {
        return other is JsonNull
    }
}