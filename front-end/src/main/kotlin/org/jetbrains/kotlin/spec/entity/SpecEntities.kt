package org.jetbrains.kotlin.spec.entity

import org.jetbrains.kotlin.spec.entity.test.Test
import org.jetbrains.kotlin.spec.entity.test.parameters.LinkType
import org.jetbrains.kotlin.spec.entity.test.parameters.TestType
import org.jetbrains.kotlin.spec.entity.test.parameters.testArea.TestArea

private interface SpecEntity {
    val loadedTestAreas: MutableMap<TestArea, List<Test>>
}

class Section(override val loadedTestAreas: MutableMap<TestArea, List<Test>>) : SpecEntity

class Paragraph(section: Section, val paragraph: Int) : SpecEntity {
    override val loadedTestAreas: MutableMap<TestArea, List<Test>> = mutableMapOf()

    init {
        section.loadedTestAreas.forEach { map ->
            loadedTestAreas[map.key] = map.value.filter { test -> test.testPlace.paragraph == paragraph }
        }
    }
}

class Sentence(paragraph: Paragraph, sentence: Int) : SpecEntity {
    override val loadedTestAreas: MutableMap<TestArea, List<Test>> = mutableMapOf()

    init {
        paragraph.loadedTestAreas.forEach { map ->
            loadedTestAreas[map.key] = map.value.filter { test -> test.testPlace.sentence == sentence }
        }
    }

    fun getTestsByTestArea(testArea: TestArea): List<Test>? {
        loadedTestAreas.forEach { map ->
            if (map.key == testArea)
                return if (map.value.isEmpty())
                    null else map.value
        }
        return null
    }

    fun getTestsByTestType(testArea: TestArea, testType: TestType): List<Test>? {
        val result = getTestsByTestArea(testArea)?.filter { test -> test.testPlace.testType == testType }
        return when {
            result == null -> null
            result.isEmpty() -> null
            else -> result
        }
    }

    fun getTestsByTestPriority(testArea: TestArea, testType: TestType, linkType: LinkType): List<Test>? {
        val result = getTestsByTestType(testArea, testType)?.filter { test -> test.testInfo.linkType == linkType }
        return when {
            result == null -> null
            result.isEmpty() -> null
            else -> result
        }
    }

    fun getTestByTestNumber(testArea: TestArea, testType: TestType, linkType: LinkType, testNumber: Int): Test? {
        getTestsByTestPriority(testArea, testType, linkType)?.forEach { test ->
            if (test.testInfo.testNumber == testNumber)
                return test
        }
        return null
    }
}

