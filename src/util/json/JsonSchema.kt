package util.json

class ValidationError(private val path: Array<String>, private val message: String) {
    override fun toString(): String {
        return "At path \"${path.joinToString(".")}\": $message"
    }
}

abstract class Result<out TOk, out TErr> {
    class Ok<TOk, TErr>(val value: TOk) : Result<TOk, TErr>() {
        override fun isOk(): Boolean = true
        override fun toString(): String {
            return "Ok($value)"
        }
    }

    class Err<TOk, TErr>(val value: TErr) : Result<TOk, TErr>() {
        override fun isOk(): Boolean = false
        override fun toString(): String {
            return "Err($value)"
        }
    }

    abstract fun isOk(): Boolean
    fun isErr(): Boolean = !isOk()
    fun ok(): TOk? = (this as? Ok)?.value
    fun err(): TErr? = (this as? Err)?.value
}

abstract class JsonSchema {
    internal abstract fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError?

    fun validate(json: JsonElement): ValidationError? {
        return validateInternal(mutableListOf(), json)
    }

    fun isValid(json: JsonElement): Boolean {
        return validate(json) == null
    }

    fun convert(json: JsonElement): SchemaData {
        val validation = validate(json)
        require(validation == null) { "JSON does not match schema: $validation" }
        return convertInternal(json)
    }

    internal abstract fun convertInternal(json: JsonElement): SchemaData

    fun safeParse(json: String): Result<SchemaData, ValidationError> {
        val parsed = JsonParser(json).parse()
        val validation = validate(parsed)
        return if (validation != null) Result.Err(validation) else Result.Ok(convert(parsed))
    }
}

abstract class SchemaData {
    fun obj(): ObjectSchema.ObjectData = this as ObjectSchema.ObjectData
    fun tuple(): TupleSchema.TupleData = this as TupleSchema.TupleData
    fun arr(): ArraySchema.ArrayData = this as ArraySchema.ArrayData
    fun enum(): EnumSchema.EnumData = this as EnumSchema.EnumData
    fun str(): StringSchema.StringData = this as StringSchema.StringData
    fun integer(): IntSchema.IntData = this as IntSchema.IntData
    fun num(): NumberSchema.NumberData = this as NumberSchema.NumberData
    fun bool(): BooleanSchema.BooleanData = this as BooleanSchema.BooleanData
    fun nil(): NullSchema.NullData = this as NullSchema.NullData
    fun union(): UnionSchema.UnionData = this as UnionSchema.UnionData
    fun any(): AnySchema.AnyData = this as AnySchema.AnyData

    open operator fun get(key: String): SchemaData? = null
    open operator fun get(index: Int): SchemaData = throw UnsupportedOperationException()
}

class ObjectSchema(private val properties: Map<String, Pair<JsonSchema, Boolean>>) : JsonSchema() {
    constructor(vararg properties: Pair<Pair<String, JsonSchema>, Boolean>) : this(properties.associate {
        it.first.first to Pair(
            it.first.second, it.second
        )
    })

    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        if (json !is JsonObject) return ValidationError(path.toTypedArray(), "Expected object")
        val unusedKeys = json.keys.toMutableSet()
        for ((key, value) in properties) {
            if (!json.containsKey(key)) {
                if (!value.second) return ValidationError(path.toTypedArray(), "Missing required key")
            } else {
                path.add(key)
                val err = value.first.validateInternal(path, json[key]!!)
                path.removeLast()
                if (err != null) return err
                unusedKeys.remove(key)
            }
        }
        if (unusedKeys.isNotEmpty()) return ValidationError(path.toTypedArray(), "Unexpected keys")
        return null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonObject)
        val properties = mutableMapOf<String, SchemaData?>()
        for ((key, value) in this.properties) {
            properties[key] = json[key]?.let { value.first.convertInternal(it) }
        }
        return ObjectData(properties)
    }

    data class ObjectData(val properties: Map<String, SchemaData?>) : SchemaData() {
        override operator fun get(key: String): SchemaData? = properties[key]
    }
}

class NullableSchema(private val schema: JsonSchema) : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        if (json is JsonNull) return null
        return schema.validateInternal(path, json)
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        return if (json is JsonNull) NullableData(null) else NullableData(schema.convertInternal(json))
    }

    data class NullableData(val data: SchemaData?) : SchemaData() {
        override fun get(index: Int): SchemaData {
            return data?.get(index) ?: throw UnsupportedOperationException()
        }

        override fun get(key: String): SchemaData? {
            return data?.get(key)
        }
    }
}

