package org.jetbrains.kotlin.spec.entity.test.parameters.testArea

import org.jetbrains.kotlin.spec.entity.test.TestCase
import org.jetbrains.kotlin.spec.entity.test.parameters.testArea.SpecTestsParser.implementationTestInfoPattern
import org.jetbrains.kotlin.spec.entity.test.parameters.testArea.SpecTestsParser.topLevelDirectivePattern

enum class TestArea(val path: String,
                    val description: String,
                    val attributeValue: String,
                    val testCasesParser: (String) -> List<TestCase>
) {
    Diagnostics("diagnostics", "Front-end diagnostics tests", "diagnostics",
            { data -> parseDiagnosticsTest(data) }),
    CodegenBox("codegen/box", "Codegen box tests", "box",
            { data -> parseCodegenBoxTest(data) }
    );

    companion object {
        fun parseCodegenBoxTest(testFileCode: String): List<TestCase> {
            val data = removeMetadataComments(testFileCode)

            val testCase = TestCase(data.replace(SpecTestsParser.testInfoPattern, ""))
            return listOf(testCase)
        }

        fun parseDiagnosticsTest(testFileCode: String): List<TestCase> {
            val data = removeMetadataComments(testFileCode)

            val list = mutableListOf<TestCase>()
            var testCaseMatches = SpecTestsParser.testCaseInfoPattern.find(data)
            var startPosition = 0
            while (testCaseMatches != null) {
                val infoElementGroup = testCaseMatches.groups[1] ?: testCaseMatches.groups[4]
                val infoElements = (infoElementGroup)?.value ?: continue
                val codeGroup = testCaseMatches.groups[2] ?: testCaseMatches.groups[5]
                val code = (codeGroup)?.value?.replace(SpecTestsParser.diagnosticTagRegexp, "") ?: continue
                val testCaseMatchesGroup = (testCaseMatches.groups[3] ?: testCaseMatches.groups[6]) ?: continue

                startPosition += testCaseMatches.range.last - (testCaseMatchesGroup).value.length

                list.add(TestCase(code = code, infoElements = SpecTestsParser.parseTestInfoElements(infoElements)))

                testCaseMatches = SpecTestsParser.testCaseInfoPattern.find(data.substring(startPosition))
            }
            return list
        }

        private fun removeMetadataComments(testFileCode: String) = testFileCode
                .replace(SpecTestsParser.diagnosticTagRegexp, "")
                .replace(implementationTestInfoPattern, "")
                .replace(topLevelDirectivePattern, "")

        fun getByAttribute(attributeValue: String): TestArea {
            values().forEach { testArea ->
                if (testArea.attributeValue == attributeValue)
                    return testArea
            }
            throw Exception("TestArea by attributeValue $attributeValue is not found")
        }
    }
}
