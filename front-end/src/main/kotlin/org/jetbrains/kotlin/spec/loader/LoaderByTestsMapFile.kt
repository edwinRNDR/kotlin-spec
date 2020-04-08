package org.jetbrains.kotlin.spec.loader

import org.jetbrains.kotlin.spec.entity.Section
import org.jetbrains.kotlin.spec.entity.SectionTestMap
import org.jetbrains.kotlin.spec.entity.test.Test
import org.jetbrains.kotlin.spec.entity.test.TestPlace
import org.jetbrains.kotlin.spec.entity.test.parameters.TestInfo
import org.jetbrains.kotlin.spec.entity.test.parameters.TestType
import org.jetbrains.kotlin.spec.entity.test.parameters.testArea.TestArea
import org.jetbrains.kotlin.spec.loader.GithubTestsLoader.Companion.loadTestFileFromRawGithub
import org.jetbrains.kotlin.spec.loader.GithubTestsLoader.Companion.loadTestMapFileFromRawGithub
import org.jetbrains.kotlin.spec.utils.format
import kotlin.js.Promise


class LoaderByTestsMapFile : GithubTestsLoader {
    private val testsMapFilename = "testsMap.json"

    private fun loadTestsMapFile(sectionsPath: String, testAreasToLoad: Array<TestArea>) =
            loadTestMapFileFromRawGithub("$sectionsPath/$testsMapFilename",
                    null,
                    GithubTestsLoader.TestFileType.SPEC_TEST,
                    testAreasToLoad = testAreasToLoad)


    private fun getPromisesForTestFilesFromTestMap(
            testsMapSection: SectionTestMap?,
            sectionsPath: List<String>,
            sectionName: String
    ): Array<Promise<Test>> {
        val promises = mutableListOf<Promise<Test>>()
        val testsMap = testsMapSection?.sectionTestMap ?: return promises.toTypedArray()

        for ((paragraph, testsByParagraphs) in testsMap.jsonObject) {
            for ((testType, testsByTypes) in testsByParagraphs.jsonObject) {
                for ((testSentence, testsBySentences) in testsByTypes.jsonObject) {

                    testsBySentences.jsonArray.forEachIndexed { i, testInfo ->
                        val testPathInSpec = "{1}.{2}.p-{3}.{4}.{5}.{6}"
                                .format(sectionsPath.joinToString("."), sectionName, paragraph, testType, testSentence, i + 1)
                        val testFilePath = testInfo.jsonObject["path"]?.primitive?.content!!

                        val testElementInfo = TestInfo(testInfo.jsonObject, i + 1)
                        if (testElementInfo.path.isEmpty())
                            testElementInfo.path = "{1}/{2}/p-{3}/{4}/{5}.{6}.kt"
                                    .format(sectionsPath.joinToString("/"), sectionName, paragraph, testType, testSentence, i + 1)

                        val testPath = TestPlace(paragraph.toInt(), TestType.getByShortName(testType), testSentence.toInt())
                        promises.add(loadTestFileFromRawGithub(testFilePath, testPathInSpec, testInfo = testElementInfo, testPlace = testPath, testArea = testsMapSection.testArea))
                    }
                }
            }
        }
        return promises.toTypedArray()
    }


    override fun loadTestFiles(sectionName: String, sectionsPath: List<String>, testAreasToLoad: Array<TestArea>)
            : Promise<Promise<Section>> {
        return loadTestsMapFile(sectionsPath.joinToString("/") + "/" + sectionName, testAreasToLoad)
                .then { testsMapDataForSectionByTestAreaMap ->

                    val querySet = mutableListOf<Promise<Any>>()
                    val resultMap = mutableMapOf<TestArea, List<Test>>()
                    testAreasToLoad.forEach {
                        querySet.add(getPromiseForTests(it, testsMapDataForSectionByTestAreaMap, sectionsPath, sectionName, resultMap))
                    }
                    Promise.all(querySet.toTypedArray()
                    ).then {
                        Section(resultMap)
                    }
                }
    }

    private fun getPromiseForTests(
            testArea: TestArea,
            testsMapDataForSectionBySectionTestAreaEnumMap: MutableMap<TestArea, SectionTestMap>,
            sectionsPath: List<String>,
            sectionName: String,
            mapOfTests: MutableMap<TestArea, List<Test>>
    ): Promise<Any> {
        return Promise.all(
                getPromisesForTestFilesFromTestMap(
                        testsMapDataForSectionBySectionTestAreaEnumMap[testArea],
                        sectionsPath, sectionName)
        ).then { tests ->
            mapOfTests[testArea] = tests.toList()
        }
    }

}