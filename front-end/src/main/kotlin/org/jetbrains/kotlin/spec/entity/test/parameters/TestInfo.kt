package org.jetbrains.kotlin.spec.entity.test.parameters

import kotlinx.serialization.json.JsonObject

/** contains all options which could be defined in testMap.json's tests */
private enum class TestElementKey {
    specVersion,
    casesNumber,
    description,
    path,
    unexpectedBehaviour,
    linkType
    ;

    override fun toString(): String {
        return name
    }
}

/** contains parsed info of test values declared in json test element */
class TestInfo(jsonObject: JsonObject, val testNumber: Int) {
    val specVersion: String
    val casesNumber: Int
    val description: String
    var path: String
    val unexpectedBehaviour: Boolean
    val linkType: LinkType

    private val valuesMap: MutableMap<TestElementKey, String>

    init {
        valuesMap = parseTestMap(jsonObject)

        specVersion = valuesMap[TestElementKey.specVersion]!!
        casesNumber = (valuesMap[TestElementKey.casesNumber]!!).toInt()
        description = valuesMap[TestElementKey.description]!!
        path = valuesMap[TestElementKey.path]!!
        unexpectedBehaviour = (valuesMap[TestElementKey.unexpectedBehaviour]!!).toBoolean()
        linkType = LinkType.valueOf(valuesMap[TestElementKey.linkType]!!)
    }

    companion object {
        private fun parseTestMap(jsonObject: JsonObject): MutableMap<TestElementKey, String> {
            val map = mutableMapOf<TestElementKey, String>()
            TestElementKey.values().forEach {
                map[it] = jsonObject[it.name]?.primitive?.content ?: ""
            }
            return map
        }
    }
}