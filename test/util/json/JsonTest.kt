package util.json

fun main() {
    val json = """
        {
            "name": "John",
            "age": 30,
            "cars": [
                "Ford",
                25,
                "Fiat"
            ],
            "variantList": [
                {
                    "key": "value"
                },
                {
                    "a": 3
                },
                {
                    "arr": []
                },
                3,
                5,
                {
                    "key": "value2"
                }
            ],
            "enum values": ["value1", "value2", "value1", "value3", {}, [1,2,3]]
        }
    """.trimIndent()

    val schema = ObjectSchema(
        "name" to NullableSchema(StringSchema()) to false, "age" to IntSchema() to false, "cars" to TupleSchema(
            NullableSchema(StringSchema()), NullableSchema(IntSchema()), NullableSchema(StringSchema())
        ) to false, "variantList" to ArraySchema(
            UnionSchema(
                ObjectSchema("key" to StringSchema() to false),
                ObjectSchema("a" to IntSchema() to false),
                ObjectSchema("arr" to ArraySchema(StringSchema()) to false),
                IntSchema()
            )
        ) to false, "enum values" to ArraySchema(
            EnumSchema(
                JsonString("value1"), JsonString("value2"), JsonString("value3"), JsonObject(
                    mutableMapOf()
                ), JsonArray(
                    mutableListOf(JsonNumber(1), JsonNumber(2), JsonNumber(3))
                )
            )
        ) to false
    )

    val parsed = JsonParser.parse(json)
    println(parsed)

    schema.validate(parsed).let { println(it) }
    schema.convert(parsed).let { println(it) }

    val data = schema.safeParse(json).ok()!!
    data["variantList"].let { println(it) }
    data["variantList"]!![1].let { println(it) }
    data["variantList"]!![1]["a"].let { println(it) }
}