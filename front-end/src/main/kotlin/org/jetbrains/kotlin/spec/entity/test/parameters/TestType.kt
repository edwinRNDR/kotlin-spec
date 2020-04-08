package org.jetbrains.kotlin.spec.entity.test.parameters

enum class TestType(val shortName: String) {
    Positive("pos"),
    Negative("neg");

    override fun toString(): String {
        return name.toLowerCase()
    }

    companion object {
        fun getByShortName(shortName: String): TestType {
            values().forEach {
                if (it.shortName == shortName) return it
            }
            throw Exception("TestType $shortName is not found")
        }
    }
}