class TupleSchema(private val schemas: List<JsonSchema>) : JsonSchema() {
    constructor(vararg schemas: JsonSchema) : this(schemas.toList())

    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        if (json !is JsonArray) return ValidationError(path.toTypedArray(), "Expected array")
        if (json.size != schemas.size) return ValidationError(path.toTypedArray(), "Array size does not match schema")
        for (i in schemas.indices) {
            path.add(i.toString())
            val err = schemas[i].validate(json[i])
            path.removeLast()
            if (err != null) return err
        }
        return null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonArray)
        val elements = mutableListOf<SchemaData>()
        for (i in schemas.indices) {
            elements.add(schemas[i].convertInternal(json[i]))
        }
        return TupleData(elements)
    }

    data class TupleData(val elements: List<SchemaData>) : SchemaData() {
        override operator fun get(index: Int): SchemaData = elements[index]
    }
}

class ArraySchema(private val schema: JsonSchema) : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        if (json !is JsonArray) return ValidationError(path.toTypedArray(), "Expected array")
        for (i in 0..<json.size) {
            path.add(i.toString())
            val err = schema.validateInternal(path, json[i])
            path.removeLast()
            if (err != null) return err
        }
        return null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonArray)
        val elements = mutableListOf<SchemaData>()
        for (element in json) {
            elements.add(schema.convertInternal(element))
        }
        return ArrayData(elements)
    }

    data class ArrayData(val elements: List<SchemaData>) : SchemaData() {
        override operator fun get(index: Int): SchemaData = elements[index]
    }
}

class EnumSchema(private val values: List<JsonElement>) : JsonSchema() {
    constructor(vararg values: JsonElement) : this(values.toList())
    constructor(vararg values: String) : this(values.map { JsonString(it) })

    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        val index = values.indexOfFirst { it.contentEquals(json) }
        return if (index == -1) ValidationError(
            path.toTypedArray(), "JSON element $json does not match any enum value $values"
        ) else null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        return EnumData(values.indexOfFirst { it.contentEquals(json) })
    }

    data class EnumData(val id: Int) : SchemaData()
}

class StringSchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        return if (json is JsonString) null else ValidationError(path.toTypedArray(), "Expected string")
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonString)
        return StringData(json.value)
    }

    data class StringData(val value: String) : SchemaData()
}

class IntSchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        if (json !is JsonNumber) return ValidationError(path.toTypedArray(), "Expected number")
        if (json.value !is Int) return ValidationError(path.toTypedArray(), "Expected integer")
        return null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonNumber && json.value is Int)
        return IntData(json.value)
    }

    data class IntData(val value: Int) : SchemaData()
}

class NumberSchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        return if (json is JsonNumber) null else ValidationError(path.toTypedArray(), "Expected number")
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonNumber)
        return NumberData(json.value)
    }

    data class NumberData(val value: Number) : SchemaData()
}

class BooleanSchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        return if (json is JsonBoolean) null else ValidationError(path.toTypedArray(), "Expected boolean")
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        require(json is JsonBoolean)
        return BooleanData(json.value)
    }

    data class BooleanData(val value: Boolean) : SchemaData()
}

class NullSchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        return if (json is JsonNull) null else ValidationError(path.toTypedArray(), "Expected null")
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        return NullData
    }

    data object NullData : SchemaData()
}

class UnionSchema(private val schemas: List<JsonSchema>) : JsonSchema() {
    constructor(vararg schemas: JsonSchema) : this(schemas.toList())

    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        val index = schemas.indexOfFirst { it.isValid(json) }
        return if (index == -1) ValidationError(path.toTypedArray(), "JSON element does not match any schema") else null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        val index = schemas.indexOfFirst { it.isValid(json) }
        return UnionData(index, schemas[index].convertInternal(json))
    }

    data class UnionData(val id: Int, val data: SchemaData) : SchemaData() {
        override operator fun get(index: Int): SchemaData {
            return data[index]
        }

        override fun get(key: String): SchemaData? {
            return data[key]
        }
    }
}

class AnySchema : JsonSchema() {
    override fun validateInternal(path: MutableList<String>, json: JsonElement): ValidationError? {
        return null
    }

    override fun convertInternal(json: JsonElement): SchemaData {
        return AnyData(json)
    }

    data class AnyData(val json: JsonElement) : SchemaData()
}