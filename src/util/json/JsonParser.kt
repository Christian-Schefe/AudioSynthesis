package util.json

class JsonParser(private val json: String) {
    private var index = 0

    fun parse(): JsonElement {
        return parseElement()
    }

    private fun parseElement(): JsonElement {
        skipWhitespace()
        return when (json[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonObject {
        val obj = JsonObject(mutableMapOf())
        index++
        skipWhitespace()
        while (json[index] != '}') {
            val key = parseString().value
            skipWhitespace()
            require(json[index] == ':') { "Expected ':'" }
            index++
            skipWhitespace()
            val value = parseElement()
            obj[key] = value
            skipWhitespace()
            if (json[index] == ',') {
                index++
                skipWhitespace()
            }
        }
        index++
        return obj
    }

    private fun parseArray(): JsonArray {
        val arr = JsonArray(mutableListOf())
        index++
        skipWhitespace()
        while (json[index] != ']') {
            arr.add(parseElement())
            skipWhitespace()
            if (json[index] == ',') {
                index++
                skipWhitespace()
            }
        }
        index++
        return arr
    }

    private fun parseString(): JsonString {
        require(json[index] == '"') { "Expected '\"'" }
        index++
        val start = index
        while (json[index] != '"') {
            index++
        }
        val value = json.substring(start, index)
        index++
        return JsonString(value)
    }

    private fun parseBoolean(): JsonBoolean {
        val value = when (json[index]) {
            't' -> true
            'f' -> false
            else -> throw IllegalArgumentException("Invalid boolean")
        }
        val expected = if (value) "true" else "false"
        require(json.substring(index, index + expected.length) == expected) { "Invalid boolean" }
        index += expected.length
        return JsonBoolean(value)
    }

    private fun parseNull(): JsonNull {
        require(json.substring(index, index + 4) == "null") { "Invalid null" }
        index += 4
        return JsonNull
    }

    private fun parseNumber(): JsonNumber {
        val start = index
        while (json[index].isDigit() || json[index] == '.' || json[index] == '-' || json[index] == 'e' || json[index] == 'E') {
            index++
        }
        val value = json.substring(start, index)
        return JsonNumber(value.toIntOrNull() ?: value.toDouble())
    }

    private fun skipWhitespace() {
        while (json[index].isWhitespace()) {
            index++
        }
    }

    companion object {
        fun parse(json: String): JsonElement {
            val parser = JsonParser(json)
            return parser.parse()
        }
    }
}
